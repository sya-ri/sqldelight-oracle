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
 * Reports conflicting Oracle subquery restriction clauses.
 */
public class ValidSubqueryRestrictionClauseRule : Rule {
    override val id: RuleId = RuleId("valid-subquery-restriction-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSubqueryRestrictionCommentsAndQuotedTextPreservingOffsets()
            .subqueryRestrictionTokens()
            .splitSubqueryRestrictionStatements()
            .flatMap { statement -> statement.conflictingSubqueryRestrictionClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use one Oracle subquery restriction clause: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SubqueryRestrictionToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SubqueryRestrictionOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SubqueryRestrictionConflict(
    val group: String,
    val first: SubqueryRestrictionOccurrence,
    val second: SubqueryRestrictionOccurrence,
)

private fun List<SubqueryRestrictionToken>.splitSubqueryRestrictionStatements(): List<List<SubqueryRestrictionToken>> {
    val statements = mutableListOf<List<SubqueryRestrictionToken>>()
    var startIndex = 0
    forEachIndexed { index, token ->
        if (token.subqueryRestrictionHasText(";")) {
            if (startIndex < index) statements += subList(startIndex, index)
            startIndex = index + 1
        }
    }
    if (startIndex < size) statements += subList(startIndex, size)
    return statements
}

private fun List<SubqueryRestrictionToken>.conflictingSubqueryRestrictionClauses(): List<SubqueryRestrictionConflict> {
    val firstByGroup = linkedMapOf<String, SubqueryRestrictionOccurrence>()
    return subqueryRestrictionOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent("subquery restriction", occurrence)
        first?.let {
            SubqueryRestrictionConflict(
                group = "${it.group}/${occurrence.group}",
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<SubqueryRestrictionToken>.subqueryRestrictionOccurrences(): List<SubqueryRestrictionOccurrence> =
    mapIndexedNotNull { index, token ->
        when {
            token.subqueryRestrictionHasText("WITH") && getOrNull(index + 1).subqueryRestrictionHasText("READ") &&
                getOrNull(index + 2).subqueryRestrictionHasText("ONLY") -> {
                SubqueryRestrictionOccurrence(
                    group = "WITH READ ONLY",
                    startOffset = token.startOffset,
                    endOffset = get(index + 2).endOffset,
                )
            }

            token.subqueryRestrictionHasText("WITH") && getOrNull(index + 1).subqueryRestrictionHasText("CHECK") &&
                getOrNull(index + 2).subqueryRestrictionHasText("OPTION") -> {
                SubqueryRestrictionOccurrence(
                    group = "WITH CHECK OPTION",
                    startOffset = token.startOffset,
                    endOffset = get(index + 2).endOffset,
                )
            }

            else -> {
                null
            }
        }
    }

private val subqueryRestrictionTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.subqueryRestrictionTokens(): List<SubqueryRestrictionToken> =
    subqueryRestrictionTokenPattern
        .findAll(this)
        .map { match ->
            SubqueryRestrictionToken(
                text = match.value,
                startOffset = match.range.first,
                endOffset = match.range.last + 1,
            )
        }.toList()

private fun String.maskSubqueryRestrictionCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> subqueryRestrictionMaskRange(chars, index, skipSubqueryRestrictionLineComment(index))
                startsWith("/*", index) -> subqueryRestrictionMaskRange(chars, index, skipSubqueryRestrictionBlockComment(index))
                chars[index] == '\'' -> subqueryRestrictionMaskRange(chars, index, skipSubqueryRestrictionQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipSubqueryRestrictionLineComment(start: Int): Int =
    indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipSubqueryRestrictionBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipSubqueryRestrictionQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'') {
            if (index + 1 < length && this[index + 1] == '\'') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun subqueryRestrictionMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun SubqueryRestrictionToken?.subqueryRestrictionHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
