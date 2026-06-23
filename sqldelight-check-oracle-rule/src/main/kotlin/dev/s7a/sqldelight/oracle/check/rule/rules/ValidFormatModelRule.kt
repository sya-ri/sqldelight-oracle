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
 * Reports statically invalid Oracle number and datetime format model literals.
 */
public class ValidFormatModelRule : Rule {
    override val id: RuleId = RuleId("valid-format-model")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskFormatModelCommentsAndQuotedTextPreservingOffsets()
        oracleFormatFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.first)
            val arguments = content.formatModelFunctionArgumentsAt(openParenthesisOffset) ?: return@forEach
            val formatArgument = arguments.getOrNull(1)?.asStaticStringLiteral(content) ?: return@forEach
            val modelKind = formatModelKind(functionName, formatArgument.value) ?: return@forEach
            val error = validateFormatModel(modelKind, formatArgument.value) ?: return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = error.message,
                    file = context.file,
                    range = content.rangeAtOffsets(formatArgument.startOffset, formatArgument.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private const val INVALID_NUMBER_FORMAT_MODEL_MESSAGE =
    "Use a valid Oracle number format model literal."

private const val INVALID_DATETIME_FORMAT_MODEL_MESSAGE =
    "Use a valid Oracle datetime format model literal."

private enum class FormatModelKind {
    Number,
    Date,
    Timestamp,
}

private data class FormatModelError(
    val message: String,
)

private data class FormatModelArgumentRange(
    val startOffset: Int,
    val endOffset: Int,
)

private data class StaticStringLiteral(
    val value: String,
    val startOffset: Int,
    val endOffset: Int,
)

private val oracleFormatFunctionPattern =
    Regex("""(?i)\b(TO_CHAR|TO_DATE|TO_TIMESTAMP|TO_TIMESTAMP_TZ|TO_NUMBER|TO_BINARY_FLOAT|TO_BINARY_DOUBLE)\s*\(""")

private fun formatModelKind(
    functionName: String,
    value: String,
): FormatModelKind? =
    when (functionName) {
        "TO_DATE" -> FormatModelKind.Date
        "TO_TIMESTAMP", "TO_TIMESTAMP_TZ" -> FormatModelKind.Timestamp
        "TO_NUMBER", "TO_BINARY_FLOAT", "TO_BINARY_DOUBLE" -> FormatModelKind.Number
        "TO_CHAR" -> inferToCharFormatModelKind(value)
        else -> null
    }

private fun inferToCharFormatModelKind(value: String): FormatModelKind? {
    val numberModel = tokenizeNumberFormatModel(value) != null
    val datetimeModel = tokenizeDatetimeFormatModel(value) != null
    return when {
        numberModel && !datetimeModel -> FormatModelKind.Number
        datetimeModel && !numberModel -> FormatModelKind.Timestamp
        else -> null
    }
}

private fun validateFormatModel(
    kind: FormatModelKind,
    value: String,
): FormatModelError? =
    when (kind) {
        FormatModelKind.Number -> validateNumberFormatModel(value)
        FormatModelKind.Date -> validateDatetimeFormatModel(value, allowTimestampTokens = false)
        FormatModelKind.Timestamp -> validateDatetimeFormatModel(value, allowTimestampTokens = true)
    }

private fun validateNumberFormatModel(value: String): FormatModelError? {
    val tokens = tokenizeNumberFormatModel(value) ?: return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
    val significantTokens = tokens.filter { token -> token.isNotBlank() }
    if (significantTokens.isEmpty()) return null

    val decimalCount = significantTokens.count { token -> token == "." || token == "D" }
    if (decimalCount > 1) return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)

    val decimalIndex = significantTokens.indexOfFirst { token -> token == "." || token == "D" }
    significantTokens.forEachIndexed { index, token ->
        if ((token == "," || token == "G") && (index == 0 || (decimalIndex != -1 && index > decimalIndex))) {
            return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
        }
        if ((token == "MI" || token == "PR") && index != significantTokens.lastIndex) {
            return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
        }
        if (token == "S" && index != 0 && index != significantTokens.lastIndex) {
            return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
        }
    }

    val tmIndex = significantTokens.indexOf("TM")
    if (tmIndex != -1) {
        val tail = significantTokens.drop(tmIndex + 1)
        if (tmIndex != 0 || tail !in listOf(emptyList<String>(), listOf("9"), listOf("E"), listOf("EEEE"))) {
            return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
        }
    }

    if ("X" in significantTokens && significantTokens.any { token -> token !in setOf("FM", "0", "X") }) {
        return FormatModelError(INVALID_NUMBER_FORMAT_MODEL_MESSAGE)
    }

    return null
}

private fun tokenizeNumberFormatModel(value: String): List<String>? {
    val tokens = mutableListOf<String>()
    var index = 0
    while (index < value.length) {
        val character = value[index]
        when {
            character.isWhitespace() -> {
                index++
            }

            character in setOf(',', '.', '$', '0', '9') -> {
                tokens += character.toString()
                index++
            }

            character.isLetter() -> {
                val token =
                    numberFormatTokens
                        .firstOrNull { token -> value.regionMatches(index, token, 0, token.length, ignoreCase = true) }
                        ?: return null
                tokens += token
                index += token.length
            }

            else -> {
                return null
            }
        }
    }
    return tokens
}

private fun validateDatetimeFormatModel(
    value: String,
    allowTimestampTokens: Boolean,
): FormatModelError? {
    if (value.length > 22) return FormatModelError(INVALID_DATETIME_FORMAT_MODEL_MESSAGE)

    val tokens = tokenizeDatetimeFormatModel(value) ?: return FormatModelError(INVALID_DATETIME_FORMAT_MODEL_MESSAGE)
    if (!allowTimestampTokens && tokens.any { token -> token in timestampOnlyDatetimeTokens || token.matches(ffTokenPattern) }) {
        return FormatModelError(INVALID_DATETIME_FORMAT_MODEL_MESSAGE)
    }
    return null
}

private fun tokenizeDatetimeFormatModel(value: String): List<String>? {
    val tokens = mutableListOf<String>()
    var index = 0
    while (index < value.length) {
        val character = value[index]
        when {
            character.isWhitespace() || character in datetimePunctuation -> {
                index++
            }

            character == '"' -> {
                index = value.skipDoubleQuotedFormatText(index)
            }

            character.isLetter() -> {
                val token =
                    datetimeFormatTokens
                        .firstOrNull { token -> value.regionMatches(index, token, 0, token.length, ignoreCase = true) }
                        ?: return null
                tokens += token
                index += token.length
            }

            else -> {
                return null
            }
        }
    }
    return tokens
}

private fun String.skipDoubleQuotedFormatText(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '"') return index + 1
        index++
    }
    return length
}

private fun FormatModelArgumentRange.asStaticStringLiteral(content: String): StaticStringLiteral? {
    var start = startOffset
    while (start < endOffset && content[start].isWhitespace()) start++
    var end = endOffset
    while (end > start && content[end - 1].isWhitespace()) end--
    if (start >= end) return null
    if (content[start].equals('N', ignoreCase = true)) start++
    if (start >= end || content[start] != '\'') return null
    val literalEnd = content.skipFormatModelQuotedString(start)
    if (literalEnd != end) return null
    return StaticStringLiteral(
        value = content.substring(start + 1, literalEnd - 1).replace("''", "'"),
        startOffset = start,
        endOffset = literalEnd,
    )
}

private fun String.formatModelFunctionArgumentsAt(openParenthesisOffset: Int): List<FormatModelArgumentRange>? {
    if (openParenthesisOffset !in indices || this[openParenthesisOffset] != '(') return null

    var argumentStart = openParenthesisOffset + 1
    val arguments = mutableListOf<FormatModelArgumentRange>()
    var index = argumentStart
    var depth = 0
    while (index < length) {
        index =
            when {
                startsWith("--", index) -> {
                    skipFormatModelLineComment(index)
                }

                startsWith("/*", index) -> {
                    skipFormatModelBlockComment(index)
                }

                this[index] == '\'' -> {
                    skipFormatModelQuotedString(index)
                }

                this[index] == '(' -> {
                    depth++
                    index + 1
                }

                this[index] == ')' -> {
                    if (depth == 0) {
                        if (hasFormatModelNonWhitespace(argumentStart, index)) {
                            arguments += FormatModelArgumentRange(argumentStart, index)
                        }
                        return arguments
                    }
                    depth--
                    index + 1
                }

                this[index] == ',' && depth == 0 -> {
                    arguments += FormatModelArgumentRange(argumentStart, index)
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

private fun String.hasFormatModelNonWhitespace(
    startOffset: Int,
    endOffset: Int,
): Boolean = substring(startOffset, endOffset).any { character -> !character.isWhitespace() }

private fun String.maskFormatModelCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskFormatModelRange(chars, index, skipFormatModelLineComment(index))
                startsWith("/*", index) -> maskFormatModelRange(chars, index, skipFormatModelBlockComment(index))
                chars[index] == '\'' -> maskFormatModelRange(chars, index, skipFormatModelQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipFormatModelLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipFormatModelBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipFormatModelQuotedString(start: Int): Int {
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

private fun maskFormatModelRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private val numberFormatTokens =
    listOf("EEEE", "MI", "PR", "RN", "TM", "FM", "S", "B", "C", "D", "G", "L", "U", "V", "X", "E")

private val datetimeFormatTokens =
    listOf(
        "A.D.",
        "B.C.",
        "P.M.",
        "A.M.",
        "SYYYY",
        "YYYY",
        "RRRR",
        "MONTH",
        "HH24",
        "HH12",
        "SSSSS",
        "TZH",
        "TZM",
        "TZR",
        "TZD",
        "SCC",
        "YYY",
        "MON",
        "DDD",
        "DAY",
        "FF9",
        "FF8",
        "FF7",
        "FF6",
        "FF5",
        "FF4",
        "FF3",
        "FF2",
        "FF1",
        "AD",
        "BC",
        "PM",
        "AM",
        "YY",
        "RR",
        "MM",
        "RM",
        "WW",
        "IW",
        "DD",
        "DY",
        "HH",
        "MI",
        "SS",
        "FF",
        "CC",
        "DL",
        "DS",
        "TS",
        "FM",
        "FX",
        "EE",
        "Y",
        "Q",
        "W",
        "D",
        "J",
        "E",
        "X",
    )

private val timestampOnlyDatetimeTokens = setOf("TZH", "TZM", "TZR", "TZD")

private val datetimePunctuation = setOf('-', '/', ',', '.', ';', ':')

private val ffTokenPattern = Regex("""FF[1-9]?""")
