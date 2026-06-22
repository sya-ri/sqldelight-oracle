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
                "ABS", "CEIL", "FLOOR" -> {
                    argumentTypes.singleOrNull()?.takeIf { type -> type in numericTypes }
                }

                "MOD", "REMAINDER" -> {
                    argumentTypes.highestType(*numericTypes.toTypedArray())
                }

                "POWER" -> {
                    when {
                        argumentTypes.any { type -> type == BINARY_FLOAT || type == BINARY_DOUBLE } -> BINARY_DOUBLE
                        argumentTypes.all { type -> type in numericTypes } -> DECIMAL_NUMBER
                        else -> null
                    }
                }

                "ROUND", "TRUNC" -> {
                    when (argumentTypes.size) {
                        1 -> argumentTypes.single().takeIf { type -> type in numericTypes }
                        2 -> DECIMAL_NUMBER.takeIf { argumentTypes.all { type -> type in numericTypes } }
                        else -> null
                    }
                }

                "COALESCE", "DECODE", "GREATEST", "LEAST", "NVL" -> {
                    argumentTypes.highestComparableType()
                }

                "NVL2" -> {
                    argumentTypes.drop(1).highestComparableType()
                }

                "NANVL" -> {
                    argumentTypes.highestType(*numericTypes.toTypedArray())
                }

                "MAX" -> {
                    argumentTypes.highestMaxType()
                }

                "MIN" -> {
                    argumentTypes.highestMinType()
                }

                else -> {
                    null
                }
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

        private val numericTypes: Set<OracleType> =
            setOf(
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                BINARY_FLOAT,
                BINARY_DOUBLE,
            )

        private val functionReturnTypes: Map<String, OracleType> =
            mapOf(
                "CURRENT_DATE" to DATE,
                "SYSDATE" to DATE,
                "CURRENT_TIMESTAMP" to TIMESTAMP,
                "LOCALTIMESTAMP" to TIMESTAMP,
                "SYSTIMESTAMP" to TIMESTAMP_TIME_ZONE,
                "ADD_MONTHS" to DATE,
                "LAST_DAY" to DATE,
                "NEXT_DAY" to DATE,
                "CHR" to TEXT,
                "CONCAT" to TEXT,
                "INITCAP" to TEXT,
                "LOWER" to TEXT,
                "LPAD" to TEXT,
                "LTRIM" to TEXT,
                "NCHR" to TEXT,
                "NLS_INITCAP" to TEXT,
                "NLS_LOWER" to TEXT,
                "NLS_UPPER" to TEXT,
                "NLS_CHARSET_NAME" to TEXT,
                "COLLATION" to TEXT,
                "NLS_COLLATION_NAME" to TEXT,
                "EMPTY_CLOB" to TEXT,
                "REGEXP_REPLACE" to TEXT,
                "REGEXP_SUBSTR" to TEXT,
                "RPAD" to TEXT,
                "RTRIM" to TEXT,
                "SOUNDEX" to TEXT,
                "SUBSTR" to TEXT,
                "TRANSLATE" to TEXT,
                "TRIM" to TEXT,
                "UPPER" to TEXT,
                "TO_CHAR" to TEXT,
                "LISTAGG" to TEXT,
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
                "MONTHS_BETWEEN" to DECIMAL_NUMBER,
                "NLS_CHARSET_DECL_LEN" to DECIMAL_NUMBER,
                "NLS_CHARSET_ID" to DECIMAL_NUMBER,
                "NLS_COLLATION_ID" to DECIMAL_NUMBER,
                "BFILENAME" to BINARY,
                "EMPTY_BLOB" to BINARY,
                "HEXTORAW" to BINARY,
                "NLSSORT" to BINARY,
                "UUID" to BINARY,
                "UUID_TO_RAW" to BINARY,
                "JSON_ID" to BINARY,
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
                "PREDICTION_BOUNDS" to TEXT,
                "PREDICTION_DETAILS" to TEXT,
                "PREDICTION_SET" to TEXT,
                "CLUSTER_DETAILS" to TEXT,
                "CLUSTER_SET" to TEXT,
                "FEATURE_DETAILS" to TEXT,
                "FEATURE_SET" to TEXT,
                "ORA_DM_PARTITION_NAME" to TEXT,
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
                "ASCII" to LONG_NUMBER,
                "ROW_NUMBER" to LONG_NUMBER,
                "RANK" to LONG_NUMBER,
                "DENSE_RANK" to LONG_NUMBER,
                "NTILE" to LONG_NUMBER,
                "CUME_DIST" to BINARY_DOUBLE,
                "PERCENT_RANK" to BINARY_DOUBLE,
                "RATIO_TO_REPORT" to BINARY_DOUBLE,
                "PERCENTILE_CONT" to DECIMAL_NUMBER,
                "PERCENTILE_DISC" to DECIMAL_NUMBER,
                "CLUSTER_ID" to LONG_NUMBER,
                "FEATURE_ID" to LONG_NUMBER,
                "VECTOR_DIMENSION_COUNT" to LONG_NUMBER,
                "COUNT" to LONG_NUMBER,
                "APPROX_COUNT" to LONG_NUMBER,
                "APPROX_COUNT_DISTINCT" to LONG_NUMBER,
                "CHECKSUM" to LONG_NUMBER,
                "AVG" to DECIMAL_NUMBER,
                "MEDIAN" to DECIMAL_NUMBER,
                "APPROX_MEDIAN" to DECIMAL_NUMBER,
                "APPROX_PERCENTILE" to DECIMAL_NUMBER,
                "STDDEV" to DECIMAL_NUMBER,
                "STDDEV_POP" to DECIMAL_NUMBER,
                "STDDEV_SAMP" to DECIMAL_NUMBER,
                "VARIANCE" to DECIMAL_NUMBER,
                "VAR_POP" to DECIMAL_NUMBER,
                "VAR_SAMP" to DECIMAL_NUMBER,
                "CORR" to DECIMAL_NUMBER,
                "COVAR_POP" to DECIMAL_NUMBER,
                "COVAR_SAMP" to DECIMAL_NUMBER,
                "KURTOSIS_POP" to DECIMAL_NUMBER,
                "KURTOSIS_SAMP" to DECIMAL_NUMBER,
                "SKEWNESS_POP" to DECIMAL_NUMBER,
                "SKEWNESS_SAMP" to DECIMAL_NUMBER,
                "APPROX_SUM" to DECIMAL_NUMBER,
                "BIT_AND_AGG" to DECIMAL_NUMBER,
                "BIT_OR_AGG" to DECIMAL_NUMBER,
                "BIT_XOR_AGG" to DECIMAL_NUMBER,
                "BOOLEAN_AND_AGG" to BOOLEAN_TYPE,
                "BOOLEAN_OR_AGG" to BOOLEAN_TYPE,
                "VECTOR_DISTANCE" to BINARY_DOUBLE,
                "L1_DISTANCE" to BINARY_DOUBLE,
                "L2_DISTANCE" to BINARY_DOUBLE,
                "COSINE_DISTANCE" to BINARY_DOUBLE,
                "INNER_PRODUCT" to BINARY_DOUBLE,
                "HAMMING_DISTANCE" to BINARY_DOUBLE,
                "JACCARD_DISTANCE" to BINARY_DOUBLE,
                "VECTOR_NORM" to BINARY_DOUBLE,
                "PREDICTION_COST" to BINARY_DOUBLE,
                "PREDICTION_PROBABILITY" to BINARY_DOUBLE,
                "CLUSTER_DISTANCE" to BINARY_DOUBLE,
                "CLUSTER_PROBABILITY" to BINARY_DOUBLE,
                "FEATURE_COMPARE" to BINARY_DOUBLE,
                "FEATURE_VALUE" to BINARY_DOUBLE,
                "FUZZY_MATCH" to DECIMAL_NUMBER,
                "BITAND" to DECIMAL_NUMBER,
                "SIGN" to DECIMAL_NUMBER,
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
                "WIDTH_BUCKET" to LONG_NUMBER,
                "IS_UUID" to BOOLEAN_TYPE,
                "DOMAIN_CHECK" to BOOLEAN_TYPE,
                "DOMAIN_CHECK_TYPE" to BOOLEAN_TYPE,
                "REGEXP_LIKE" to BOOLEAN_TYPE,
                "RAW_TO_UUID" to TEXT,
                "DOMAIN_NAME" to TEXT,
                "PHONIC_ENCODE" to TEXT,
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
