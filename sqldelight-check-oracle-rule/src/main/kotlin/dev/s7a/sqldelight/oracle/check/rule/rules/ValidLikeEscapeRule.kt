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
        val masked = content.maskLikeEscapeCommentsAndQuotedTextPreservingOffsets()
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
    if (this[index].equals('N', ignoreCase = true) && index + 1 < length && this[index + 1] == '\'') {
        index++
    }
    if (this[index] != '\'') return null
    val end = skipLikeEscapeQuotedString(index)
    if (end > length || end <= index + 1) return null
    return LikeEscapeLiteral(
        value = substring(index + 1, end - 1).replace("''", "'"),
        startOffset = index,
        endOffset = end,
    )
}

private fun String.maskLikeEscapeCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> likeEscapeMaskRange(chars, index, skipLikeEscapeLineComment(index))
                startsWith("/*", index) -> likeEscapeMaskRange(chars, index, skipLikeEscapeBlockComment(index))
                chars[index] == '\'' -> likeEscapeMaskRange(chars, index, skipLikeEscapeQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipLikeEscapeLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipLikeEscapeBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipLikeEscapeQuotedString(start: Int): Int {
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

private fun likeEscapeMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}
