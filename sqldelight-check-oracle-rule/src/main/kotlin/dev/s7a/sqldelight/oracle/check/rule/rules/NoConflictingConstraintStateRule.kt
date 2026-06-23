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
 * Reports mutually exclusive Oracle constraint state clauses in a single table statement.
 */
public class NoConflictingConstraintStateRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-constraint-state")
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
            .filter { statement -> statement.isConstraintStateRuleStatement() }
            .flatMap { statement -> statement.conflictingConstraintStateClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle constraint state clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class ConstraintStateConflict(
    val group: String,
    val first: ConstraintStateOccurrence,
    val second: ConstraintStateOccurrence,
)

private data class ConstraintStateOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<SqlToken>.isConstraintStateRuleStatement(): Boolean =
    (getOrNull(0).constraintStateRuleHasText("CREATE") && take(5).any { token -> token.constraintStateRuleHasText("TABLE") }) ||
        (getOrNull(0).constraintStateRuleHasText("ALTER") && getOrNull(1).constraintStateRuleHasText("TABLE"))

private fun List<SqlToken>.conflictingConstraintStateClauses(): List<ConstraintStateConflict> {
    val firstByGroup = linkedMapOf<String, ConstraintStateOccurrence>()
    return constraintStateOccurrences()
        .mapNotNull { occurrence ->
            val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
            first?.let {
                ConstraintStateConflict(
                    group = occurrence.group,
                    first = it,
                    second = occurrence,
                )
            }
        }
}

private fun List<SqlToken>.constraintStateOccurrences(): List<ConstraintStateOccurrence> =
    mapIndexedNotNull { index, token ->
        when {
            token.constraintStateRuleHasText("ENABLE") || token.constraintStateRuleHasText("DISABLE") -> {
                token.constraintStateOccurrence("ENABLE/DISABLE")
            }

            token.constraintStateRuleHasText("VALIDATE") || token.constraintStateRuleHasText("NOVALIDATE") -> {
                token.constraintStateOccurrence("VALIDATE/NOVALIDATE")
            }

            token.constraintStateRuleHasText("DEFERRABLE") && getOrNull(index - 1).constraintStateRuleHasText("NOT") -> {
                constraintStateOccurrence(
                    group = "DEFERRABLE/NOT DEFERRABLE",
                    start = get(index - 1),
                    end = token,
                )
            }

            token.constraintStateRuleHasText("DEFERRABLE") -> {
                token.constraintStateOccurrence("DEFERRABLE/NOT DEFERRABLE")
            }

            token.constraintStateRuleHasText("INITIALLY") && getOrNull(index + 1).constraintStateRuleHasText("IMMEDIATE") -> {
                constraintStateOccurrence(
                    group = "INITIALLY IMMEDIATE/INITIALLY DEFERRED",
                    start = token,
                    end = get(index + 1),
                )
            }

            token.constraintStateRuleHasText("INITIALLY") && getOrNull(index + 1).constraintStateRuleHasText("DEFERRED") -> {
                constraintStateOccurrence(
                    group = "INITIALLY IMMEDIATE/INITIALLY DEFERRED",
                    start = token,
                    end = get(index + 1),
                )
            }

            token.constraintStateRuleHasText("RELY") || token.constraintStateRuleHasText("NORELY") -> {
                token.constraintStateOccurrence("RELY/NORELY")
            }

            else -> {
                null
            }
        }
    }

private fun SqlToken.constraintStateOccurrence(group: String): ConstraintStateOccurrence =
    ConstraintStateOccurrence(
        group = group,
        startOffset = startOffset,
        endOffset = endOffset,
    )

private fun constraintStateOccurrence(
    group: String,
    start: SqlToken,
    end: SqlToken,
): ConstraintStateOccurrence =
    ConstraintStateOccurrence(
        group = group,
        startOffset = start.startOffset,
        endOffset = end.endOffset,
    )

private fun SqlToken?.constraintStateRuleHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
