package dev.s7a.sqldelight.oracle.dialect

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

                "SMALLINT" -> {
                    INTEGER_NUMBER
                }

                "FLOAT" -> {
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

        public fun fromFunctionName(functionName: String): OracleType? =
            when (functionName.trim().uppercase()) {
                "CURRENT_DATE", "SYSDATE" -> DATE
                "CURRENT_TIMESTAMP", "LOCALTIMESTAMP" -> TIMESTAMP
                "SYSTIMESTAMP" -> TIMESTAMP_TIME_ZONE
                "TO_CHAR", "RAWTOHEX", "JSON_QUERY", "JSON_SERIALIZE", "XMLSERIALIZE" -> TEXT
                "TO_DATE" -> DATE
                "TO_TIMESTAMP" -> TIMESTAMP
                "TO_TIMESTAMP_TZ" -> TIMESTAMP_TIME_ZONE
                "TO_NUMBER" -> DECIMAL_NUMBER
                "HEXTORAW" -> BINARY
                "JSON_OBJECT", "JSON_ARRAY", "JSON_OBJECTAGG", "JSON_ARRAYAGG" -> TEXT
                "XMLTYPE", "XMLELEMENT", "XMLFOREST", "XMLAGG" -> TEXT
                "LENGTH", "INSTR", "ROW_NUMBER", "RANK", "DENSE_RANK", "NTILE" -> LONG_NUMBER
                "COUNT" -> LONG_NUMBER
                "AVG", "MEDIAN", "STDDEV", "VARIANCE" -> DECIMAL_NUMBER
                "SIN", "COS", "TAN", "ASIN", "ACOS", "ATAN", "ATAN2", "EXP", "LN", "LOG", "SQRT" -> BINARY_DOUBLE
                else -> null
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
                "VECTOR",
                "OBJECT",
                "REF",
                "ANYDATA",
            )

        private val binaryBaseNames: Set<String> =
            setOf(
                "BLOB",
                "BFILE",
                "RAW",
                "LONG RAW",
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
        startsWith("TIMESTAMP") -> "TIMESTAMP"
        startsWith("INTERVAL") -> "INTERVAL"
        else -> substringBefore("(").substringBefore(" ").trim()
    }
