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
 * Reports sequence-trigger identity emulation where Oracle identity columns are clearer.
 */
public class PreferIdentityColumnRule : Rule {
    override val id: RuleId = RuleId("prefer-identity-column")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val statements = content.sqlTokens().toList().sqlStatements()
        if (statements.none { statement -> statement.startsWithCreateSequence() }) return

        statements
            .filter { statement -> statement.isSequenceTrigger() }
            .forEach { statement ->
                val createToken = statement.first()
                val triggerToken = statement[1]
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Prefer an Oracle identity column over sequence-trigger generated keys.",
                        file = context.file,
                        range = content.rangeAtOffsets(createToken.startOffset, triggerToken.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.startsWithCreateSequence(): Boolean = getOrNull(0).hasText("CREATE") && getOrNull(1).hasText("SEQUENCE")

private fun List<SqlToken>.isSequenceTrigger(): Boolean =
    getOrNull(0).hasText("CREATE") &&
        getOrNull(1).hasText("TRIGGER") &&
        assignsSequenceToNewRow()

private fun List<SqlToken>.assignsSequenceToNewRow(): Boolean =
    indices.any { index ->
        val token = get(index)
        val usesSelectIntoNew = getOrNull(index + 1).hasText("INTO") && getOrNull(index + 2).hasText("NEW")
        val followsNewAssignment =
            previousStatementTokenIndex(index)?.let { previousIndex -> get(previousIndex).hasText("NEW") } == true

        token.hasText("NEXTVAL") && (usesSelectIntoNew || followsNewAssignment)
    }

private fun List<SqlToken>.previousStatementTokenIndex(index: Int): Int? =
    (index - 1 downTo 0).firstOrNull { candidate ->
        get(candidate).hasText("NEW") || get(candidate).hasText("INSERT") || get(candidate).hasText("SELECT") ||
            get(candidate).hasText("BEGIN") || get(candidate).hasText("END")
    }

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
