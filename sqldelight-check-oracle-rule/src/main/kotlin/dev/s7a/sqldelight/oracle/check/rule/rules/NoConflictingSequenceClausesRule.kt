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
 * Reports mutually exclusive Oracle sequence clauses in a single sequence statement.
 */
public class NoConflictingSequenceClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-sequence-clauses")
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
            .filter { statement -> statement.isSequenceStatement() }
            .flatMap { statement -> statement.conflictingSequenceClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle sequence clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SequenceClauseConflict(
    val group: String,
    val first: SqlToken,
    val second: SqlToken,
)

private data class SequenceClauseOccurrence(
    val group: String,
    val token: SqlToken,
)

private fun List<SqlToken>.isSequenceStatement(): Boolean =
    (getOrNull(0).hasText("CREATE") || getOrNull(0).hasText("ALTER")) && getOrNull(1).hasText("SEQUENCE")

private fun List<SqlToken>.conflictingSequenceClauses(): List<SequenceClauseConflict> {
    val firstByGroup = linkedMapOf<String, SqlToken>()
    return sequenceClauseOccurrences()
        .mapNotNull { occurrence ->
            val first = firstByGroup.putIfAbsent(occurrence.group, occurrence.token)
            first?.let {
                SequenceClauseConflict(
                    group = occurrence.group,
                    first = it,
                    second = occurrence.token,
                )
            }
        }
}

private fun List<SqlToken>.sequenceClauseOccurrences(): List<SequenceClauseOccurrence> =
    mapIndexedNotNull { index, token ->
        val group =
            when {
                token.hasText("SHARING") -> "SHARING"
                token.hasText("INCREMENT") && getOrNull(index + 1).hasText("BY") -> "INCREMENT BY"
                token.hasText("START") && getOrNull(index + 1).hasText("WITH") -> "START WITH"
                token.hasText("RESTART") -> "RESTART"
                token.hasText("MAXVALUE") || token.hasText("NOMAXVALUE") -> "MAXVALUE/NOMAXVALUE"
                token.hasText("MINVALUE") || token.hasText("NOMINVALUE") -> "MINVALUE/NOMINVALUE"
                token.hasText("CYCLE") || token.hasText("NOCYCLE") -> "CYCLE/NOCYCLE"
                token.hasText("CACHE") || token.hasText("NOCACHE") -> "CACHE/NOCACHE"
                token.hasText("ORDER") || token.hasText("NOORDER") -> "ORDER/NOORDER"
                token.hasText("KEEP") || token.hasText("NOKEEP") -> "KEEP/NOKEEP"
                token.hasText("SCALE") || token.hasText("NOSCALE") -> "SCALE/NOSCALE"
                token.hasText("SHARD") || token.hasText("NOSHARD") -> "SHARD/NOSHARD"
                token.hasText("SESSION") || token.hasText("GLOBAL") -> "SESSION/GLOBAL"
                else -> null
            }
        group?.let { SequenceClauseOccurrence(group = it, token = token) }
    }

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
