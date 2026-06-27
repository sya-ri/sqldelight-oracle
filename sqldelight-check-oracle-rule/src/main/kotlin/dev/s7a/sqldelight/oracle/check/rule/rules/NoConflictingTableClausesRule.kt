package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports mutually exclusive Oracle table clauses in a single table statement.
 */
public class NoConflictingTableClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-table-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val maskedContent = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .filter { statement -> statement.isTableClauseRuleStatement() }
            .flatMap { statement -> statement.conflictingTableClauses(maskedContent) }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle table clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class TableClauseConflict(
    val group: String,
    val first: TableClauseOccurrence,
    val second: TableClauseOccurrence,
)

private data class TableClauseOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<SqlToken>.isTableClauseRuleStatement(): Boolean =
    (getOrNull(0).tableClauseRuleHasText("ALTER") && getOrNull(1).tableClauseRuleHasText("TABLE")) ||
        (getOrNull(0).tableClauseRuleHasText("CREATE") && take(5).any { token -> token.tableClauseRuleHasText("TABLE") })

private fun List<SqlToken>.conflictingTableClauses(maskedContent: String): List<TableClauseConflict> {
    val firstByGroup = linkedMapOf<String, TableClauseOccurrence>()
    return tableClauseOccurrences(maskedContent)
        .mapNotNull { occurrence ->
            val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
            first?.let {
                TableClauseConflict(
                    group = occurrence.group,
                    first = it,
                    second = occurrence,
                )
            }
        }
}

private fun List<SqlToken>.tableClauseOccurrences(maskedContent: String): List<TableClauseOccurrence> =
    mapIndexedNotNull { index, token ->
        if (maskedContent.tableClauseParenthesisDepthBetween(first().startOffset, token.startOffset) != 0) {
            return@mapIndexedNotNull null
        }
        when {
            token.tableClauseRuleHasText("LOGGING") || token.tableClauseRuleHasText("NOLOGGING") -> {
                token.tableClauseOccurrence("LOGGING/NOLOGGING")
            }

            token.tableClauseRuleHasText("CACHE") || token.tableClauseRuleHasText("NOCACHE") -> {
                token.tableClauseOccurrence("CACHE/NOCACHE")
            }

            token.tableClauseRuleHasText("COMPRESS") || token.tableClauseRuleHasText("NOCOMPRESS") -> {
                token.tableClauseOccurrence("COMPRESS/NOCOMPRESS")
            }

            token.tableClauseRuleHasText("READ") && getOrNull(index + 1).tableClauseRuleHasText("ONLY") -> {
                tableClauseOccurrence(
                    group = "READ ONLY/READ WRITE",
                    start = token,
                    end = get(index + 1),
                )
            }

            token.tableClauseRuleHasText("READ") && getOrNull(index + 1).tableClauseRuleHasText("WRITE") -> {
                tableClauseOccurrence(
                    group = "READ ONLY/READ WRITE",
                    start = token,
                    end = get(index + 1),
                )
            }

            else -> {
                null
            }
        }
    }

private fun SqlToken.tableClauseOccurrence(group: String): TableClauseOccurrence =
    TableClauseOccurrence(
        group = group,
        startOffset = startOffset,
        endOffset = endOffset,
    )

private fun tableClauseOccurrence(
    group: String,
    start: SqlToken,
    end: SqlToken,
): TableClauseOccurrence =
    TableClauseOccurrence(
        group = group,
        startOffset = start.startOffset,
        endOffset = end.endOffset,
    )

private fun String.tableClauseParenthesisDepthBetween(
    startOffset: Int,
    endOffset: Int,
): Int {
    var depth = 0
    for (index in startOffset until endOffset) {
        when (this[index]) {
            '(' -> depth++
            ')' -> if (depth > 0) depth--
        }
    }
    return depth
}

private fun SqlToken?.tableClauseRuleHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
