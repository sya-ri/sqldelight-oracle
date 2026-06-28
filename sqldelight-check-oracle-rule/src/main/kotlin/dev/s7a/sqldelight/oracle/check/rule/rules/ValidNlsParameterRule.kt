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
        nlsParameterFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.first)
            val arguments = content.nlsRuleFunctionArgumentsAt(openParenthesisOffset) ?: return@forEach
            val argument = arguments.getOrNull(2)?.let { content.staticSqlStringLiteral(it.startOffset, it.endOffset) } ?: return@forEach
            val error = validateNlsParameter(functionName, argument.value) ?: return@forEach

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

private val nlsParameterFunctionPattern =
    Regex("""(?i)\b(TO_CHAR|TO_DATE|TO_TIMESTAMP|TO_TIMESTAMP_TZ|TO_NUMBER|TO_BINARY_FLOAT|TO_BINARY_DOUBLE)\s*\(""")

private val nlsAssignmentPattern =
    Regex("""(?i)\b(NLS_DATE_LANGUAGE|NLS_NUMERIC_CHARACTERS|NLS_CURRENCY|NLS_ISO_CURRENCY)\s*=\s*('[^']*'|[A-Za-z_][A-Za-z_ ]*)""")

private fun validateNlsParameter(
    functionName: String,
    value: String,
): String? =
    when (functionName) {
        "TO_DATE", "TO_TIMESTAMP", "TO_TIMESTAMP_TZ" -> validateDatetimeNlsParameter(value)
        "TO_NUMBER", "TO_BINARY_FLOAT", "TO_BINARY_DOUBLE" -> validateNumberNlsParameter(value)
        "TO_CHAR" -> validateToCharNlsParameter(value)
        else -> null
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
