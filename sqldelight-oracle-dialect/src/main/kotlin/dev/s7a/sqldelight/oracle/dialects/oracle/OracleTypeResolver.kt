package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY_DOUBLE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY_FLOAT
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BOOLEAN_TYPE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.DATE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.DECIMAL_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.INTEGER_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.LONG_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.TIMESTAMP
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.TIMESTAMP_TIME_ZONE

public class OracleTypeResolver(
    private val parentResolver: TypeResolver,
) : TypeResolver by parentResolver {
    override fun definitionType(typeName: SqlTypeName): IntermediateType = IntermediateType(OracleType.fromSqlTypeName(typeName.text))

    override fun resolvedType(expr: SqlExpr): IntermediateType =
        when {
            expr.text.hasOracleVectorDistanceShorthand() -> {
                IntermediateType(BINARY_DOUBLE)
            }

            else -> {
                oracleExtensionFunctionType(expr)
                    ?: oracleExtensionPseudocolumnType(expr)
                    ?: oracleExtensionLiteralType(expr)
                    ?: oracleConcatenationOperatorType(expr)
                    ?: oracleDatetimeOperatorType(expr)
                    ?: oracleNumericOperatorType(expr)
                    ?: parentResolver.resolvedType(expr)
            }
        }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
        val functionName = functionExpr.functionName.text
        return oracleFunctionType(functionName, functionExpr.text, functionExpr.exprList)
            ?: parentResolver.functionType(functionExpr)
    }

    private fun oracleExtensionFunctionType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        val functionName = extensionExpr.text.oracleFunctionName() ?: return null
        val invocationEnd = extensionExpr.text.oracleFirstFunctionInvocationEnd()
        val childExpressions = PsiTreeUtil.findChildrenOfType(extensionExpr, SqlExpr::class.java).toList()
        val invocationArguments =
            childExpressions.filter { argument ->
                argument.textRange.startOffset - extensionExpr.textRange.startOffset < invocationEnd
            }
        val arguments =
            if (functionName.isOracleWithinGroupOrderedValueFunction()) {
                invocationArguments + childExpressions.oracleWithinGroupOrderingExpressions(extensionExpr)
            } else {
                invocationArguments
            }
        return oracleFunctionType(functionName, extensionExpr.text, arguments)
    }

    private fun oracleExtensionPseudocolumnType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        return when (extensionExpr.text.oracleTerminalIdentifier()) {
            "CONNECT_BY_ISCYCLE",
            "CONNECT_BY_ISLEAF",
            "CURRVAL",
            "LEVEL",
            "NEXTVAL",
            "OBJECT_ID",
            "ORA_INVOKING_USERID",
            "ORA_ROWSCN",
            "ROWNUM",
            "UID",
            -> IntermediateType(LONG_NUMBER)

            "CURRENT_DATE",
            "SYSDATE",
            -> IntermediateType(DATE)

            "LOCALTIMESTAMP",
            -> IntermediateType(TIMESTAMP)

            "CURRENT_TIMESTAMP",
            "SYSTIMESTAMP",
            -> IntermediateType(TIMESTAMP_TIME_ZONE)

            "DBTIMEZONE",
            "ORA_INVOKING_USER",
            "ORA_SHARDSPACE_NAME",
            "ROWID",
            "SESSIONTIMEZONE",
            "USER",
            -> IntermediateType(OracleType.TEXT)

            else -> null
        }
    }

    private fun oracleExtensionLiteralType(expr: SqlExpr): IntermediateType? {
        val text =
            expr
                .oracleExtensionExpr()
                ?.text
                ?.trim()
                ?.uppercase()
                ?: return null
        return when {
            text == "TRUE" || text == "FALSE" || text == "UNKNOWN" -> IntermediateType(BOOLEAN_TYPE)
            text.startsWith("DATE ") -> IntermediateType(DATE)
            text.startsWith("TIMESTAMP ") && text.contains(" TIME ZONE ") -> IntermediateType(TIMESTAMP_TIME_ZONE)
            text.startsWith("TIMESTAMP ") -> IntermediateType(TIMESTAMP)
            text.startsWith("INTERVAL ") -> IntermediateType(OracleType.TEXT)
            else -> null
        }
    }

    private fun oracleNumericOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands) ?: return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        val operandDialectTypes = operandTypes.map { type -> type.dialectType }
        if (operandDialectTypes.any { type -> type !in NUMERIC_TYPE_ORDER }) return null

        val resultType =
            when {
                operator == "/" && operandDialectTypes.none { type -> type == REAL || type == BINARY_FLOAT || type == BINARY_DOUBLE } -> {
                    DECIMAL_NUMBER
                }

                else -> {
                    NUMERIC_TYPE_ORDER.last { type -> type in operandDialectTypes }
                }
            }
        return IntermediateType(resultType).nullableIf(operandTypes.any { type -> type.javaType.isNullable })
    }

    private fun oracleConcatenationOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands)
        if (operator != "||") return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        return IntermediateType(OracleType.TEXT).nullableIf(operandTypes.all { type -> type.javaType.isNullable })
    }

    private fun oracleDatetimeOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands) ?: return null
        if (operator != "+" && operator != "-") return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        val dialectTypes = operandTypes.map { type -> type.dialectType }
        val datetimeCount = dialectTypes.count { type -> type in DATETIME_TYPE_ORDER }
        if (datetimeCount == 0) return null
        val nullable = operandTypes.any { type -> type.javaType.isNullable }
        return when {
            // Oracle datetime subtraction: DATE - DATE yields a NUMBER of days, while any
            // subtraction involving TIMESTAMP/TIMESTAMP WITH TIME ZONE yields an INTERVAL DAY TO SECOND.
            operator == "-" && datetimeCount == 2 -> {
                if (dialectTypes.all { type -> type == DATE }) {
                    IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                } else {
                    IntermediateType(OracleType.TEXT).nullableIf(nullable)
                }
            }

            // Oracle interprets a number added to or subtracted from any datetime value as a number of
            // days, and the result is always a DATE (TIMESTAMP operands are converted to DATE first).
            datetimeCount == 1 && dialectTypes.any { type -> type in NUMERIC_TYPE_ORDER } -> {
                IntermediateType(DATE).nullableIf(nullable)
            }

            // Oracle datetime ± interval keeps the datetime operand's type
            // (DATE -> DATE, TIMESTAMP -> TIMESTAMP, TIMESTAMP WITH TIME ZONE -> TIMESTAMP WITH TIME ZONE).
            datetimeCount == 1 && operands.any { operand -> operand.isOracleIntervalOperand() } -> {
                val datetimeType = dialectTypes.first { type -> type in DATETIME_TYPE_ORDER }
                IntermediateType(datetimeType).nullableIf(nullable)
            }

            else -> {
                null
            }
        }
    }

    private fun SqlExpr.oracleExtensionExpr(): SqlExtensionExpr? =
        this as? SqlExtensionExpr
            ?: runCatching { children.filterIsInstance<SqlExtensionExpr>().singleOrNull() }.getOrNull()

    private fun SqlExpr.oracleBinaryOperatorBetween(operands: List<SqlExpr>): String? {
        val leftEnd = operands[0].textRange.endOffset - textRange.startOffset
        val rightStart = operands[1].textRange.startOffset - textRange.startOffset
        val between = text.substring(leftEnd, rightStart)
        return listOf("||", "*", "/", "+", "-").firstOrNull { operator -> operator in between }
    }

    private fun SqlExpr.isOracleIntervalOperand(): Boolean {
        val normalized = text.trim().uppercase()
        return normalized.startsWith("INTERVAL ") ||
            ORACLE_INTERVAL_FUNCTION_REGEX.containsMatchIn(normalized)
    }

    private fun oracleFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        argumentDependentFunctionType(functionName, functionText, exprList)
            ?: returningClauseFunctionType(functionName, functionText)
            ?: nullableAggregateFunctionType(functionName)
            ?: OracleType.fromFunctionName(functionName)?.let { type ->
                val propagatesNull = functionName.isOracleNullPropagatingFixedReturnFunction()
                val hasNullableInput = propagatesNull && exprList.any { expression -> resolvedType(expression).javaType.isNullable }
                IntermediateType(type)
                    .nullableIf(hasNullableInput)
            }

    private fun nullableAggregateFunctionType(functionName: String): IntermediateType? =
        OracleType
            .fromFunctionName(functionName)
            ?.takeIf { functionName.isOracleNullableAggregateFunction() }
            ?.let { type -> IntermediateType(type).asNullable() }

    private fun returningClauseFunctionType(
        functionName: String,
        functionText: String,
    ): IntermediateType? =
        when (functionName.trim().uppercase()) {
            "JSON_ARRAY",
            "JSON_ARRAYAGG",
            "JSON_OBJECT",
            "JSON_OBJECTAGG",
            "JSON_VALUE",
            "JSON_QUERY",
            "JSON_SERIALIZE",
            "JSON_MERGEPATCH",
            "JSON_TRANSFORM",
            "XMLSERIALIZE",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }
            }

            "CAST",
            "XMLCAST",
            -> {
                functionText.oracleCastTypeName()?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }
            }

            else -> {
                null
            }
        }

    private fun argumentDependentFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        when (functionName.trim().lowercase()) {
            "abs" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).takeIf { type -> type.dialectType in NUMERIC_TYPE_ORDER }
                }
            }

            "ceil", "floor" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).ceilOrFloorSingleArgumentType()
                }
            }

            "median", "approx_median" -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER, DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        in DATETIME_TYPE_ORDER -> {
                            resolvedType(expression).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "mod", "remainder" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
                        .nullableIf(args.any { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "power" -> {
                exprList
                    .takeIf { args -> args.size == 2 }
                    ?.map { expression ->
                        resolvedType(expression)
                    }?.let { argumentTypes ->
                        val nullable = argumentTypes.any { type -> type.javaType.isNullable }
                        when {
                            argumentTypes.any { type ->
                                type.dialectType == REAL ||
                                    type.dialectType == BINARY_FLOAT ||
                                    type.dialectType == BINARY_DOUBLE
                            } -> {
                                IntermediateType(BINARY_DOUBLE).nullableIf(nullable)
                            }

                            argumentTypes.all { type -> type.dialectType in NUMERIC_TYPE_ORDER } -> {
                                IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                            }

                            else -> {
                                null
                            }
                        }
                    }
            }

            "round", "trunc" -> {
                when (exprList.size) {
                    1 -> {
                        resolvedType(exprList.single()).roundOrTruncSingleArgumentType()
                    }

                    2 -> {
                        exprList
                            .map { expression ->
                                resolvedType(expression)
                            }.roundOrTruncTwoArgumentType()
                    }

                    else -> {
                        null
                    }
                }
            }

            "coalesce", "nvl" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
                }
            }

            "concat" -> {
                exprList.takeIf { args -> args.size >= 2 }?.let { args ->
                    IntermediateType(OracleType.TEXT)
                        .nullableIf(args.all { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "nullif" -> {
                exprList.takeIf { args -> args.size == 2 }?.firstOrNull()?.let { expression ->
                    resolvedType(expression).asNullable()
                }
            }

            "nvl2" -> {
                exprList.drop(1).takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.any { isNullable -> isNullable } },
                    )
                }
            }

            "greatest", "least" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.any { isNullable -> isNullable } },
                    )
                }
            }

            "decode" -> {
                exprList.drop(1).takeIf { args -> args.size >= 2 }?.let { args ->
                    val resultExpressions =
                        args
                            .withIndex()
                            .filter { (index) -> index % 2 == 1 || (index == args.lastIndex && args.size % 2 == 1) }
                            .map { (_, expression) -> expression }
                    encapsulatingTypePreferringKotlin(resultExpressions, *COMPARABLE_TYPE_ORDER)
                        .nullableIf(
                            args.size % 2 == 0 ||
                                resultExpressions.any { expression -> resolvedType(expression).javaType.isNullable },
                        )
                }
            }

            "nanvl" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
                        .nullableIf(args.any { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "max" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *MAX_TYPE_ORDER).asNullable()
                }
            }

            "min" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *MIN_TYPE_ORDER).asNullable()
                }
            }

            "avg",
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
            -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER, DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "sum" -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER -> {
                            IntermediateType(LONG_NUMBER).asNullable()
                        }

                        DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "percentile_cont", "percentile_disc" -> {
                exprList.getOrNull(1)?.let { expression ->
                    resolvedType(expression).asNullable()
                }
            }

            "approx_percentile" -> {
                when {
                    functionText.hasOracleApproxPercentileDiagnosticReturn() -> {
                        IntermediateType(DECIMAL_NUMBER).asNullable()
                    }

                    else -> {
                        exprList.lastOrNull()?.let { expression -> resolvedType(expression).asNullable() }
                    }
                }
            }

            "any_value", "stats_mode" -> {
                exprList.singleOrNull()?.let { expression -> resolvedType(expression).asNullable() }
            }

            "first_value", "lag", "last_value", "lead", "nth_value" -> {
                exprList.firstOrNull()?.let { expression -> resolvedType(expression).asNullable() }
            }

            "to_lob" -> {
                exprList.singleOrNull()?.let { expression ->
                    OracleType
                        .fromToLobArgumentType(resolvedType(expression).dialectType)
                        ?.let { type -> IntermediateType(type) }
                }
            }

            "userenv" -> {
                exprList.singleOrNull()?.let { expression ->
                    IntermediateType(OracleType.fromUserEnvParameter(expression.text))
                }
            }

            "extract" -> {
                val nullable =
                    exprList.any { expression ->
                        runCatching { resolvedType(expression).javaType.isNullable }.getOrDefault(false)
                    }
                when (exprList.size) {
                    1 -> {
                        when (functionText.oracleExtractDatetimeField()) {
                            "TIMEZONE_REGION", "TIMEZONE_ABBR" -> IntermediateType(OracleType.TEXT).nullableIf(nullable)
                            null -> null
                            else -> IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                        }
                    }

                    2 -> {
                        IntermediateType(OracleType.TEXT).nullableIf(nullable)
                    }

                    else -> {
                        null
                    }
                }
            }

            else -> {
                null
            }
        }

    private companion object {
        private val VECTOR_DISTANCE_SHORTHAND_OPERATORS = listOf("<->", "<=>", "<#>")

        private val ORACLE_INTERVAL_FUNCTION_REGEX = Regex("""\b(?:NUMTODSINTERVAL|NUMTOYMINTERVAL|TO_DSINTERVAL|TO_YMINTERVAL)\s*\(""")

        private fun String.hasOracleVectorDistanceShorthand(): Boolean =
            VECTOR_DISTANCE_SHORTHAND_OPERATORS.any { operator -> contains(operator) }

        private fun String.isOracleNullPropagatingFixedReturnFunction(): Boolean =
            trim().uppercase() in
                setOf(
                    "ACOS",
                    "ADD_MONTHS",
                    "ASCII",
                    "ASCIISTR",
                    "ASIN",
                    "ATAN",
                    "ATAN2",
                    "BITAND",
                    "CHR",
                    "COMPOSE",
                    "CONVERT",
                    "COS",
                    "COSH",
                    "DECOMPOSE",
                    "DUMP",
                    "EXP",
                    "FROM_TZ",
                    "HEXTORAW",
                    "INITCAP",
                    "INSTR",
                    "LAST_DAY",
                    "LENGTH",
                    "LN",
                    "LOG",
                    "LOWER",
                    "LPAD",
                    "LTRIM",
                    "MONTHS_BETWEEN",
                    "NEW_TIME",
                    "NEXT_DAY",
                    "NCHR",
                    "NLS_INITCAP",
                    "NLS_LOWER",
                    "NLS_UPPER",
                    "NUMTODSINTERVAL",
                    "NUMTOYMINTERVAL",
                    "REGEXP_COUNT",
                    "REGEXP_INSTR",
                    "REGEXP_REPLACE",
                    "REGEXP_SUBSTR",
                    "REPLACE",
                    "RAWTOHEX",
                    "RATIO_TO_REPORT",
                    "RPAD",
                    "RTRIM",
                    "SIGN",
                    "SIN",
                    "SINH",
                    "SOUNDEX",
                    "SQRT",
                    "SUBSTR",
                    "SYS_EXTRACT_UTC",
                    "TAN",
                    "TANH",
                    "TO_CLOB",
                    "TO_BINARY_DOUBLE",
                    "TO_BINARY_FLOAT",
                    "TO_CHAR",
                    "TO_DATE",
                    "TO_DSINTERVAL",
                    "TO_MULTI_BYTE",
                    "TO_NCHAR",
                    "TO_NUMBER",
                    "TO_SINGLE_BYTE",
                    "TO_TIMESTAMP",
                    "TO_TIMESTAMP_TZ",
                    "TO_YMINTERVAL",
                    "TRANSLATE",
                    "TRIM",
                    "UPPER",
                    "VSIZE",
                    "WIDTH_BUCKET",
                )

        private fun String.isOracleNullableAggregateFunction(): Boolean =
            trim().uppercase() in
                setOf(
                    "APPROX_MEDIAN",
                    "APPROX_PERCENTILE",
                    "APPROX_SUM",
                    "BIT_AND_AGG",
                    "BIT_OR_AGG",
                    "BIT_XOR_AGG",
                    "BOOLEAN_AND_AGG",
                    "BOOLEAN_OR_AGG",
                    "CORR",
                    "COVAR_POP",
                    "COVAR_SAMP",
                    "KURTOSIS_POP",
                    "KURTOSIS_SAMP",
                    "LISTAGG",
                    "PERCENTILE_CONT",
                    "PERCENTILE_DISC",
                    "REGR_AVGX",
                    "REGR_AVGY",
                    "REGR_INTERCEPT",
                    "REGR_R2",
                    "REGR_SLOPE",
                    "REGR_SXX",
                    "REGR_SXY",
                    "REGR_SYY",
                    "SKEWNESS_POP",
                    "SKEWNESS_SAMP",
                )

        private fun String.oracleFunctionName(): String? =
            Regex("""(?i)^\s*(?:[A-Z_][A-Z0-9_$#]*\s*\.\s*)*([A-Z_][A-Z0-9_$#]*)\s*\(""")
                .find(this)
                ?.groupValues
                ?.get(1)
                ?.uppercase()

        private fun String.isOracleWithinGroupOrderedValueFunction(): Boolean =
            trim().uppercase() in setOf("APPROX_PERCENTILE", "PERCENTILE_CONT", "PERCENTILE_DISC")

        private fun String.hasOracleApproxPercentileDiagnosticReturn(): Boolean {
            val normalized = uppercase()
            return "DETERMINISTIC" in normalized && ("'ERROR_RATE'" in normalized || "'CONFIDENCE'" in normalized)
        }

        private fun List<SqlExpr>.oracleWithinGroupOrderingExpressions(extensionExpr: SqlExtensionExpr): List<SqlExpr> {
            val orderByStart = extensionExpr.text.oracleWithinGroupOrderByExpressionStart() ?: return emptyList()
            val withinGroupEnd = extensionExpr.text.oracleWithinGroupClauseEnd(orderByStart) ?: return emptyList()
            val extensionStart = extensionExpr.textRange.startOffset
            return filter { expression ->
                val relativeStart = expression.textRange.startOffset - extensionStart
                relativeStart in orderByStart..<withinGroupEnd
            }
        }

        private fun String.oracleWithinGroupOrderByExpressionStart(): Int? =
            Regex("""(?i)\bWITHIN\s+GROUP\s*\(\s*ORDER\s+BY\s+""")
                .find(this)
                ?.range
                ?.last
                ?.plus(1)

        private fun String.oracleWithinGroupClauseEnd(orderByStart: Int): Int? {
            val openParen = lastIndexOf('(', startIndex = orderByStart).takeIf { index -> index >= 0 } ?: return null
            var depth = 0
            var inStringLiteral = false
            var index = openParen
            while (index < length) {
                val char = this[index]
                if (inStringLiteral) {
                    if (char == '\'' && getOrNull(index + 1) == '\'') {
                        index += 2
                        continue
                    }
                    if (char == '\'') {
                        inStringLiteral = false
                    }
                } else {
                    when (char) {
                        '\'' -> {
                            inStringLiteral = true
                        }

                        '(' -> {
                            depth += 1
                        }

                        ')' -> {
                            depth -= 1
                            if (depth == 0) return index
                        }
                    }
                }
                index += 1
            }
            return null
        }

        private fun String.oracleTerminalIdentifier(): String =
            trim()
                .substringAfterLast(".")
                .trim()
                .uppercase()

        private fun String.oracleFirstFunctionInvocationEnd(): Int {
            val start = indexOf('(').takeIf { index -> index >= 0 } ?: return length
            var depth = 0
            var inStringLiteral = false
            var index = start
            while (index < length) {
                val char = this[index]
                if (inStringLiteral) {
                    if (char == '\'' && getOrNull(index + 1) == '\'') {
                        index += 2
                        continue
                    }
                    if (char == '\'') {
                        inStringLiteral = false
                    }
                } else {
                    when (char) {
                        '\'' -> {
                            inStringLiteral = true
                        }

                        '(' -> {
                            depth += 1
                        }

                        ')' -> {
                            depth -= 1
                            if (depth == 0) return index + 1
                        }
                    }
                }
                index += 1
            }
            return length
        }

        private fun String.oracleReturningTypeName(): String? = oracleTypeNameAfterKeyword("RETURNING")

        private fun String.oracleCastTypeName(): String? = oracleTypeNameAfterKeyword("AS")

        private fun String.oracleExtractDatetimeField(): String? =
            Regex("""(?i)^\s*EXTRACT\s*\(\s*([A-Z_]+)\s+FROM\b""")
                .find(this)
                ?.groupValues
                ?.get(1)
                ?.uppercase()

        private fun String.oracleTypeNameAfterKeyword(keyword: String): String? {
            val match = oracleReturningTypeRegex(keyword).find(this)
            return match?.groupValues?.get(1)?.trim()
        }

        private fun oracleReturningTypeRegex(keyword: String): Regex =
            Regex(
                """(?i)\b${Regex.escape(keyword)}\s+""" +
                    """(DOUBLE\s+PRECISION|TIMESTAMP(?:\s*\([^)]*\))?(?:\s+WITH(?:\s+LOCAL)?\s+TIME\s+ZONE)?|""" +
                    """INTERVAL\s+(?:YEAR|DAY)\s+TO\s+(?:MONTH|SECOND)|""" +
                    """NATIONAL\s+CHARACTER\s+VARYING\s*\([^)]*\)|NATIONAL\s+CHAR\s+VARYING\s*\([^)]*\)|""" +
                    """CHARACTER\s+VARYING\s*\([^)]*\)|VARYING\s+ARRAY\s*(?:\([^)]*\))?|""" +
                    """[A-Z_]+(?:\s*\([^)]*\))?)""",
            )

        private fun IntermediateType.roundOrTruncSingleArgumentType(): IntermediateType? =
            when (dialectType) {
                in NUMERIC_TYPE_ORDER -> this
                in DATETIME_TYPE_ORDER -> IntermediateType(DATE).nullableIf(javaType.isNullable)
                else -> null
            }

        private fun IntermediateType.ceilOrFloorSingleArgumentType(): IntermediateType? =
            when (dialectType) {
                in NUMERIC_TYPE_ORDER -> this
                in DATETIME_TYPE_ORDER -> IntermediateType(DATE).nullableIf(javaType.isNullable)
                else -> null
            }

        private fun List<IntermediateType>.roundOrTruncTwoArgumentType(): IntermediateType? =
            when {
                all { type -> type.dialectType in NUMERIC_TYPE_ORDER } -> {
                    IntermediateType(DECIMAL_NUMBER).nullableIf(any { type -> type.javaType.isNullable })
                }

                first().dialectType in DATETIME_TYPE_ORDER && this[1].dialectType in TEXT_TYPE_ORDER -> {
                    IntermediateType(DATE).nullableIf(any { type -> type.javaType.isNullable })
                }

                else -> {
                    null
                }
            }

        private val COMPARABLE_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                BOOLEAN,
                BOOLEAN_TYPE,
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TEXT,
                BLOB,
                BINARY,
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private val MAX_TYPE_ORDER = COMPARABLE_TYPE_ORDER

        private val NUMERIC_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
            )

        private val DATETIME_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private val TEXT_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                TEXT,
                OracleType.TEXT,
            )

        private val MIN_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                BLOB,
                BINARY,
                TEXT,
                BOOLEAN,
                BOOLEAN_TYPE,
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TIMESTAMP_TIME_ZONE,
                TIMESTAMP,
                DATE,
            )
    }
}
