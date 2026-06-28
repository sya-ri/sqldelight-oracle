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
 * Reports static Oracle LIKE ESCAPE literals that are not exactly one character.
 */
public class ValidLikeEscapeRule : Rule {
    override val id: RuleId = RuleId("valid-like-escape")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        likeEscapePattern.findAll(masked).forEach { match ->
            val literal = content.staticLikeEscapeLiteralAfter(match.range.last + 1) ?: return@forEach
            if (literal.value.codePointCount(0, literal.value.length) == 1) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use exactly one character in static Oracle LIKE ESCAPE literals.",
                    file = context.file,
                    range = content.rangeAtOffsets(literal.startOffset, literal.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class LikeEscapeLiteral(
    val value: String,
    val startOffset: Int,
    val endOffset: Int,
)

private val likeEscapePattern = Regex("""(?i)\bESCAPE\b""")

private fun String.staticLikeEscapeLiteralAfter(offset: Int): LikeEscapeLiteral? {
    var index = offset
    while (index < length && this[index].isWhitespace()) index++
    if (index >= length) return null
    if (startsSqlAlternativeQuotedString(index)) {
        val end = skipSqlAlternativeQuotedString(index)
        val qIndex = if (this[index].equals('q', ignoreCase = true)) index else index + 1
        return LikeEscapeLiteral(
            value = substring(qIndex + 3, end - 2),
            startOffset = index,
            endOffset = end,
        )
    }
    if (this[index].equals('N', ignoreCase = true) && index + 1 < length && this[index + 1] == '\'') {
        index++
    }
    if (this[index] != '\'') return null
    val end = skipSqlQuotedString(index)
    if (end > length || end <= index + 1) return null
    return LikeEscapeLiteral(
        value = substring(index + 1, end - 1).replace("''", "'"),
        startOffset = index,
        endOffset = end,
    )
}
