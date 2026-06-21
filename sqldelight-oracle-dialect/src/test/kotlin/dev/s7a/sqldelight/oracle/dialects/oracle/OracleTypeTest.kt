package dev.s7a.sqldelight.oracle.dialects.oracle

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
                    "BINARY_INTEGER" to OracleType.INTEGER_NUMBER,
                    "PLS_INTEGER" to OracleType.INTEGER_NUMBER,
                    "FLOAT" to OracleType.BINARY_DOUBLE,
                    "REAL" to OracleType.BINARY_DOUBLE,
                    "DOUBLE PRECISION" to OracleType.BINARY_DOUBLE,
                    "BINARY_FLOAT" to OracleType.BINARY_FLOAT,
                    "BINARY_DOUBLE" to OracleType.BINARY_DOUBLE,
                    "CHAR(1 CHAR)" to OracleType.TEXT,
                    "CHARACTER(1)" to OracleType.TEXT,
                    "CHARACTER VARYING(100)" to OracleType.TEXT,
                    "NCHAR(1)" to OracleType.TEXT,
                    "NATIONAL CHAR(1)" to OracleType.TEXT,
                    "NATIONAL CHARACTER(1)" to OracleType.TEXT,
                    "NATIONAL CHAR VARYING(100)" to OracleType.TEXT,
                    "NATIONAL CHARACTER VARYING(100)" to OracleType.TEXT,
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
                    "SDO_TOPO_GEOMETRY" to OracleType.TEXT,
                    "SDO_GEORASTER" to OracleType.TEXT,
                    "VECTOR" to OracleType.TEXT,
                    "OBJECT" to OracleType.TEXT,
                    "REF" to OracleType.TEXT,
                    "ANYTYPE" to OracleType.TEXT,
                    "ANYDATA" to OracleType.TEXT,
                    "ANYDATASET" to OracleType.TEXT,
                    "URITYPE" to OracleType.TEXT,
                    "DBURITYPE" to OracleType.TEXT,
                    "XDBURITYPE" to OracleType.TEXT,
                    "HTTPURITYPE" to OracleType.TEXT,
                    "NESTED TABLE" to OracleType.TEXT,
                    "VARRAY" to OracleType.TEXT,
                    "VARRAY(10)" to OracleType.TEXT,
                    "VARYING ARRAY" to OracleType.TEXT,
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

        test("maps Oracle function names to SQLDelight dialect types exactly") {
            val mappings =
                listOf(
                    "CURRENT_DATE" to OracleType.DATE,
                    "SYSDATE" to OracleType.DATE,
                    "CURRENT_TIMESTAMP" to OracleType.TIMESTAMP,
                    "LOCALTIMESTAMP" to OracleType.TIMESTAMP,
                    "SYSTIMESTAMP" to OracleType.TIMESTAMP_TIME_ZONE,
                    "TO_CHAR" to OracleType.TEXT,
                    "RAWTOHEX" to OracleType.TEXT,
                    "JSON_QUERY" to OracleType.TEXT,
                    "JSON_SERIALIZE" to OracleType.TEXT,
                    "JSON_VALUE" to OracleType.TEXT,
                    "XMLSERIALIZE" to OracleType.TEXT,
                    "TO_DATE" to OracleType.DATE,
                    "TO_TIMESTAMP" to OracleType.TIMESTAMP,
                    "TO_TIMESTAMP_TZ" to OracleType.TIMESTAMP_TIME_ZONE,
                    "TO_NUMBER" to OracleType.DECIMAL_NUMBER,
                    "HEXTORAW" to OracleType.BINARY,
                    "UUID" to OracleType.BINARY,
                    "UUID_TO_RAW" to OracleType.BINARY,
                    "JSON_OBJECT" to OracleType.TEXT,
                    "JSON_ARRAY" to OracleType.TEXT,
                    "JSON_OBJECTAGG" to OracleType.TEXT,
                    "JSON_ARRAYAGG" to OracleType.TEXT,
                    "JSON_SCALAR" to OracleType.TEXT,
                    "JSON_TRANSFORM" to OracleType.TEXT,
                    "XMLTYPE" to OracleType.TEXT,
                    "XMLELEMENT" to OracleType.TEXT,
                    "XMLFOREST" to OracleType.TEXT,
                    "XMLAGG" to OracleType.TEXT,
                    "XMLQUERY" to OracleType.TEXT,
                    "VECTOR" to OracleType.TEXT,
                    "TO_VECTOR" to OracleType.TEXT,
                    "VECTOR_EMBEDDING" to OracleType.TEXT,
                    "VECTOR_SERIALIZE" to OracleType.TEXT,
                    "LENGTH" to OracleType.LONG_NUMBER,
                    "INSTR" to OracleType.LONG_NUMBER,
                    "ROW_NUMBER" to OracleType.LONG_NUMBER,
                    "RANK" to OracleType.LONG_NUMBER,
                    "DENSE_RANK" to OracleType.LONG_NUMBER,
                    "NTILE" to OracleType.LONG_NUMBER,
                    "COUNT" to OracleType.LONG_NUMBER,
                    "AVG" to OracleType.DECIMAL_NUMBER,
                    "MEDIAN" to OracleType.DECIMAL_NUMBER,
                    "STDDEV" to OracleType.DECIMAL_NUMBER,
                    "VARIANCE" to OracleType.DECIMAL_NUMBER,
                    "VECTOR_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "L1_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "L2_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "COSINE_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "INNER_PRODUCT" to OracleType.BINARY_DOUBLE,
                    "HAMMING_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "JACCARD_DISTANCE" to OracleType.BINARY_DOUBLE,
                    "SIN" to OracleType.BINARY_DOUBLE,
                    "COS" to OracleType.BINARY_DOUBLE,
                    "TAN" to OracleType.BINARY_DOUBLE,
                    "ASIN" to OracleType.BINARY_DOUBLE,
                    "ACOS" to OracleType.BINARY_DOUBLE,
                    "ATAN" to OracleType.BINARY_DOUBLE,
                    "ATAN2" to OracleType.BINARY_DOUBLE,
                    "EXP" to OracleType.BINARY_DOUBLE,
                    "LN" to OracleType.BINARY_DOUBLE,
                    "LOG" to OracleType.BINARY_DOUBLE,
                    "SQRT" to OracleType.BINARY_DOUBLE,
                    "IS_UUID" to OracleType.BOOLEAN_TYPE,
                    "DOMAIN_CHECK" to OracleType.BOOLEAN_TYPE,
                    "DOMAIN_CHECK_TYPE" to OracleType.BOOLEAN_TYPE,
                    "REGEXP_LIKE" to OracleType.BOOLEAN_TYPE,
                    "RAW_TO_UUID" to OracleType.TEXT,
                    "DOMAIN_NAME" to OracleType.TEXT,
                )

            mappings.map { (functionName, expectedType) -> functionName to OracleType.fromFunctionName(functionName) } shouldBe mappings
        }

        test("leaves unsupported Oracle function names to the parent resolver exactly") {
            OracleType.fromFunctionName("UNSUPPORTED_FUNCTION") shouldBe null
            OracleType.fromFunctionName("nvl") shouldBe null
            OracleType.fromFunctionName("coalesce") shouldBe null
        }

        test("maps Oracle comparable function argument types exactly") {
            val mappings =
                listOf(
                    Triple("COALESCE", listOf(OracleType.INTEGER_NUMBER, OracleType.LONG_NUMBER), OracleType.LONG_NUMBER),
                    Triple("NVL", listOf(OracleType.TEXT, OracleType.DECIMAL_NUMBER), OracleType.TEXT),
                    Triple("NVL2", listOf(OracleType.BOOLEAN_TYPE, OracleType.DATE, OracleType.TIMESTAMP), OracleType.TIMESTAMP),
                    Triple("GREATEST", listOf(OracleType.BINARY_FLOAT, OracleType.BINARY_DOUBLE), OracleType.BINARY_DOUBLE),
                    Triple("LEAST", listOf(OracleType.DATE, OracleType.TIMESTAMP_TIME_ZONE), OracleType.TIMESTAMP_TIME_ZONE),
                    Triple("MAX", listOf(OracleType.TEXT, OracleType.BINARY), OracleType.BINARY),
                    Triple("MIN", listOf(OracleType.TIMESTAMP, OracleType.DATE), OracleType.DATE),
                    Triple("UNSUPPORTED_FUNCTION", listOf(OracleType.INTEGER_NUMBER, OracleType.LONG_NUMBER), null),
                    Triple("NVL2", listOf(OracleType.TEXT), null),
                )

            mappings.map { (functionName, argumentTypes, expectedType) ->
                Triple(functionName, argumentTypes, OracleType.fromComparableFunctionTypes(functionName, argumentTypes))
            } shouldBe mappings
        }

        test("maps Oracle aggregate function argument types exactly") {
            val mappings =
                listOf(
                    Triple("SUM", OracleType.INTEGER_NUMBER, OracleType.LONG_NUMBER),
                    Triple("SUM", OracleType.LONG_NUMBER, OracleType.LONG_NUMBER),
                    Triple("SUM", OracleType.DECIMAL_NUMBER, OracleType.DECIMAL_NUMBER),
                    Triple("SUM", OracleType.BINARY_FLOAT, OracleType.BINARY_FLOAT),
                    Triple("SUM", OracleType.BINARY_DOUBLE, OracleType.BINARY_DOUBLE),
                    Triple("SUM", OracleType.TEXT, null),
                    Triple("COUNT", OracleType.LONG_NUMBER, null),
                )

            mappings.map { (functionName, argumentType, expectedType) ->
                Triple(functionName, argumentType, OracleType.fromAggregateFunctionType(functionName, argumentType))
            } shouldBe mappings
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
