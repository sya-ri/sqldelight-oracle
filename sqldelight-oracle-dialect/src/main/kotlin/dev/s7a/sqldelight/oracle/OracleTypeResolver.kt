package dev.s7a.sqldelight.oracle

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import dev.s7a.sqldelight.oracle.OracleType.BINARY
import dev.s7a.sqldelight.oracle.OracleType.BINARY_DOUBLE
import dev.s7a.sqldelight.oracle.OracleType.BINARY_FLOAT
import dev.s7a.sqldelight.oracle.OracleType.BOOLEAN_TYPE
import dev.s7a.sqldelight.oracle.OracleType.DATE
import dev.s7a.sqldelight.oracle.OracleType.DECIMAL_NUMBER
import dev.s7a.sqldelight.oracle.OracleType.INTEGER_NUMBER
import dev.s7a.sqldelight.oracle.OracleType.LONG_NUMBER
import dev.s7a.sqldelight.oracle.OracleType.TIMESTAMP
import dev.s7a.sqldelight.oracle.OracleType.TIMESTAMP_TIME_ZONE

public class OracleTypeResolver(
    private val parentResolver: TypeResolver,
) : TypeResolver by parentResolver {
    override fun definitionType(typeName: SqlTypeName): IntermediateType = IntermediateType(OracleType.fromSqlTypeName(typeName.text))

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
        val functionName = functionExpr.functionName.text
        return OracleType.fromFunctionName(functionName)?.let { type -> IntermediateType(type) }
            ?: argumentDependentFunctionType(functionName, functionExpr)
            ?: parentResolver.functionType(functionExpr)
    }

    private fun argumentDependentFunctionType(
        functionName: String,
        functionExpr: SqlFunctionExpr,
    ): IntermediateType? =
        when (functionName.trim().lowercase()) {
            "coalesce", "nvl" -> {
                functionExpr.exprList.takeIf { exprList -> exprList.isNotEmpty() }?.let { exprList ->
                    parentResolver.encapsulatingTypePreferringKotlin(
                        exprList,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
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

            else -> {
                null
            }
        }

    private companion object {
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
