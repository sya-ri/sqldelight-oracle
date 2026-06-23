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
 * Reports conflicting Oracle SET CONSTRAINTS timing clauses.
 */
public class NoConflictingSetConstraintsClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-set-constraints-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSetConstraintsCommentsAndQuotedTextPreservingOffsets()
            .setConstraintsStatements()
            .mapNotNull { statement -> statement.conflictingSetConstraintsClause() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle SET CONSTRAINTS clauses: TIMING.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SetConstraintsToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetConstraintsOccurrence(
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetConstraintsConflict(
    val first: SetConstraintsOccurrence,
    val second: SetConstraintsOccurrence,
)

private fun String.setConstraintsStatements(): List<List<SetConstraintsToken>> {
    val statements = mutableListOf<List<SetConstraintsToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).setConstraintsTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<SetConstraintsToken>.conflictingSetConstraintsClause(): SetConstraintsConflict? {
    if (!getOrNull(0).setConstraintsHasText("SET") || !getOrNull(1).setConstraintsHasText("CONSTRAINTS")) return null
    val timings =
        filter { token -> token.setConstraintsHasText("IMMEDIATE") || token.setConstraintsHasText("DEFERRED") }
            .map { token ->
                SetConstraintsOccurrence(
                    startOffset = token.startOffset,
                    endOffset = token.endOffset,
                )
            }
    val first = timings.firstOrNull() ?: return null
    val second = timings.drop(1).firstOrNull() ?: return null
    return SetConstraintsConflict(first, second)
}

private val setConstraintsTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.setConstraintsTokens(offset: Int): List<SetConstraintsToken> =
    setConstraintsTokenPattern
        .findAll(this)
        .map { match ->
            SetConstraintsToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskSetConstraintsCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> setConstraintsMaskRange(chars, index, skipSetConstraintsLineComment(index))
                startsWith("/*", index) -> setConstraintsMaskRange(chars, index, skipSetConstraintsBlockComment(index))
                chars[index] == '\'' -> setConstraintsMaskRange(chars, index, skipSetConstraintsQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipSetConstraintsLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipSetConstraintsBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipSetConstraintsQuotedString(start: Int): Int {
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

private fun setConstraintsMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun SetConstraintsToken?.setConstraintsHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
