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
 * Reports mutually exclusive Oracle index clauses in a single index statement.
 */
public class NoConflictingIndexClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-index-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .filter { statement -> statement.isIndexClauseRuleStatement() }
            .flatMap { statement -> statement.conflictingIndexClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle index clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class IndexClauseConflict(
    val group: String,
    val first: IndexClauseOccurrence,
    val second: IndexClauseOccurrence,
)

private data class IndexClauseOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<SqlToken>.isIndexClauseRuleStatement(): Boolean =
    (getOrNull(0).indexClauseRuleHasText("CREATE") && take(5).any { token -> token.indexClauseRuleHasText("INDEX") }) ||
        (getOrNull(0).indexClauseRuleHasText("ALTER") && getOrNull(1).indexClauseRuleHasText("INDEX"))

private fun List<SqlToken>.conflictingIndexClauses(): List<IndexClauseConflict> {
    val firstByGroup = linkedMapOf<String, IndexClauseOccurrence>()
    return indexClauseOccurrences()
        .mapNotNull { occurrence ->
            val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
            first?.let {
                IndexClauseConflict(
                    group = occurrence.group,
                    first = it,
                    second = occurrence,
                )
            }
        }
}

private fun List<SqlToken>.indexClauseOccurrences(): List<IndexClauseOccurrence> =
    mapIndexedNotNull { index, token ->
        when {
            token.indexClauseRuleHasText("UNIQUE") || token.indexClauseRuleHasText("BITMAP") -> {
                token.indexClauseOccurrence("UNIQUE/BITMAP")
            }

            token.indexClauseRuleHasText("LOGGING") || token.indexClauseRuleHasText("NOLOGGING") -> {
                token.indexClauseOccurrence("LOGGING/NOLOGGING")
            }

            token.indexClauseRuleHasText("VISIBLE") || token.indexClauseRuleHasText("INVISIBLE") -> {
                token.indexClauseOccurrence("VISIBLE/INVISIBLE")
            }

            token.indexClauseRuleHasText("USABLE") || token.indexClauseRuleHasText("UNUSABLE") -> {
                token.indexClauseOccurrence("USABLE/UNUSABLE")
            }

            token.indexClauseRuleHasText("COMPRESS") || token.indexClauseRuleHasText("NOCOMPRESS") -> {
                token.indexClauseOccurrence("COMPRESS/NOCOMPRESS")
            }

            token.indexClauseRuleHasText("PARALLEL") || token.indexClauseRuleHasText("NOPARALLEL") -> {
                token.indexClauseOccurrence("PARALLEL/NOPARALLEL")
            }

            token.indexClauseRuleHasText("ONLINE") || token.indexClauseRuleHasText("OFFLINE") -> {
                token.indexClauseOccurrence("ONLINE/OFFLINE")
            }

            token.indexClauseRuleHasText("INDEXING") && getOrNull(index + 1).indexClauseRuleHasText("FULL") -> {
                indexClauseOccurrence(
                    group = "INDEXING FULL/INDEXING PARTIAL",
                    start = token,
                    end = get(index + 1),
                )
            }

            token.indexClauseRuleHasText("INDEXING") && getOrNull(index + 1).indexClauseRuleHasText("PARTIAL") -> {
                indexClauseOccurrence(
                    group = "INDEXING FULL/INDEXING PARTIAL",
                    start = token,
                    end = get(index + 1),
                )
            }

            else -> {
                null
            }
        }
    }

private fun SqlToken.indexClauseOccurrence(group: String): IndexClauseOccurrence =
    IndexClauseOccurrence(
        group = group,
        startOffset = startOffset,
        endOffset = endOffset,
    )

private fun indexClauseOccurrence(
    group: String,
    start: SqlToken,
    end: SqlToken,
): IndexClauseOccurrence =
    IndexClauseOccurrence(
        group = group,
        startOffset = start.startOffset,
        endOffset = end.endOffset,
    )

private fun SqlToken?.indexClauseRuleHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
