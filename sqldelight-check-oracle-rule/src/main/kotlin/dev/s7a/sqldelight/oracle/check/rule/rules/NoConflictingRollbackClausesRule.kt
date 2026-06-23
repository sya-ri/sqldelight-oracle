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
 * Reports conflicting Oracle ROLLBACK clauses.
 */
public class NoConflictingRollbackClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-rollback-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .rollbackStatements()
            .flatMap { statement -> statement.conflictingRollbackClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle ROLLBACK clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class RollbackToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class RollbackOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class RollbackConflict(
    val group: String,
    val first: RollbackOccurrence,
    val second: RollbackOccurrence,
)

private fun String.rollbackStatements(): List<List<RollbackToken>> {
    val statements = mutableListOf<List<RollbackToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).rollbackTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<RollbackToken>.conflictingRollbackClauses(): List<RollbackConflict> {
    if (!getOrNull(0).rollbackHasText("ROLLBACK")) return emptyList()
    val firstByGroup = linkedMapOf<String, RollbackOccurrence>()
    return rollbackOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            RollbackConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<RollbackToken>.rollbackOccurrences(): List<RollbackOccurrence> =
    buildList {
        this@rollbackOccurrences.forEachIndexed { index, token ->
            when {
                token.rollbackHasText("TO") -> {
                    add(
                        RollbackOccurrence(
                            group = "TARGET",
                            startOffset = token.startOffset,
                            endOffset =
                                this@rollbackOccurrences.getOrNull(index + 2)?.endOffset
                                    ?: this@rollbackOccurrences.getOrNull(index + 1)?.endOffset
                                    ?: token.endOffset,
                        ),
                    )
                }

                token.rollbackHasText("FORCE") -> {
                    add(
                        RollbackOccurrence(
                            group = "TARGET",
                            startOffset = token.startOffset,
                            endOffset = this@rollbackOccurrences.getOrNull(index + 1)?.endOffset ?: token.endOffset,
                        ),
                    )
                }
            }
        }
    }

private val rollbackTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.rollbackTokens(offset: Int): List<RollbackToken> =
    rollbackTokenPattern
        .findAll(this)
        .map { match ->
            RollbackToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun RollbackToken?.rollbackHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
