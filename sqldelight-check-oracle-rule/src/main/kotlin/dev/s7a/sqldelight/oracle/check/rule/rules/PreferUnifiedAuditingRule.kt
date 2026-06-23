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
        val masked = content.maskAuditRuleCommentsAndQuotedTextPreservingOffsets()
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

private fun String.maskAuditRuleCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> {
                    auditRuleMaskRange(chars, index, skipAuditRuleLineComment(index))
                }

                startsWith("/*", index) -> {
                    auditRuleMaskRange(chars, index, skipAuditRuleBlockComment(index))
                }

                chars[index] == '\'' -> {
                    auditRuleMaskRange(chars, index, skipAuditRuleQuotedString(index))
                }

                else -> {
                    index + 1
                }
            }
    }
    return String(chars)
}

private fun String.skipAuditRuleLineComment(start: Int): Int = indexOf('\n', startIndex = start).let {
    if (it == -1) length else it
}

private fun String.skipAuditRuleBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipAuditRuleQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'') {
            if (index + 1 < length && this[index + 1] == '\'') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun auditRuleMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}
