package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlTokens
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports bare `NUMBER` type declarations because Oracle precision and scale affect generated Kotlin types.
 */
public class RequireNumberPrecisionRule : Rule {
    override val id: RuleId = RuleId("require-number-precision")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.sqlTokens().forEach { token ->
            if (!token.text.equals("NUMBER", ignoreCase = true)) return@forEach
            if (content.hasPrecisionAfter(token.endOffset)) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Declare Oracle NUMBER with explicit precision and scale, such as NUMBER(19) or NUMBER(10, 2).",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.hasPrecisionAfter(offset: Int): Boolean {
    var index = offset
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    return index < length && this[index] == '('
}
