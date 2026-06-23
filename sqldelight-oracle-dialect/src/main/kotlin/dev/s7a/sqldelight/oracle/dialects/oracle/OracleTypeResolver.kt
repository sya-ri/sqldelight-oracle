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
        if (expr.text.hasOracleVectorDistanceShorthand()) {
            IntermediateType(BINARY_DOUBLE)
        } else {
            parentResolver.resolvedType(expr)
        }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
        val functionName = functionExpr.functionName.text
        return argumentDependentFunctionType(functionName, functionExpr)
            ?: returningClauseFunctionType(functionName, functionExpr)
            ?: OracleType.fromFunctionName(functionName)?.let { type -> IntermediateType(type) }
            ?: parentResolver.functionType(functionExpr)
    }

    private fun returningClauseFunctionType(
        functionName: String,
        functionExpr: SqlFunctionExpr,
    ): IntermediateType? =
        when (functionName.trim().uppercase()) {
            "JSON_VALUE",
            "JSON_QUERY",
            "JSON_SERIALIZE",
            "XMLSERIALIZE",
            -> {
                functionExpr.text.oracleReturningTypeName()?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }
            }

            "XMLCAST" -> {
                functionExpr.text.oracleCastTypeName()?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }
            }

            else -> {
                null
            }
        }

    private fun argumentDependentFunctionType(
        functionName: String,
        functionExpr: SqlFunctionExpr,
    ): IntermediateType? =
        when (functionName.trim().lowercase()) {
            "abs", "ceil", "floor" -> {
                functionExpr.exprList.singleOrNull()?.let { expression ->
                    parentResolver.resolvedType(expression).takeIf { type -> type.dialectType in NUMERIC_TYPE_ORDER }
                }
            }

            "mod", "remainder" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.size == 2 }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(exprList, *NUMERIC_TYPE_ORDER)
                }
            }

            "power" -> {
                functionExpr.exprList
                    .takeIf { exprList -> exprList.size == 2 }
                    ?.map { expression ->
                        parentResolver.resolvedType(expression).dialectType
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
                when (functionExpr.exprList.size) {
                    1 -> {
                        parentResolver
                            .resolvedType(functionExpr.exprList.single())
                            .takeIf { type -> type.dialectType in NUMERIC_TYPE_ORDER }
                    }

                    2 -> {
                        functionExpr.exprList
                            .map { expression ->
                                parentResolver.resolvedType(expression).dialectType
                            }.let { argumentTypes ->
                                if (argumentTypes.all { type -> type in NUMERIC_TYPE_ORDER }) {
                                    IntermediateType(DECIMAL_NUMBER)
                                } else {
                                    null
                                }
                            }
                    }

                    else -> {
                        null
                    }
                }
            }

            "coalesce", "nvl" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.isNotEmpty() }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(
                        exprList,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
                }
            }

            "nullif" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.size == 2 }?.firstOrNull()?.let { expression ->
                    parentResolver.resolvedType(expression)
                }
            }

            "nvl2" -> {
                functionExpr.exprList.drop(1).takeIf { exprList -> exprList.size == 2 }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(
                        exprList,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
                }
            }

            "greatest", "least" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.isNotEmpty() }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(exprList, *COMPARABLE_TYPE_ORDER)
                }
            }

            "decode" -> {
                functionExpr.exprList.drop(1).takeIf { exprList -> exprList.size >= 2 }?.let { exprList ->
                    val resultExpressions =
                        exprList
                            .withIndex()
                            .filter { (index) -> index % 2 == 1 || (index == exprList.lastIndex && exprList.size % 2 == 1) }
                            .map { (_, expression) -> expression }
                    parentResolver.encapsulatingTypePreferringKotlin(resultExpressions, *COMPARABLE_TYPE_ORDER)
                }
            }

            "nanvl" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.size == 2 }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(exprList, *NUMERIC_TYPE_ORDER)
                }
            }

            "max" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.isNotEmpty() }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(exprList, *MAX_TYPE_ORDER).asNullable()
                }
            }

            "min" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.isNotEmpty() }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(exprList, *MIN_TYPE_ORDER).asNullable()
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
                functionExpr.exprList.singleOrNull()?.let { expression ->
                    when (parentResolver.resolvedType(expression).dialectType) {
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
                functionExpr.exprList.singleOrNull()?.let { expression ->
                    when (parentResolver.resolvedType(expression).dialectType) {
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
                functionExpr.exprList.firstOrNull()?.let { expression -> parentResolver.resolvedType(expression).asNullable() }
            }

            "to_lob" -> {
                functionExpr.exprList.singleOrNull()?.let { expression ->
                    OracleType
                        .fromToLobArgumentType(parentResolver.resolvedType(expression).dialectType)
                        ?.let { type -> IntermediateType(type) }
                }
            }

            "userenv" -> {
                functionExpr.exprList.singleOrNull()?.let { expression ->
                    IntermediateType(OracleType.fromUserEnvParameter(expression.text))
                }
            }

            "extract" -> {
                when (functionExpr.exprList.size) {
                    1 -> {
                        when (functionExpr.text.oracleExtractDatetimeField()) {
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
