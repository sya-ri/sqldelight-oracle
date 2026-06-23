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
 * Reports static Oracle REGEXP_* match parameters that Oracle rejects at runtime.
 */
public class ValidRegexpMatchParamRule : Rule {
    override val id: RuleId = RuleId("valid-regexp-match-param")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        regexpFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            val matchParamArgumentIndex = regexpMatchParamArgumentIndex[functionName] ?: return@forEach
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.first)
            val arguments = content.functionArgumentsAt(openParenthesisOffset) ?: return@forEach
            val argument = arguments.getOrNull(matchParamArgumentIndex - 1) ?: return@forEach
            val literal = content.staticStringLiteral(argument) ?: return@forEach
            val invalidCharacters = literal.filterNot { character -> character in validRegexpMatchParamCharacters }
            if (invalidCharacters.isEmpty()) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use only Oracle REGEXP_* match_param characters i, c, n, m, and x.",
                    file = context.file,
                    range = content.rangeAtOffsets(argument.startOffset, argument.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private val regexpMatchParamArgumentIndex =
    mapOf(
        "REGEXP_LIKE" to 3,
        "REGEXP_COUNT" to 4,
        "REGEXP_INSTR" to 6,
        "REGEXP_REPLACE" to 6,
        "REGEXP_SUBSTR" to 5,
    )

private val validRegexpMatchParamCharacters = setOf('i', 'c', 'n', 'm', 'x')

private val regexpFunctionPattern =
    Regex("""(?i)\b(REGEXP_LIKE|REGEXP_COUNT|REGEXP_INSTR|REGEXP_REPLACE|REGEXP_SUBSTR)\s*\(""")

private data class ArgumentRange(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.functionArgumentsAt(openParenthesisOffset: Int): List<ArgumentRange>? {
    if (openParenthesisOffset !in indices || this[openParenthesisOffset] != '(') return null

    val arguments = mutableListOf<ArgumentRange>()
    var argumentStart = openParenthesisOffset + 1
    var index = argumentStart
    var depth = 0
    while (index < length) {
        index =
            when {
                startsWith("--", index) -> {
                    skipSqlLineComment(index)
                }

                startsWith("/*", index) -> {
                    skipSqlBlockComment(index)
                }

                this[index] == '\'' -> {
                    skipSqlQuotedString(index)
                }

                this[index] == '(' -> {
                    depth++
                    index + 1
                }

                this[index] == ')' -> {
                    if (depth == 0) {
                        trimmedArgumentRange(argumentStart, index)?.let { arguments += it }
                        return arguments
                    }
                    depth--
                    index + 1
                }

                this[index] == ',' && depth == 0 -> {
                    arguments += trimmedArgumentRange(argumentStart, index) ?: ArgumentRange(index, index)
                    argumentStart = index + 1
                    index + 1
                }

                else -> {
                    index + 1
                }
            }
    }
    return null
}

private fun String.trimmedArgumentRange(
    startOffset: Int,
    endOffset: Int,
): ArgumentRange? {
    var start = startOffset
    var end = endOffset
    while (start < end && this[start].isWhitespace()) start++
    while (end > start && this[end - 1].isWhitespace()) end--
    return if (start < end) ArgumentRange(start, end) else null
}

private fun String.staticStringLiteral(argument: ArgumentRange): String? {
    val text = substring(argument.startOffset, argument.endOffset)
    val literalStart =
        when {
            text.startsWith("N'", ignoreCase = true) -> 1
            text.startsWith("'") -> 0
            else -> return null
        }
    if (!text.endsWith("'")) return null
    return text
        .substring(literalStart + 1, text.lastIndex)
        .replace("''", "'")
        .lowercase()
}
