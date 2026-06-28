package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports static row-value comparisons whose right-hand row arity differs from the left-hand row.
 */
public class ValidRowValueArityRule : Rule {
    override val id: RuleId = RuleId("valid-row-value-arity")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.maskSqlCommentsAndQuotedTextPreservingOffsets().rowValueArityMismatches().forEach { mismatch ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message =
                        "Oracle row value has ${mismatch.rightArity} column(s), " +
                            "but the left row has ${mismatch.leftArity}.",
                    file = context.file,
                    range = content.rangeAtOffsets(mismatch.range.first, mismatch.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}

private data class RowValueArityMismatch(
    val leftArity: Int,
    val rightArity: Int,
    val range: IntRange,
)

private fun String.rowValueArityMismatches(): List<RowValueArityMismatch> {
    val mismatches = mutableListOf<RowValueArityMismatch>()
    var index = 0
    while (index < length) {
        if (this[index] != '(') {
            index++
            continue
        }

        val leftEnd =
            matchingSqlParenthesis(index) ?: run {
                index++
                continue
            }
        val leftArity = countTopLevelSqlItems(index + 1, leftEnd)
        if (leftArity < 2) {
            index = leftEnd + 1
            continue
        }

        val afterLeft = skipRowValueWhitespace(leftEnd + 1)
        when {
            hasRowValueOperator(afterLeft) -> {
                val rightStart = skipRowValueWhitespace(afterLeft + rowValueOperatorLength(afterLeft))
                val quantifiedListStart = rowValueQuantifiedListStart(rightStart)
                if (quantifiedListStart != null) {
                    mismatches += rowValueInListMismatches(leftArity, quantifiedListStart)
                } else {
                    if (rightStart < length && this[rightStart] == '(') {
                        val rightEnd = matchingSqlParenthesis(rightStart) ?: return mismatches
                        val rightArity = countTopLevelSqlItems(rightStart + 1, rightEnd)
                        if (rightArity != leftArity) {
                            mismatches += RowValueArityMismatch(leftArity, rightArity, rightStart..rightEnd)
                        }
                    }
                }
            }

            startsWithKeyword(afterLeft, "IN") || startsWithKeyword(afterLeft, "NOT") -> {
                val inOffset =
                    if (startsWithKeyword(afterLeft, "NOT")) {
                        skipRowValueWhitespace(afterLeft + "NOT".length).takeIf { startsWithKeyword(it, "IN") }
                    } else {
                        afterLeft
                    }
                val listStart = inOffset?.let { skipRowValueWhitespace(it + "IN".length) }
                if (listStart != null && listStart < length && this[listStart] == '(') {
                    mismatches += rowValueInListMismatches(leftArity, listStart)
                }
            }
        }

        index = leftEnd + 1
    }
    return mismatches
}

private fun String.rowValueQuantifiedListStart(offset: Int): Int? {
    val quantifier =
        rowValueQuantifiers.firstOrNull { keyword -> startsWithKeyword(offset, keyword) }
            ?: return null
    val listStart = skipRowValueWhitespace(offset + quantifier.length)
    return listStart.takeIf { index -> index < length && this[index] == '(' }
}

private fun String.rowValueInListMismatches(
    leftArity: Int,
    listStart: Int,
): List<RowValueArityMismatch> {
    val listEnd = matchingSqlParenthesis(listStart) ?: return emptyList()
    val mismatches = mutableListOf<RowValueArityMismatch>()
    var index = skipRowValueWhitespace(listStart + 1)
    while (index < listEnd) {
        if (this[index] == '(') {
            val rowEnd = matchingSqlParenthesis(index) ?: return mismatches
            val rightArity = countTopLevelSqlItems(index + 1, rowEnd)
            if (rightArity != leftArity) {
                mismatches += RowValueArityMismatch(leftArity, rightArity, index..rowEnd)
            }
            index = skipRowValueWhitespace(rowEnd + 1)
            if (index < listEnd && this[index] == ',') index = skipRowValueWhitespace(index + 1)
        } else {
            index++
        }
    }
    return mismatches
}

private fun String.hasRowValueOperator(index: Int): Boolean =
    listOf("!=", "<>", ">=", "<=", "=", ">", "<").any { operator -> startsWith(operator, index) }

private fun String.rowValueOperatorLength(index: Int): Int =
    listOf("!=", "<>", ">=", "<=", "=", ">", "<").first { operator -> startsWith(operator, index) }.length

private val rowValueQuantifiers = listOf("ANY", "SOME", "ALL")

private fun String.startsWithKeyword(
    index: Int,
    keyword: String,
): Boolean =
    regionMatches(index, keyword, 0, keyword.length, ignoreCase = true) &&
        isRowValueBoundary(index - 1) &&
        isRowValueBoundary(index + keyword.length)

private fun String.isRowValueBoundary(index: Int): Boolean = index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_')

private fun String.skipRowValueWhitespace(index: Int): Int {
    var current = index
    while (current < length && this[current].isWhitespace()) current++
    return current
}
