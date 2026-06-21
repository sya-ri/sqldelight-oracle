package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionEndStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionStartStatement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleDialectProviderTest :
    FunSpec({
        test("resolves the published Oracle SQLDelight dialect coordinate") {
            OracleDialectProvider().resolve(
                SqlDialectCoordinate("dev.s7a.sqldelight.oracle", "sqldelight-oracle-dialect", null),
            ) shouldBe OracleDialect
        }

        test("ignores unrelated coordinates") {
            OracleDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "postgresql-dialect", null)) shouldBe null
        }

        test("registers provider through ServiceLoader") {
            val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

            providers.map { provider -> provider::class } shouldContain OracleDialectProvider::class
        }

        oracleStatementStarts.forEach { syntax ->
            test("recognizes Oracle statement start: ${syntax.expression}") {
                OracleDialectSourcePatterns.matches(StatementStart, syntax.terms) shouldBe true
            }
        }

        oracleExecutableStatementStarts.forEach { syntax ->
            test("recognizes Oracle executable statement start: ${syntax.expression}") {
                OracleDialectSourcePatterns.matches(SqlDelightExecutableStatementStart, syntax.terms) shouldBe true
            }
        }

        oracleClauseBoundaries.forEach { syntax ->
            test("recognizes Oracle clause boundary: ${syntax.expression}") {
                OracleDialectSourcePatterns.matches(ClauseBoundary, syntax.terms) shouldBe true
            }
        }

        oracleDataTypes.forEach { syntax ->
            test("recognizes Oracle data type: ${syntax.expression}") {
                OracleDialectSourcePatterns.matches(DataTypeName, syntax.terms) shouldBe true
            }
        }

        oracleCommonFunctions.forEach { syntax ->
            test("recognizes Oracle common function: ${syntax.expression}") {
                OracleDialectSourcePatterns.matches(CommonFunctionName, syntax.terms) shouldBe true
            }
        }

        test("recognizes Oracle CREATE SEQUENCE statement role") {
            OracleDialectSourcePatterns.matches(CreateSequenceStatementStart, listOf("create", "sequence")) shouldBe true
        }

        test("recognizes Oracle CONNECT BY clause role") {
            OracleDialectSourcePatterns.matches(ConnectByClause, listOf("connect", "by")) shouldBe true
        }

        test("recognizes Oracle START WITH clause role") {
            OracleDialectSourcePatterns.matches(StartWithClause, listOf("start", "with")) shouldBe true
        }

        test("recognizes Oracle transaction start") {
            OracleDialectSourcePatterns.matches(TransactionStartStatement, listOf("set", "transaction")) shouldBe true
        }

        test("recognizes Oracle transaction end") {
            OracleDialectSourcePatterns.matches(TransactionEndStatement, listOf("rollback")) shouldBe true
        }
    })

private val oracleStatementStarts =
    listOf(
        Syntax("ALTER SESSION"),
        Syntax("ALTER SYSTEM"),
        Syntax("ALTER USER"),
        Syntax("ALTER ROLE"),
        Syntax("ANALYZE"),
        Syntax("CALL"),
        Syntax("COMMENT"),
        Syntax("CREATE CLUSTER"),
        Syntax("CREATE DATABASE LINK"),
        Syntax("CREATE DIRECTORY"),
        Syntax("CREATE INDEXTYPE"),
        Syntax("CREATE MATERIALIZED VIEW"),
        Syntax("CREATE MATERIALIZED VIEW LOG"),
        Syntax("CREATE PACKAGE"),
        Syntax("CREATE PROCEDURE"),
        Syntax("CREATE ROLE"),
        Syntax("CREATE SEQUENCE"),
        Syntax("CREATE SYNONYM"),
        Syntax("CREATE TRIGGER"),
        Syntax("CREATE TYPE"),
        Syntax("CREATE USER"),
        Syntax("DROP MATERIALIZED VIEW"),
        Syntax("DROP ROLE"),
        Syntax("DROP SEQUENCE"),
        Syntax("DROP SYNONYM"),
        Syntax("DROP USER"),
        Syntax("DROP VIEW"),
        Syntax("EXPLAIN PLAN"),
        Syntax("FLASHBACK"),
        Syntax("GRANT"),
        Syntax("LOCK TABLE"),
        Syntax("MERGE"),
        Syntax("PURGE"),
        Syntax("RENAME"),
        Syntax("REVOKE"),
        Syntax("ROLLBACK"),
        Syntax("SAVEPOINT"),
        Syntax("SET CONSTRAINT"),
        Syntax("SET TRANSACTION"),
        Syntax("TRUNCATE"),
    )

private val oracleExecutableStatementStarts =
    listOf(
        Syntax("DELETE"),
        Syntax("INSERT"),
        Syntax("MERGE"),
        Syntax("SELECT"),
        Syntax("UPDATE"),
    )

private val oracleClauseBoundaries =
    listOf(
        Syntax("AS OF SCN"),
        Syntax("AS OF TIMESTAMP"),
        Syntax("CONNECT BY"),
        Syntax("FETCH FIRST ROW"),
        Syntax("FETCH NEXT ROWS"),
        Syntax("FOR UPDATE"),
        Syntax("JSON_TABLE"),
        Syntax("MATCH_RECOGNIZE"),
        Syntax("MODEL"),
        Syntax("OFFSET"),
        Syntax("PIVOT"),
        Syntax("QUALIFY"),
        Syntax("RETURNING"),
        Syntax("START WITH"),
        Syntax("UNPIVOT"),
        Syntax("VERSIONS BETWEEN"),
        Syntax("XMLTABLE"),
    )

private val oracleDataTypes =
    listOf(
        Syntax("ANYDATA"),
        Syntax("BFILE"),
        Syntax("BINARY_DOUBLE"),
        Syntax("BINARY_FLOAT"),
        Syntax("BINARY_INTEGER"),
        Syntax("BLOB"),
        Syntax("BOOLEAN"),
        Syntax("CHAR"),
        Syntax("CLOB"),
        Syntax("DATE"),
        Syntax("INTERVAL"),
        Syntax("JSON"),
        Syntax("LONG"),
        Syntax("LONG RAW"),
        Syntax("NCHAR"),
        Syntax("NCLOB"),
        Syntax("NUMBER"),
        Syntax("NVARCHAR2"),
        Syntax("OBJECT"),
        Syntax("PLS_INTEGER"),
        Syntax("RAW"),
        Syntax("REF"),
        Syntax("ROWID"),
        Syntax("SDO_GEOMETRY"),
        Syntax("TIMESTAMP"),
        Syntax("URITYPE"),
        Syntax("UROWID"),
        Syntax("VARCHAR2"),
        Syntax("VECTOR"),
        Syntax("XMLTYPE"),
    )

private val oracleCommonFunctions =
    listOf(
        Syntax("COALESCE"),
        Syntax("CURRENT_DATE"),
        Syntax("CURRENT_TIMESTAMP"),
        Syntax("DECODE"),
        Syntax("JSON_ARRAY"),
        Syntax("JSON_OBJECT"),
        Syntax("LISTAGG"),
        Syntax("NANVL"),
        Syntax("NVL"),
        Syntax("NVL2"),
        Syntax("REGEXP_LIKE"),
        Syntax("SYSTIMESTAMP"),
        Syntax("TO_CHAR"),
        Syntax("TO_DATE"),
        Syntax("TO_NUMBER"),
    )

private data class Syntax(
    val expression: String,
) {
    val terms: List<String> = expression.split(' ').map { term -> term.lowercase() }
}
