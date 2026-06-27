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
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        oracleFunctionPattern.findAll(masked).forEach { match ->
            val functionName = match.groupValues[1].uppercase()
            if (functionName in oracleNoParenthesesExpressions) {
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Oracle expression $functionName does not accept parentheses.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.first + functionName.length),
                        database = context.database,
                    ),
                )
                return@forEach
            }
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
        "EMPTY_BLOB" to exactArity(0),
        "EMPTY_CLOB" to exactArity(0),
        "SYS_GUID" to exactArity(0),
        "UUID" to exactArity(0),
        "ABS" to exactArity(1),
        "ACOS" to exactArity(1),
        "APPROX_COUNT" to exactArity(1),
        "APPROX_COUNT_DISTINCT" to exactArity(1),
        "APPROX_MEDIAN" to exactArity(1),
        "APPROX_SUM" to exactArity(1),
        "ANY_VALUE" to exactArity(1),
        "ASCII" to exactArity(1),
        "ASCIISTR" to exactArity(1),
        "ASIN" to exactArity(1),
        "ATAN" to exactArity(1),
        "AVG" to exactArity(1),
        "BFILENAME" to exactArity(2),
        "BIN_TO_NUM" to arityRange(1, Int.MAX_VALUE),
        "BIT_AND_AGG" to exactArity(1),
        "BIT_OR_AGG" to exactArity(1),
        "BIT_XOR_AGG" to exactArity(1),
        "BOOLEAN_AND_AGG" to exactArity(1),
        "BOOLEAN_OR_AGG" to exactArity(1),
        "CARDINALITY" to exactArity(1),
        "CEIL" to exactArity(1),
        "CHECKSUM" to exactArity(1),
        "CHARTOROWID" to exactArity(1),
        "CHR" to exactArity(1),
        "COLLATION" to exactArity(1),
        "COMPOSE" to exactArity(1),
        "CON_DBID_TO_ID" to exactArity(1),
        "CON_GUID_TO_ID" to exactArity(1),
        "CON_NAME_TO_ID" to exactArity(1),
        "CON_UID_TO_ID" to exactArity(1),
        "COS" to exactArity(1),
        "COSH" to exactArity(1),
        "DECOMPOSE" to arityRange(1, 2),
        "DOMAIN_CHECK" to exactArity(2),
        "DOMAIN_CHECK_TYPE" to exactArity(2),
        "DOMAIN_NAME" to exactArity(1),
        "DUMP" to arityRange(1, 4),
        "EXP" to exactArity(1),
        "EXISTSNODE" to arityRange(2, 3),
        "EXTRACTVALUE" to arityRange(2, 3),
        "FIRST_VALUE" to exactArity(1),
        "FLOOR" to exactArity(1),
        "FROM_VECTOR" to exactArity(1),
        "GROUPING" to exactArity(1),
        "GROUPING_ID" to arityRange(1, Int.MAX_VALUE),
        "GROUP_ID" to exactArity(0),
        "HAMMING_DISTANCE" to exactArity(2),
        "HEXTORAW" to exactArity(1),
        "INITCAP" to exactArity(1),
        "INNER_PRODUCT" to exactArity(2),
        "IS_UUID" to exactArity(1),
        "JACCARD_DISTANCE" to exactArity(2),
        "L1_DISTANCE" to exactArity(2),
        "L2_DISTANCE" to exactArity(2),
        "LAST_DAY" to exactArity(1),
        "LAST_VALUE" to exactArity(1),
        "LAG" to arityRange(1, 3),
        "LEAD" to arityRange(1, 3),
        "LENGTH" to exactArity(1),
        "LN" to exactArity(1),
        "LNNVL" to exactArity(1),
        "LOWER" to exactArity(1),
        "LTRIM" to arityRange(1, 2),
        "MAX" to exactArity(1),
        "MEDIAN" to exactArity(1),
        "MIN" to exactArity(1),
        "NLS_CHARSET_DECL_LEN" to exactArity(2),
        "NLS_CHARSET_ID" to exactArity(1),
        "NLS_CHARSET_NAME" to exactArity(1),
        "NLS_COLLATION_ID" to exactArity(1),
        "NLS_COLLATION_NAME" to exactArity(1),
        "NLS_INITCAP" to arityRange(1, 2),
        "NLS_LOWER" to arityRange(1, 2),
        "NLS_UPPER" to arityRange(1, 2),
        "NLSSORT" to arityRange(1, 2),
        "NCHR" to exactArity(1),
        "NTH_VALUE" to exactArity(2),
        "NTILE" to exactArity(1),
        "ORA_DST_AFFECTED" to exactArity(1),
        "ORA_DST_CONVERT" to exactArity(1),
        "ORA_DST_ERROR" to exactArity(1),
        "ORA_HASH" to arityRange(1, 3),
        "PHONIC_ENCODE" to arityRange(2, 3),
        "RAW_TO_UUID" to exactArity(1),
        "RAWTOHEX" to exactArity(1),
        "RAWTONHEX" to exactArity(1),
        "REPLACE" to arityRange(2, 3),
        "ROWIDTOCHAR" to exactArity(1),
        "ROWIDTONCHAR" to exactArity(1),
        "ROW_NUMBER" to exactArity(0),
        "RTRIM" to arityRange(1, 2),
        "SCN_TO_TIMESTAMP" to exactArity(1),
        "SIGN" to exactArity(1),
        "SIN" to exactArity(1),
        "SINH" to exactArity(1),
        "SOUNDEX" to exactArity(1),
        "SQRT" to exactArity(1),
        "STATS_MODE" to exactArity(1),
        "STANDARD_HASH" to arityRange(1, 2),
        "SUM" to exactArity(1),
        "SYS_CONTEXT" to arityRange(2, 3),
        "SYS_CONNECT_BY_PATH" to exactArity(2),
        "SYS_EXTRACT_UTC" to exactArity(1),
        "SYS_TYPEID" to exactArity(1),
        "TAN" to exactArity(1),
        "TANH" to exactArity(1),
        "TIMESTAMP_TO_SCN" to exactArity(1),
        "TO_BLOB" to exactArity(1),
        "TO_CLOB" to exactArity(1),
        "TO_DSINTERVAL" to exactArity(1),
        "TO_LOB" to exactArity(1),
        "TO_MULTI_BYTE" to exactArity(1),
        "TO_NCHAR" to exactArity(1),
        "TO_NCLOB" to exactArity(1),
        "TO_SINGLE_BYTE" to exactArity(1),
        "TO_YMINTERVAL" to exactArity(1),
        "TZ_OFFSET" to exactArity(1),
        "UNISTR" to exactArity(1),
        "UPPER" to exactArity(1),
        "UUID_TO_RAW" to exactArity(1),
        "VSIZE" to exactArity(1),
        "ADD_MONTHS" to exactArity(2),
        "ATAN2" to exactArity(2),
        "BITAND" to exactArity(2),
        "COUNT" to exactArity(1),
        "FROM_TZ" to exactArity(2),
        "FUZZY_MATCH" to arityRange(3, Int.MAX_VALUE),
        "LOG" to exactArity(2),
        "MOD" to exactArity(2),
        "MONTHS_BETWEEN" to exactArity(2),
        "NANVL" to exactArity(2),
        "NEXT_DAY" to exactArity(2),
        "NULLIF" to exactArity(2),
        "NUMTODSINTERVAL" to exactArity(2),
        "NUMTOYMINTERVAL" to exactArity(2),
        "POWER" to exactArity(2),
        "REMAINDER" to exactArity(2),
        "REGR_AVGX" to exactArity(2),
        "REGR_AVGY" to exactArity(2),
        "REGR_COUNT" to exactArity(2),
        "REGR_INTERCEPT" to exactArity(2),
        "REGR_R2" to exactArity(2),
        "REGR_SLOPE" to exactArity(2),
        "REGR_SXX" to exactArity(2),
        "REGR_SXY" to exactArity(2),
        "REGR_SYY" to exactArity(2),
        "RATIO_TO_REPORT" to exactArity(1),
        "WIDTH_BUCKET" to exactArity(4),
        "NEW_TIME" to exactArity(3),
        "NVL2" to exactArity(3),
        "TRANSLATE" to exactArity(3),
        "COALESCE" to arityRange(2, Int.MAX_VALUE),
        "CONCAT" to arityRange(2, Int.MAX_VALUE),
        "CONVERT" to arityRange(2, 3),
        "COSINE_DISTANCE" to exactArity(2),
        "CORR" to exactArity(2),
        "COVAR_POP" to exactArity(2),
        "COVAR_SAMP" to exactArity(2),
        "CURRENT_TIMESTAMP" to exactArity(1),
        "DECODE" to arityRange(3, 255),
        "GREATEST" to arityRange(1, Int.MAX_VALUE),
        "INSTR" to arityRange(2, 4),
        "LEAST" to arityRange(1, Int.MAX_VALUE),
        "LOCALTIMESTAMP" to exactArity(1),
        "LPAD" to arityRange(2, 3),
        "NVL" to exactArity(2),
        "ROUND" to arityRange(1, 2),
        "RPAD" to arityRange(2, 3),
        "REGEXP_COUNT" to arityRange(2, 4),
        "REGEXP_INSTR" to arityRange(2, 7),
        "REGEXP_LIKE" to arityRange(2, 3),
        "REGEXP_REPLACE" to arityRange(2, 6),
        "REGEXP_SUBSTR" to arityRange(2, 6),
        "KURTOSIS_POP" to exactArity(1),
        "KURTOSIS_SAMP" to exactArity(1),
        "SKEWNESS_POP" to exactArity(1),
        "SKEWNESS_SAMP" to exactArity(1),
        "STDDEV" to exactArity(1),
        "STDDEV_POP" to exactArity(1),
        "STDDEV_SAMP" to exactArity(1),
        "SUBSTR" to arityRange(2, 3),
        "TO_BINARY_DOUBLE" to arityRange(1, 3),
        "TO_BINARY_FLOAT" to arityRange(1, 3),
        "TO_CHAR" to arityRange(1, 3),
        "TO_DATE" to arityRange(1, 3),
        "TO_NUMBER" to arityRange(1, 3),
        "TO_TIMESTAMP" to arityRange(1, 3),
        "TO_TIMESTAMP_TZ" to arityRange(1, 3),
        "TO_VECTOR" to arityRange(1, 3),
        "TRUNC" to arityRange(1, 2),
        "USERENV" to exactArity(1),
        "VARIANCE" to exactArity(1),
        "VAR_POP" to exactArity(1),
        "VAR_SAMP" to exactArity(1),
        "VECTOR_DIMENSION_COUNT" to exactArity(1),
        "VECTOR_DIMENSION_FORMAT" to exactArity(1),
        "VECTOR_DISTANCE" to arityRange(2, 3),
        "VECTOR_DIMS" to exactArity(1),
        "VECTOR_NORM" to exactArity(1),
        "XMLISVALID" to arityRange(1, 2),
    )

private val oracleNoParenthesesExpressions =
    setOf(
        "CURRENT_DATE",
        "DBTIMEZONE",
        "ORA_INVOKING_USER",
        "ORA_INVOKING_USERID",
        "SESSIONTIMEZONE",
        "SYSDATE",
        "SYSTIMESTAMP",
        "UID",
        "USER",
    )

private val oracleFunctionPattern =
    Regex("""(?i)\b(${(oracleFunctionArities.keys + oracleNoParenthesesExpressions).joinToString("|") { Regex.escape(it) }})\s*\(""")

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
