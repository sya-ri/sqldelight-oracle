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
 * Reports statically invalid Oracle NLS parameter literals in conversion functions.
 */
public class ValidNlsParameterRule : Rule {
    override val id: RuleId = RuleId("valid-nls-parameter")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        val columnTypes = content.oracleColumnNlsExpressionKinds(masked)
        nlsParameterFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.first)
            val arguments = content.nlsRuleFunctionArgumentsAt(openParenthesisOffset) ?: return@forEach
            val argument = arguments.getOrNull(2)?.let { content.staticSqlStringLiteral(it.startOffset, it.endOffset) } ?: return@forEach
            val toCharKind =
                if (functionName == "TO_CHAR") {
                    arguments.getOrNull(0)?.let { firstArgument ->
                        content.nlsExpressionKind(
                            startOffset = firstArgument.startOffset,
                            endOffset = firstArgument.endOffset,
                            columnTypes = columnTypes,
                        )
                    }
                } else {
                    null
                }
            val error = validateNlsParameter(functionName, argument.value, toCharKind) ?: return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = error,
                    file = context.file,
                    range = content.rangeAtOffsets(argument.startOffset, argument.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private const val INVALID_DATETIME_NLS_PARAMETER_MESSAGE =
    "Use a valid Oracle datetime NLS parameter literal."

private const val INVALID_NUMBER_NLS_PARAMETER_MESSAGE =
    "Use a valid Oracle number NLS parameter literal."

private data class NlsRuleArgumentRange(
    val startOffset: Int,
    val endOffset: Int,
)

private enum class NlsExpressionKind {
    Datetime,
    Number,
}

private val nlsParameterFunctionPattern =
    Regex("""(?i)\b(TO_CHAR|TO_DATE|TO_TIMESTAMP|TO_TIMESTAMP_TZ|TO_NUMBER|TO_BINARY_FLOAT|TO_BINARY_DOUBLE)\s*\(""")

private val nlsAssignmentPattern =
    Regex("""(?i)\b(NLS_DATE_LANGUAGE|NLS_NUMERIC_CHARACTERS|NLS_CURRENCY|NLS_ISO_CURRENCY)\s*=\s*('[^']*'|[A-Za-z_][A-Za-z_ ]*)""")

private fun validateNlsParameter(
    functionName: String,
    value: String,
    toCharKind: NlsExpressionKind?,
): String? =
    when (functionName) {
        "TO_DATE", "TO_TIMESTAMP", "TO_TIMESTAMP_TZ" -> {
            validateDatetimeNlsParameter(value)
        }

        "TO_NUMBER", "TO_BINARY_FLOAT", "TO_BINARY_DOUBLE" -> {
            validateNumberNlsParameter(value)
        }

        "TO_CHAR" -> {
            when (toCharKind) {
                NlsExpressionKind.Datetime -> {
                    validateDatetimeNlsParameter(value)
                }

                NlsExpressionKind.Number -> {
                    validateNumberNlsParameter(value)
                }

                null -> {
                    validateToCharNlsParameter(value)
                }
            }
        }

        else -> {
            null
        }
    }

private fun validateToCharNlsParameter(value: String): String? =
    when {
        value.contains("NLS_DATE_LANGUAGE", ignoreCase = true) -> validateDatetimeNlsParameter(value)
        else -> validateNumberNlsParameter(value)
    }

private fun validateDatetimeNlsParameter(value: String): String? {
    val assignments = parseNlsAssignments(value) ?: return INVALID_DATETIME_NLS_PARAMETER_MESSAGE
    if (assignments.size != 1) return INVALID_DATETIME_NLS_PARAMETER_MESSAGE
    val assignment = assignments.single()
    if (!assignment.name.equals("NLS_DATE_LANGUAGE", ignoreCase = true)) return INVALID_DATETIME_NLS_PARAMETER_MESSAGE
    if (!assignment.value.matches(Regex("""[A-Za-z_][A-Za-z_ ]*"""))) return INVALID_DATETIME_NLS_PARAMETER_MESSAGE
    return null
}

private fun validateNumberNlsParameter(value: String): String? {
    val assignments = parseNlsAssignments(value) ?: return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
    if (assignments.isEmpty()) return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
    assignments.forEach { assignment ->
        when (assignment.name.uppercase()) {
            "NLS_NUMERIC_CHARACTERS" -> {
                if (!assignment.quoted || assignment.value.length != 2 || assignment.value[0] == assignment.value[1]) {
                    return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
                }
            }

            "NLS_CURRENCY" -> {
                if (!assignment.quoted || assignment.value.isEmpty() || assignment.value.length > 10) {
                    return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
                }
            }

            "NLS_ISO_CURRENCY" -> {
                if (!assignment.value.matches(Regex("""[A-Za-z_][A-Za-z_ ]*"""))) {
                    return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
                }
            }

            else -> {
                return INVALID_NUMBER_NLS_PARAMETER_MESSAGE
            }
        }
    }
    return null
}

private data class NlsAssignment(
    val name: String,
    val value: String,
    val quoted: Boolean,
)

private fun parseNlsAssignments(value: String): List<NlsAssignment>? {
    val assignments = mutableListOf<NlsAssignment>()
    var end = 0
    nlsAssignmentPattern.findAll(value).forEach { match ->
        if (value.substring(end, match.range.first).isNotBlank()) return null
        val rawValue = match.groupValues[2].trim()
        assignments +=
            NlsAssignment(
                name = match.groupValues[1],
                value = rawValue.removeSurrounding("'"),
                quoted = rawValue.startsWith("'") && rawValue.endsWith("'"),
            )
        end = match.range.last + 1
    }
    if (value.substring(end).isNotBlank()) return null
    return assignments
}

private fun String.nlsExpressionKind(
    startOffset: Int,
    endOffset: Int,
    columnTypes: Map<String, NlsExpressionKind?>,
): NlsExpressionKind? {
    val expression = substring(startOffset, endOffset).trim()
    if (expression.isEmpty()) return null

    expression.nlsCastTargetType()?.let { return it.oracleTypeNlsExpressionKind() }
    expression.nlsFunctionReturnKind()?.let { return it }
    expression.nlsLiteralKind()?.let { return it }

    val columnName = expression.nlsColumnReferenceName() ?: return null
    return columnTypes[normalizeOracleIdentifier(columnName)]
}

private fun String.nlsLiteralKind(): NlsExpressionKind? {
    val value = trim()
    if (value.matches(Regex("""(?i)(DATE|TIMESTAMP)\s*'.*'""", RegexOption.DOT_MATCHES_ALL))) {
        return NlsExpressionKind.Datetime
    }
    if (value.matches(Regex("""[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?[fFdD]?"""))) {
        return NlsExpressionKind.Number
    }
    return null
}

private fun String.nlsFunctionReturnKind(): NlsExpressionKind? {
    val functionName = trimStart().takeWhile { character -> character.isLetterOrDigit() || character == '_' }.uppercase()
    return when (functionName) {
        "CURRENT_DATE",
        "CURRENT_TIMESTAMP",
        "LOCALTIMESTAMP",
        "SYSDATE",
        "SYSTIMESTAMP",
        "TO_DATE",
        "TO_TIMESTAMP",
        "TO_TIMESTAMP_TZ",
        -> NlsExpressionKind.Datetime

        "ABS",
        "ACOS",
        "ASIN",
        "ATAN",
        "ATAN2",
        "COS",
        "COSH",
        "EXP",
        "LN",
        "LOG",
        "MOD",
        "POWER",
        "SIGN",
        "SIN",
        "SINH",
        "SQRT",
        "TAN",
        "TANH",
        "TO_BINARY_DOUBLE",
        "TO_BINARY_FLOAT",
        "TO_NUMBER",
        -> NlsExpressionKind.Number

        else -> null
    }
}

private fun String.nlsCastTargetType(): String? {
    val value = trim()
    if (!value.startsWith("CAST", ignoreCase = true)) return null
    val asOffset = value.indexOfTopLevelKeyword("AS") ?: return null
    val closeOffset = value.lastIndexOf(')')
    if (closeOffset <= asOffset) return null
    return value.substring(asOffset + 2, closeOffset).trim()
}

private fun String.nlsColumnReferenceName(): String? {
    val value = trim()
    if (!value.matches(nlsColumnReferencePattern)) {
        return null
    }
    return value.substringAfterLast('.').trim()
}

private val nlsColumnReferencePattern =
    Regex(
        """(?i)"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*|("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)\s*\.\s*("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)""",
    )

private fun String.oracleColumnNlsExpressionKinds(masked: String): Map<String, NlsExpressionKind?> {
    val types = linkedMapOf<String, NlsExpressionKind?>()
    Regex("""(?i)\bCREATE\s+(?:GLOBAL\s+TEMPORARY\s+|PRIVATE\s+TEMPORARY\s+)?TABLE\s+""")
        .findAll(masked)
        .forEach { match ->
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.last + 1)
            if (openParenthesisOffset == -1) return@forEach
            val closeParenthesisOffset = masked.findMatchingParenthesis(openParenthesisOffset) ?: return@forEach
            splitTopLevelRanges(openParenthesisOffset + 1, closeParenthesisOffset).forEach { range ->
                val column = substring(range.first, range.last + 1).trim()
                val columnName = column.leadingOracleIdentifier() ?: return@forEach
                if (columnName.isOracleTableConstraintName()) return@forEach
                val typeText = column.substringAfter(columnName).trimStart()
                val kind = typeText.oracleTypeNlsExpressionKind()
                val normalizedName = normalizeOracleIdentifier(columnName)
                types[normalizedName] =
                    when (val existing = types[normalizedName]) {
                        null -> if (types.containsKey(normalizedName)) null else kind
                        kind -> kind
                        else -> null
                    }
            }
        }
    return types
}

private fun String.oracleTypeNlsExpressionKind(): NlsExpressionKind? {
    val normalized = trimStart().uppercase()
    val leadingType = normalized.takeWhile { character -> character.isLetter() || character == '_' }
    return when (leadingType) {
        "DATE", "TIMESTAMP" -> NlsExpressionKind.Datetime

        "BINARY_DOUBLE",
        "BINARY_FLOAT",
        "DEC",
        "DECIMAL",
        "DOUBLE",
        "FLOAT",
        "INT",
        "INTEGER",
        "NUMBER",
        "NUMERIC",
        "REAL",
        "SMALLINT",
        -> NlsExpressionKind.Number

        else -> null
    }
}

private fun String.leadingOracleIdentifier(): String? =
    when {
        startsWith("\"") -> {
            val end = indexOf('"', startIndex = 1)
            if (end == -1) null else substring(0, end + 1)
        }

        firstOrNull()?.let { character -> character.isLetter() || character == '_' } == true -> {
            takeWhile { character -> character.isLetterOrDigit() || character == '_' || character == '$' || character == '#' }
        }

        else -> {
            null
        }
    }

private fun String.isOracleTableConstraintName(): Boolean = removeSurrounding("\"").uppercase() in oracleTableConstraintNames

private fun normalizeOracleIdentifier(value: String): String = value.trim().removeSurrounding("\"").uppercase()

private val oracleTableConstraintNames =
    setOf("CONSTRAINT", "PRIMARY", "UNIQUE", "FOREIGN", "CHECK", "SUPPLEMENTAL")

private fun String.nlsRuleFunctionArgumentsAt(openParenthesisOffset: Int): List<NlsRuleArgumentRange>? {
    if (openParenthesisOffset !in indices || this[openParenthesisOffset] != '(') return null

    var argumentStart = openParenthesisOffset + 1
    val arguments = mutableListOf<NlsRuleArgumentRange>()
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

                startsSqlAlternativeQuotedString(index) -> {
                    skipSqlAlternativeQuotedString(index)
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
                        if (substring(argumentStart, index).isNotBlank()) arguments += NlsRuleArgumentRange(argumentStart, index)
                        return arguments
                    }
                    depth--
                    index + 1
                }

                this[index] == ',' && depth == 0 -> {
                    arguments += NlsRuleArgumentRange(argumentStart, index)
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

private fun String.findMatchingParenthesis(openParenthesisOffset: Int): Int? {
    if (openParenthesisOffset !in indices || this[openParenthesisOffset] != '(') return null

    var index = openParenthesisOffset + 1
    var depth = 0
    while (index < length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                if (depth == 0) return index
                depth--
            }
        }
        index++
    }
    return null
}

private fun String.splitTopLevelRanges(
    startOffset: Int,
    endOffset: Int,
): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var itemStart = startOffset
    var index = startOffset
    var depth = 0
    while (index < endOffset) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                if (depth > 0) depth--
            }

            ',' -> {
                if (depth == 0) {
                    if (substring(itemStart, index).isNotBlank()) {
                        ranges += itemStart until index
                    }
                    itemStart = index + 1
                }
            }
        }
        index++
    }
    if (substring(itemStart, endOffset).isNotBlank()) ranges += itemStart until endOffset
    return ranges
}

private fun String.indexOfTopLevelKeyword(keyword: String): Int? {
    var index = 0
    var depth = 0
    while (index <= length - keyword.length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                if (depth > 0) depth--
            }
        }
        if (
            depth == 1 &&
            regionMatches(index, keyword, 0, keyword.length, ignoreCase = true) &&
            (index == 0 || !this[index - 1].isLetterOrDigit()) &&
            (index + keyword.length == length || !this[index + keyword.length].isLetterOrDigit())
        ) {
            return index
        }
        index++
    }
    return null
}
