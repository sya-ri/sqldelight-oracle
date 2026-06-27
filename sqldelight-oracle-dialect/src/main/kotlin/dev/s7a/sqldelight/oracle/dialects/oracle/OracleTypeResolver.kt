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
            expr.text.hasOracleVectorDistanceShorthand() -> IntermediateType(BINARY_DOUBLE)
            else ->
                oracleExtensionFunctionType(expr)
                    ?: oracleExtensionPseudocolumnType(expr)
                    ?: oracleExtensionLiteralType(expr)
                    ?: parentResolver.resolvedType(expr)
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
        val arguments =
            extensionExpr.children
                .filterIsInstance<SqlExpr>()
                .filter { argument ->
                    argument.textRange.startOffset - extensionExpr.textRange.startOffset < invocationEnd
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
        val text = expr.oracleExtensionExpr()?.text?.trim()?.uppercase() ?: return null
        return when {
            text == "TRUE" || text == "FALSE" || text == "UNKNOWN" -> IntermediateType(BOOLEAN_TYPE)
            text.startsWith("DATE ") -> IntermediateType(DATE)
            text.startsWith("TIMESTAMP ") && text.contains(" TIME ZONE ") -> IntermediateType(TIMESTAMP_TIME_ZONE)
            text.startsWith("TIMESTAMP ") -> IntermediateType(TIMESTAMP)
            text.startsWith("INTERVAL ") -> IntermediateType(OracleType.TEXT)
            else -> null
        }
    }

    private fun SqlExpr.oracleExtensionExpr(): SqlExtensionExpr? =
        this as? SqlExtensionExpr
            ?: runCatching { children.filterIsInstance<SqlExtensionExpr>().singleOrNull() }.getOrNull()

    private fun oracleFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        argumentDependentFunctionType(functionName, functionText, exprList)
            ?: returningClauseFunctionType(functionName, functionText)
            ?: OracleType.fromFunctionName(functionName)?.let { type -> IntermediateType(type) }

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
            "abs", "ceil", "floor" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).takeIf { type -> type.dialectType in NUMERIC_TYPE_ORDER }
                }
            }

            "mod", "remainder" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
                }
            }

            "power" -> {
                exprList
                    .takeIf { args -> args.size == 2 }
                    ?.map { expression ->
                        resolvedType(expression).dialectType
                    }?.let { argumentTypes ->
                        when {
                            argumentTypes.any { type -> type == REAL || type == BINARY_FLOAT || type == BINARY_DOUBLE } -> {
                                IntermediateType(BINARY_DOUBLE)
                            }

                            argumentTypes.all { type -> type in NUMERIC_TYPE_ORDER } -> {
                                IntermediateType(DECIMAL_NUMBER)
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
                                resolvedType(expression).dialectType
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

            "nullif" -> {
                exprList.takeIf { args -> args.size == 2 }?.firstOrNull()?.let { expression ->
                    resolvedType(expression)
                }
            }

            "nvl2" -> {
                exprList.drop(1).takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
                }
            }

            "greatest", "least" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *COMPARABLE_TYPE_ORDER)
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
                }
            }

            "nanvl" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
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
            "median",
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
                when (exprList.size) {
                    1 -> {
                        when (functionText.oracleExtractDatetimeField()) {
                            "TIMEZONE_REGION", "TIMEZONE_ABBR" -> IntermediateType(OracleType.TEXT)
                            null -> null
                            else -> IntermediateType(DECIMAL_NUMBER)
                        }
                    }

                    2 -> {
                        IntermediateType(OracleType.TEXT)
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

        private fun String.hasOracleVectorDistanceShorthand(): Boolean =
            VECTOR_DISTANCE_SHORTHAND_OPERATORS.any { operator -> contains(operator) }

        private fun String.oracleFunctionName(): String? =
            Regex("""(?i)^\s*(?:[A-Z_][A-Z0-9_$#]*\s*\.\s*)*([A-Z_][A-Z0-9_$#]*)\s*\(""")
                .find(this)
                ?.groupValues
                ?.get(1)
                ?.uppercase()

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
                        '\'' -> inStringLiteral = true
                        '(' -> depth += 1
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
                in DATETIME_TYPE_ORDER -> IntermediateType(DATE)
                else -> null
            }

        private fun List<DialectType>.roundOrTruncTwoArgumentType(): IntermediateType? =
            when {
                all { type -> type in NUMERIC_TYPE_ORDER } -> IntermediateType(DECIMAL_NUMBER)
                first() in DATETIME_TYPE_ORDER && this[1] in TEXT_TYPE_ORDER -> IntermediateType(DATE)
                else -> null
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
