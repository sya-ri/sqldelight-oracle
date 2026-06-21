package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

public enum class OracleType(
    override val javaType: TypeName,
) : DialectType {
    BOOLEAN_TYPE(BOOLEAN),
    INTEGER_NUMBER(INT),
    LONG_NUMBER(LONG),
    DECIMAL_NUMBER(ClassName("java.math", "BigDecimal")),
    BINARY_FLOAT(FLOAT),
    BINARY_DOUBLE(DOUBLE),
    TEXT(STRING),
    BINARY(ByteArray::class.asTypeName()),
    DATE(ClassName("java.time", "LocalDateTime")),
    TIMESTAMP(ClassName("java.time", "LocalDateTime")),
    TIMESTAMP_TIME_ZONE(ClassName("java.time", "OffsetDateTime")),
    ;

    override fun prepareStatementBinder(
        columnIndex: CodeBlock,
        value: CodeBlock,
    ): CodeBlock =
        when (this) {
            BOOLEAN_TYPE -> CodeBlock.of("bindBoolean(%L, %L)\n", columnIndex, value)
            INTEGER_NUMBER -> CodeBlock.of("bindInt(%L, %L)\n", columnIndex, value)
            LONG_NUMBER -> CodeBlock.of("bindLong(%L, %L)\n", columnIndex, value)
            DECIMAL_NUMBER -> CodeBlock.of("bindBigDecimal(%L, %L)\n", columnIndex, value)
            BINARY_FLOAT -> CodeBlock.of("bindFloat(%L, %L)\n", columnIndex, value)
            BINARY_DOUBLE -> CodeBlock.of("bindDouble(%L, %L)\n", columnIndex, value)
            TEXT -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
            BINARY -> CodeBlock.of("bindBytes(%L, %L)\n", columnIndex, value)
            DATE, TIMESTAMP, TIMESTAMP_TIME_ZONE -> CodeBlock.of("bindObject(%L, %L)\n", columnIndex, value)
        }

    override fun cursorGetter(
        columnIndex: Int,
        cursorName: String,
    ): CodeBlock =
        CodeBlock.of(
            when (this) {
                BOOLEAN_TYPE -> "$cursorName.getBoolean($columnIndex)"
                INTEGER_NUMBER -> "$cursorName.getInt($columnIndex)"
                LONG_NUMBER -> "$cursorName.getLong($columnIndex)"
                DECIMAL_NUMBER -> "$cursorName.getBigDecimal($columnIndex)"
                BINARY_FLOAT -> "$cursorName.getFloat($columnIndex)"
                BINARY_DOUBLE -> "$cursorName.getDouble($columnIndex)"
                TEXT -> "$cursorName.getString($columnIndex)"
                BINARY -> "$cursorName.getBytes($columnIndex)"
                DATE, TIMESTAMP, TIMESTAMP_TIME_ZONE -> "$cursorName.getObject<%T>($columnIndex)"
            },
            javaType,
        )

    public companion object {
        public fun fromSqlTypeName(typeName: String): OracleType {
            val normalized = typeName.normalizedOracleTypeName()
            val baseName = normalized.baseOracleTypeName()
            return when (baseName) {
                "BOOLEAN" -> {
                    BOOLEAN_TYPE
                }

                "NUMBER", "NUMERIC", "DECIMAL", "DEC" -> {
                    normalized.numberType()
                }

                "INTEGER", "INT" -> {
                    LONG_NUMBER
                }

                "SMALLINT", "BINARY_INTEGER", "PLS_INTEGER" -> {
                    INTEGER_NUMBER
                }

                "FLOAT", "REAL" -> {
                    BINARY_DOUBLE
                }

                "BINARY_FLOAT" -> {
                    BINARY_FLOAT
                }

                "BINARY_DOUBLE" -> {
                    BINARY_DOUBLE
                }

                in textBaseNames -> {
                    TEXT
                }

                in binaryBaseNames -> {
                    BINARY
                }

                "DATE" -> {
                    DATE
                }

                "TIMESTAMP" -> {
                    when {
                        normalized.contains("WITH TIME ZONE") -> TIMESTAMP_TIME_ZONE
                        normalized.contains("WITH LOCAL TIME ZONE") -> TIMESTAMP_TIME_ZONE
                        else -> TIMESTAMP
                    }
                }

                "INTERVAL" -> {
                    TEXT
                }

                else -> {
                    throw IllegalArgumentException("Unknown Kotlin type for Oracle SQL type $typeName")
                }
            }
        }

        public fun fromFunctionName(functionName: String): OracleType? = functionReturnTypes[functionName.trim().uppercase()]

        internal fun fromComparableFunctionTypes(
            functionName: String,
            argumentTypes: List<OracleType>,
        ): OracleType? =
            when (functionName.trim().uppercase()) {
                "COALESCE", "GREATEST", "LEAST", "NVL" -> argumentTypes.highestComparableType()
                "NVL2" -> argumentTypes.drop(1).highestComparableType()
                "MAX" -> argumentTypes.highestMaxType()
                "MIN" -> argumentTypes.highestMinType()
                else -> null
            }

        internal fun fromAggregateFunctionType(
            functionName: String,
            argumentType: OracleType,
        ): OracleType? =
            when (functionName.trim().uppercase()) {
                "SUM" -> {
                    when (argumentType) {
                        INTEGER_NUMBER, LONG_NUMBER -> {
                            LONG_NUMBER
                        }

                        DECIMAL_NUMBER -> {
                            DECIMAL_NUMBER
                        }

                        BINARY_FLOAT -> {
                            BINARY_FLOAT
                        }

                        BINARY_DOUBLE -> {
                            BINARY_DOUBLE
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

        private val textBaseNames: Set<String> =
            setOf(
                "CHAR",
                "NCHAR",
                "VARCHAR",
                "VARCHAR2",
                "NVARCHAR2",
                "CLOB",
                "NCLOB",
                "LONG",
                "ROWID",
                "UROWID",
                "JSON",
                "XMLTYPE",
                "SDO_GEOMETRY",
                "SDO_TOPO_GEOMETRY",
                "SDO_GEORASTER",
                "VECTOR",
                "OBJECT",
                "REF",
                "ANYTYPE",
                "ANYDATA",
                "ANYDATASET",
                "URITYPE",
                "DBURITYPE",
                "XDBURITYPE",
                "HTTPURITYPE",
                "NESTED TABLE",
                "VARRAY",
                "VARYING ARRAY",
            )

        private val binaryBaseNames: Set<String> =
            setOf(
                "BLOB",
                "BFILE",
                "RAW",
                "LONG RAW",
            )

        private val functionReturnTypes: Map<String, OracleType> =
            mapOf(
                "CURRENT_DATE" to DATE,
                "SYSDATE" to DATE,
                "CURRENT_TIMESTAMP" to TIMESTAMP,
                "LOCALTIMESTAMP" to TIMESTAMP,
                "SYSTIMESTAMP" to TIMESTAMP_TIME_ZONE,
                "TO_CHAR" to TEXT,
                "RAWTOHEX" to TEXT,
                "JSON_QUERY" to TEXT,
                "JSON_SERIALIZE" to TEXT,
                "JSON_VALUE" to TEXT,
                "JSON" to TEXT,
                "XMLSERIALIZE" to TEXT,
                "TO_DATE" to DATE,
                "TO_TIMESTAMP" to TIMESTAMP,
                "TO_TIMESTAMP_TZ" to TIMESTAMP_TIME_ZONE,
                "TO_NUMBER" to DECIMAL_NUMBER,
                "HEXTORAW" to BINARY,
                "UUID" to BINARY,
                "UUID_TO_RAW" to BINARY,
                "JSON_OBJECT" to TEXT,
                "JSON_ARRAY" to TEXT,
                "JSON_OBJECTAGG" to TEXT,
                "JSON_ARRAYAGG" to TEXT,
                "JSON_DATAGUIDE" to TEXT,
                "JSON_MERGEPATCH" to TEXT,
                "JSON_SCALAR" to TEXT,
                "JSON_TRANSFORM" to TEXT,
                "XMLTYPE" to TEXT,
                "XMLELEMENT" to TEXT,
                "XMLFOREST" to TEXT,
                "XMLAGG" to TEXT,
                "XMLQUERY" to TEXT,
                "VECTOR" to TEXT,
                "FROM_VECTOR" to TEXT,
                "TO_VECTOR" to TEXT,
                "VECTOR_CHUNKS" to TEXT,
                "VECTOR_DIMS" to TEXT,
                "VECTOR_DIMENSION_FORMAT" to TEXT,
                "VECTOR_EMBEDDING" to TEXT,
                "VECTOR_SERIALIZE" to TEXT,
                "LENGTH" to LONG_NUMBER,
                "INSTR" to LONG_NUMBER,
                "ROW_NUMBER" to LONG_NUMBER,
                "RANK" to LONG_NUMBER,
                "DENSE_RANK" to LONG_NUMBER,
                "NTILE" to LONG_NUMBER,
                "VECTOR_DIMENSION_COUNT" to LONG_NUMBER,
                "COUNT" to LONG_NUMBER,
                "AVG" to DECIMAL_NUMBER,
                "MEDIAN" to DECIMAL_NUMBER,
                "STDDEV" to DECIMAL_NUMBER,
                "VARIANCE" to DECIMAL_NUMBER,
                "VECTOR_DISTANCE" to BINARY_DOUBLE,
                "L1_DISTANCE" to BINARY_DOUBLE,
                "L2_DISTANCE" to BINARY_DOUBLE,
                "COSINE_DISTANCE" to BINARY_DOUBLE,
                "INNER_PRODUCT" to BINARY_DOUBLE,
                "HAMMING_DISTANCE" to BINARY_DOUBLE,
                "JACCARD_DISTANCE" to BINARY_DOUBLE,
                "VECTOR_NORM" to BINARY_DOUBLE,
                "SIN" to BINARY_DOUBLE,
                "COS" to BINARY_DOUBLE,
                "TAN" to BINARY_DOUBLE,
                "ASIN" to BINARY_DOUBLE,
                "ACOS" to BINARY_DOUBLE,
                "ATAN" to BINARY_DOUBLE,
                "ATAN2" to BINARY_DOUBLE,
                "EXP" to BINARY_DOUBLE,
                "LN" to BINARY_DOUBLE,
                "LOG" to BINARY_DOUBLE,
                "SQRT" to BINARY_DOUBLE,
                "IS_UUID" to BOOLEAN_TYPE,
                "DOMAIN_CHECK" to BOOLEAN_TYPE,
                "DOMAIN_CHECK_TYPE" to BOOLEAN_TYPE,
                "REGEXP_LIKE" to BOOLEAN_TYPE,
                "RAW_TO_UUID" to TEXT,
                "DOMAIN_NAME" to TEXT,
            )

        private fun String.numberType(): OracleType {
            val precisionScale = numberPrecisionScale() ?: return DECIMAL_NUMBER
            val precision = precisionScale.first ?: return DECIMAL_NUMBER
            val scale = precisionScale.second ?: 0
            return when {
                scale != 0 -> DECIMAL_NUMBER
                precision <= 9 -> INTEGER_NUMBER
                precision <= 18 -> LONG_NUMBER
                else -> DECIMAL_NUMBER
            }
        }

        private fun String.numberPrecisionScale(): Pair<Int?, Int?>? {
            val parameters =
                substringAfter("(", missingDelimiterValue = "")
                    .substringBefore(")", missingDelimiterValue = "")
                    .takeIf { value -> value.isNotBlank() }
                    ?: return null
            val parts = parameters.split(",").map { part -> part.trim() }
            return parts.getOrNull(0)?.toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
        }

        private fun List<OracleType>.highestComparableType(): OracleType? =
            highestType(
                BOOLEAN_TYPE,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TEXT,
                BINARY,
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private fun List<OracleType>.highestMaxType(): OracleType? =
            highestType(
                BOOLEAN_TYPE,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TEXT,
                BINARY,
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private fun List<OracleType>.highestMinType(): OracleType? =
            highestType(
                BINARY,
                TEXT,
                BOOLEAN_TYPE,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TIMESTAMP_TIME_ZONE,
                TIMESTAMP,
                DATE,
            )

        private fun List<OracleType>.highestType(vararg typeOrder: OracleType): OracleType? = typeOrder.lastOrNull { type -> type in this }
    }
}

private fun String.normalizedOracleTypeName(): String =
    trim()
        .replace(Regex("""\s+"""), " ")
        .uppercase()

private fun String.baseOracleTypeName(): String =
    when {
        startsWith("LONG RAW") -> "LONG RAW"
        startsWith("DOUBLE PRECISION") -> "BINARY_DOUBLE"
        startsWith("CHARACTER VARYING") -> "VARCHAR"
        startsWith("CHARACTER") -> "CHAR"
        startsWith("NATIONAL CHARACTER VARYING") -> "NVARCHAR2"
        startsWith("NATIONAL CHARACTER") -> "NCHAR"
        startsWith("NATIONAL CHAR VARYING") -> "NVARCHAR2"
        startsWith("NATIONAL CHAR") -> "NCHAR"
        startsWith("NESTED TABLE") -> "NESTED TABLE"
        startsWith("VARYING ARRAY") -> "VARYING ARRAY"
        startsWith("TIMESTAMP") -> "TIMESTAMP"
        startsWith("INTERVAL") -> "INTERVAL"
        else -> substringBefore("(").substringBefore(" ").trim()
    }
