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
 * Reports statically invalid Oracle boolean test conditions.
 */
public class ValidBooleanTestConditionRule : Rule {
    override val id: RuleId = RuleId("valid-boolean-test-condition")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        invalidBooleanTestPattern.findAll(masked).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use a valid Oracle boolean test condition.",
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}

private val invalidBooleanTestPattern = Regex("""(?i)\bIS\s+NOT\s+NOT\s+(TRUE|FALSE|UNKNOWN)\b""")
