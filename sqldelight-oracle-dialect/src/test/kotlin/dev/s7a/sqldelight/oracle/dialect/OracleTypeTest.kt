package dev.s7a.sqldelight.oracle.dialect

import com.squareup.kotlinpoet.CodeBlock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OracleTypeTest :
    FunSpec({
        test("maps Oracle type names to SQLDelight dialect types exactly") {
            val mappings =
                listOf(
                    "BOOLEAN" to OracleType.BOOLEAN_TYPE,
                    "NUMBER" to OracleType.DECIMAL_NUMBER,
                    "NUMBER(9)" to OracleType.INTEGER_NUMBER,
                    "NUMBER(10)" to OracleType.LONG_NUMBER,
                    "NUMBER(18, 0)" to OracleType.LONG_NUMBER,
                    "NUMBER(19, 0)" to OracleType.DECIMAL_NUMBER,
                    "NUMBER(10, 2)" to OracleType.DECIMAL_NUMBER,
                    "NUMERIC(8, 0)" to OracleType.INTEGER_NUMBER,
                    "DECIMAL(20, 0)" to OracleType.DECIMAL_NUMBER,
                    "INTEGER" to OracleType.LONG_NUMBER,
                    "SMALLINT" to OracleType.INTEGER_NUMBER,
                    "FLOAT" to OracleType.BINARY_DOUBLE,
                    "BINARY_FLOAT" to OracleType.BINARY_FLOAT,
                    "BINARY_DOUBLE" to OracleType.BINARY_DOUBLE,
                    "CHAR(1 CHAR)" to OracleType.TEXT,
                    "NCHAR(1)" to OracleType.TEXT,
                    "VARCHAR2(100 CHAR)" to OracleType.TEXT,
                    "NVARCHAR2(100)" to OracleType.TEXT,
                    "CLOB" to OracleType.TEXT,
                    "NCLOB" to OracleType.TEXT,
                    "LONG" to OracleType.TEXT,
                    "ROWID" to OracleType.TEXT,
                    "UROWID" to OracleType.TEXT,
                    "JSON" to OracleType.TEXT,
                    "XMLTYPE" to OracleType.TEXT,
                    "SDO_GEOMETRY" to OracleType.TEXT,
                    "VECTOR" to OracleType.TEXT,
                    "OBJECT" to OracleType.TEXT,
                    "REF" to OracleType.TEXT,
                    "ANYDATA" to OracleType.TEXT,
                    "BLOB" to OracleType.BINARY,
                    "BFILE" to OracleType.BINARY,
                    "RAW(16)" to OracleType.BINARY,
                    "LONG RAW" to OracleType.BINARY,
                    "DATE" to OracleType.DATE,
                    "TIMESTAMP" to OracleType.TIMESTAMP,
                    "TIMESTAMP(6)" to OracleType.TIMESTAMP,
                    "TIMESTAMP WITH TIME ZONE" to OracleType.TIMESTAMP_TIME_ZONE,
                    "TIMESTAMP(6) WITH LOCAL TIME ZONE" to OracleType.TIMESTAMP_TIME_ZONE,
                    "INTERVAL YEAR TO MONTH" to OracleType.TEXT,
                    "INTERVAL DAY TO SECOND" to OracleType.TEXT,
                )

            mappings.map { (typeName, expectedType) -> typeName to OracleType.fromSqlTypeName(typeName) } shouldBe mappings
        }

        test("reports unsupported Oracle type names exactly") {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    OracleType.fromSqlTypeName("UNSUPPORTED_TYPE")
                }

            exception.message shouldBe "Unknown Kotlin type for Oracle SQL type UNSUPPORTED_TYPE"
        }

        test("generates binders and cursor getters exactly") {
            val generated =
                OracleType.entries.map { type ->
                    type.name to
                        listOf(
                            type.prepareStatementBinder(CodeBlock.of("1"), CodeBlock.of("value")).toString(),
                            type.cursorGetter(0, "cursor").toString(),
                        )
                }

            generated shouldBe
                listOf(
                    "BOOLEAN_TYPE" to listOf("bindBoolean(1, value)\n", "cursor.getBoolean(0)"),
                    "INTEGER_NUMBER" to listOf("bindInt(1, value)\n", "cursor.getInt(0)"),
                    "LONG_NUMBER" to listOf("bindLong(1, value)\n", "cursor.getLong(0)"),
                    "DECIMAL_NUMBER" to listOf("bindBigDecimal(1, value)\n", "cursor.getBigDecimal(0)"),
                    "BINARY_FLOAT" to listOf("bindFloat(1, value)\n", "cursor.getFloat(0)"),
                    "BINARY_DOUBLE" to listOf("bindDouble(1, value)\n", "cursor.getDouble(0)"),
                    "TEXT" to listOf("bindString(1, value)\n", "cursor.getString(0)"),
                    "BINARY" to listOf("bindBytes(1, value)\n", "cursor.getBytes(0)"),
                    "DATE" to listOf("bindObject(1, value)\n", "cursor.getObject<java.time.LocalDateTime>(0)"),
                    "TIMESTAMP" to listOf("bindObject(1, value)\n", "cursor.getObject<java.time.LocalDateTime>(0)"),
                    "TIMESTAMP_TIME_ZONE" to listOf("bindObject(1, value)\n", "cursor.getObject<java.time.OffsetDateTime>(0)"),
                )
        }
    })
