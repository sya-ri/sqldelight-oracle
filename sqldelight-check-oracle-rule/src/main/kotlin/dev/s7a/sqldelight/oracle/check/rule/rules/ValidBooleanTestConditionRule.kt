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
        val masked = content.maskBooleanTestCommentsAndQuotedTextPreservingOffsets()
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

private fun String.maskBooleanTestCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> booleanTestMaskRange(chars, index, skipBooleanTestLineComment(index))
                startsWith("/*", index) -> booleanTestMaskRange(chars, index, skipBooleanTestBlockComment(index))
                chars[index] == '\'' -> booleanTestMaskRange(chars, index, skipBooleanTestQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipBooleanTestLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipBooleanTestBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipBooleanTestQuotedString(start: Int): Int {
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

private fun booleanTestMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}
