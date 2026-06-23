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
 * Reports traditional Oracle AUDIT and NOAUDIT statements.
 */
public class PreferUnifiedAuditingRule : Rule {
    override val id: RuleId = RuleId("prefer-unified-auditing")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        traditionalAuditingStatementPattern.findAll(masked).forEach { match ->
            val statementKeyword = match.groups[1] ?: return@forEach
            val trailingContent = masked.substring(statementKeyword.range.last + 1)
            if (trailingContent.startsWithUnifiedAuditingClause()) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Prefer Oracle unified auditing policies over traditional AUDIT and NOAUDIT statements.",
                    file = context.file,
                    range =
                        content.rangeAtOffsets(
                            statementKeyword.range.first,
                            statementKeyword.range.last + 1,
                        ),
                    database = context.database,
                ),
            )
        }
    }
}

private val traditionalAuditingStatementPattern = Regex("""(?im)^\s*(AUDIT|NOAUDIT)\b""")

private fun String.startsWithUnifiedAuditingClause(): Boolean {
    val clause = trimStart()
    return clause.startsWith("POLICY", ignoreCase = true) ||
        clause.startsWith("CONTEXT", ignoreCase = true)
}
