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
 * Reports fixed-arity Oracle built-in function calls with the wrong number of arguments.
 */
public class ValidFunctionArityRule : Rule {
    override val id: RuleId = RuleId("valid-function-arity")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskCommentsAndQuotedTextPreservingOffsets()
        oracleFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            val arity = oracleFunctionArities[functionName] ?: return@forEach
            val openParenthesisOffset = masked.indexOf('(', startIndex = match.range.first)
            val argumentCount = content.functionArgumentCountAt(openParenthesisOffset) ?: return@forEach
            if (argumentCount in arity) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Oracle function $functionName expects ${arity.display()} argument(s), but got $argumentCount.",
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.first + functionName.length),
                    database = context.database,
                ),
            )
        }
    }
}

private data class FunctionArity(
    val min: Int,
    val max: Int,
) {
    operator fun contains(argumentCount: Int): Boolean = argumentCount in min..max

    fun display(): String = if (min == max) min.toString() else "$min..$max"
}

private fun exactArity(count: Int): FunctionArity = FunctionArity(count, count)

private fun arityRange(
    min: Int,
    max: Int,
): FunctionArity = FunctionArity(min, max)

private val oracleFunctionArities =
    mapOf(
        "CURRENT_DATE" to exactArity(0),
        "DBTIMEZONE" to exactArity(0),
        "EMPTY_BLOB" to exactArity(0),
        "EMPTY_CLOB" to exactArity(0),
        "ORA_INVOKING_USER" to exactArity(0),
        "ORA_INVOKING_USERID" to exactArity(0),
        "SESSIONTIMEZONE" to exactArity(0),
        "SYSDATE" to exactArity(0),
        "SYSTIMESTAMP" to exactArity(0),
        "SYS_GUID" to exactArity(0),
        "UID" to exactArity(0),
        "USER" to exactArity(0),
        "UUID" to exactArity(0),
        "ABS" to exactArity(1),
        "ACOS" to exactArity(1),
        "ASCII" to exactArity(1),
        "ASCIISTR" to exactArity(1),
        "ASIN" to exactArity(1),
        "CEIL" to exactArity(1),
        "CHARTOROWID" to exactArity(1),
        "COS" to exactArity(1),
        "COSH" to exactArity(1),
        "EXP" to exactArity(1),
        "FLOOR" to exactArity(1),
        "HEXTORAW" to exactArity(1),
        "INITCAP" to exactArity(1),
        "IS_UUID" to exactArity(1),
        "LAST_DAY" to exactArity(1),
        "LENGTH" to exactArity(1),
        "LN" to exactArity(1),
        "LNNVL" to exactArity(1),
        "LOWER" to exactArity(1),
        "NCHR" to exactArity(1),
        "RAW_TO_UUID" to exactArity(1),
        "RAWTOHEX" to exactArity(1),
        "RAWTONHEX" to exactArity(1),
        "ROWIDTOCHAR" to exactArity(1),
        "ROWIDTONCHAR" to exactArity(1),
        "SCN_TO_TIMESTAMP" to exactArity(1),
        "SIGN" to exactArity(1),
        "SIN" to exactArity(1),
        "SINH" to exactArity(1),
        "SOUNDEX" to exactArity(1),
        "SQRT" to exactArity(1),
        "SYS_EXTRACT_UTC" to exactArity(1),
        "TAN" to exactArity(1),
        "TANH" to exactArity(1),
        "TO_BLOB" to exactArity(1),
        "TO_CLOB" to exactArity(1),
        "TO_DSINTERVAL" to exactArity(1),
        "TO_LOB" to exactArity(1),
        "TO_MULTI_BYTE" to exactArity(1),
        "TO_NCHAR" to exactArity(1),
        "TO_NCLOB" to exactArity(1),
        "TO_SINGLE_BYTE" to exactArity(1),
        "TO_YMINTERVAL" to exactArity(1),
        "UNISTR" to exactArity(1),
        "UPPER" to exactArity(1),
        "UUID_TO_RAW" to exactArity(1),
        "VSIZE" to exactArity(1),
        "ADD_MONTHS" to exactArity(2),
        "ATAN2" to exactArity(2),
        "BITAND" to exactArity(2),
        "CONCAT" to exactArity(2),
        "FROM_TZ" to exactArity(2),
        "MOD" to exactArity(2),
        "MONTHS_BETWEEN" to exactArity(2),
        "NANVL" to exactArity(2),
        "NULLIF" to exactArity(2),
        "NUMTODSINTERVAL" to exactArity(2),
        "NUMTOYMINTERVAL" to exactArity(2),
        "POWER" to exactArity(2),
        "REMAINDER" to exactArity(2),
        "NEW_TIME" to exactArity(3),
        "NVL2" to exactArity(3),
        "TRANSLATE" to exactArity(3),
        "COALESCE" to arityRange(2, Int.MAX_VALUE),
        "GREATEST" to arityRange(1, Int.MAX_VALUE),
        "LEAST" to arityRange(1, Int.MAX_VALUE),
        "NVL" to exactArity(2),
        "REGEXP_COUNT" to arityRange(2, 4),
        "REGEXP_INSTR" to arityRange(2, 7),
        "REGEXP_LIKE" to arityRange(2, 3),
        "REGEXP_REPLACE" to arityRange(2, 6),
        "REGEXP_SUBSTR" to arityRange(2, 6),
        "TO_BINARY_DOUBLE" to arityRange(1, 3),
        "TO_BINARY_FLOAT" to arityRange(1, 3),
        "TO_CHAR" to arityRange(1, 3),
        "TO_DATE" to arityRange(1, 3),
        "TO_NUMBER" to arityRange(1, 3),
        "TO_TIMESTAMP" to arityRange(1, 3),
        "TO_TIMESTAMP_TZ" to arityRange(1, 3),
    )

private val oracleFunctionPattern =
    Regex("""(?i)\b(${oracleFunctionArities.keys.joinToString("|") { Regex.escape(it) }})\s*\(""")

private fun String.functionArgumentCountAt(openParenthesisOffset: Int): Int? {
    if (openParenthesisOffset !in indices || this[openParenthesisOffset] != '(') return null

    var argumentStart = openParenthesisOffset + 1
    var argumentCount = 0
    var index = argumentStart
    var depth = 0
    while (index < length) {
        index =
            when {
                startsWith("--", index) -> {
                    skipLineComment(index)
                }

                startsWith("/*", index) -> {
                    skipBlockComment(index)
                }

                this[index] == '\'' -> {
                    skipQuotedString(index)
                }

                this[index] == '(' -> {
                    depth++
                    index + 1
                }

                this[index] == ')' -> {
                    if (depth == 0) {
                        if (hasNonWhitespace(argumentStart, index)) argumentCount++
                        return argumentCount
                    }
                    depth--
                    index + 1
                }

                this[index] == ',' && depth == 0 -> {
                    argumentCount++
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

private fun String.hasNonWhitespace(
    startOffset: Int,
    endOffset: Int,
): Boolean = substring(startOffset, endOffset).any { character -> !character.isWhitespace() }

private fun String.maskCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskRange(chars, index, skipLineComment(index))
                startsWith("/*", index) -> maskRange(chars, index, skipBlockComment(index))
                chars[index] == '\'' -> maskRange(chars, index, skipQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipBlockComment(start: Int): Int = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipQuotedString(start: Int): Int {
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

private fun maskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}
