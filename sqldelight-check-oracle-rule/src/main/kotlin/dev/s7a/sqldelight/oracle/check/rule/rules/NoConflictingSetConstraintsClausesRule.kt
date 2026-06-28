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
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
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
    if (!getOrNull(0).setConstraintsHasText("SET") ||
        (
            !getOrNull(1).setConstraintsHasText("CONSTRAINT") &&
                !getOrNull(1).setConstraintsHasText("CONSTRAINTS")
        )
    ) {
        return null
    }
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

private fun SetConstraintsToken?.setConstraintsHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
