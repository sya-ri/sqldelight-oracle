package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.positionAt
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports comparisons against `''`, because Oracle treats zero-length strings as null.
 */
public class NoEmptyStringComparisonRule : Rule {
    override val id: RuleId = RuleId("no-empty-string-comparison")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsPreservingOffsets()
        comparisonPattern.findAll(masked).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Avoid comparing with Oracle empty string literals; Oracle treats '' as NULL, so use IS NULL or IS NOT NULL.",
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}

private val comparisonPattern = Regex("""(?i)(?:=\s*''|''\s*=|(?:<>|!=)\s*''|''\s*(?:<>|!=))""")

private fun String.rangeAtOffsets(
    startOffset: Int,
    endOffset: Int,
): SourceRange =
    SourceRange(
        start = positionAt(startOffset),
        end = positionAt(endOffset),
    )

private fun String.maskSqlCommentsPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskLineComment(chars, index)
                startsWith("/*", index) -> maskBlockComment(chars, index)
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.maskLineComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf('\n', startIndex = start).let { if (it == -1) length else it }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.maskBlockComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }
    for (index in start until end) chars[index] = ' '
    return end
}
