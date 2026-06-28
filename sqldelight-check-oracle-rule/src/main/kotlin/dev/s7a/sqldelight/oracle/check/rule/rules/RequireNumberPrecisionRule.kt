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
            if (content.hasExplicitPrecisionAfter(token.endOffset)) return@forEach
            if (content.isExpressionNumberTypeClause(token.startOffset)) return@forEach

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

private fun String.isExpressionNumberTypeClause(startOffset: Int): Boolean {
    val previousWord = previousSqlWord(startOffset) ?: return false
    if (previousWord.text == "RETURNING") return true
    if (previousWord.text == "AS" && isOracleNumberConversionTypeClause(previousWord.startOffset)) return true
    if (previousWord.text != "AS") return false

    val statementStart = lastIndexOf(';', startIndex = startOffset).let { index -> if (index == -1) 0 else index + 1 }
    val prefix = substring(statementStart, startOffset)
    return expressionStatementKeywordPattern.containsMatchIn(prefix)
}

private val expressionStatementKeywordPattern = Regex("""(?is)\b(SELECT|INSERT|UPDATE|DELETE|MERGE|WHERE|VALUES|SET)\b""")

private fun String.isOracleNumberConversionTypeClause(asOffset: Int): Boolean {
    val openParenthesis = containingOpenParenthesisBefore(asOffset) ?: return false
    val functionNameEnd =
        previousIndexSkippingWhitespace(openParenthesis - 1)
            ?.plus(1)
            ?: return false
    val functionNameStart =
        previousWordStart(functionNameEnd)
            ?: return false
    val functionName = substring(functionNameStart, functionNameEnd).uppercase()
    return functionName in oracleNumberConversionTypeFunctions
}

private fun String.containingOpenParenthesisBefore(offset: Int): Int? {
    var depth = 0
    var index = offset - 1
    while (index >= 0) {
        when (this[index]) {
            ')' -> {
                depth++
            }

            '(' -> {
                if (depth == 0) return index
                depth--
            }
        }
        index--
    }
    return null
}

private fun String.previousIndexSkippingWhitespace(index: Int): Int? {
    var current = index
    while (current >= 0 && this[current].isWhitespace()) current--
    return current.takeIf { it >= 0 }
}

private fun String.previousWordStart(endOffset: Int): Int? {
    var index = endOffset - 1
    while (index >= 0 && (this[index].isLetterOrDigit() || this[index] == '_' || this[index] == '$' || this[index] == '#')) index--
    val start = index + 1
    return start.takeIf { it < endOffset }
}

private val oracleNumberConversionTypeFunctions =
    setOf(
        "CAST",
        "TREAT",
        "VALIDATE_CONVERSION",
        "XMLCAST",
    )

private fun String.hasExplicitPrecisionAfter(offset: Int): Boolean {
    var index = offset
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    if (index >= length || this[index] != '(') return false

    index++
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    return index < length && this[index].isDigit()
}
