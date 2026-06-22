package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionEndStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionStartStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import io.kotest.core.spec.style.FunSpec
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

            providers.map { provider -> provider::class } shouldBe listOf(OracleDialectProvider::class)
        }

        test("publishes SQL dialect provider ServiceLoader resource exactly") {
            serviceResource("META-INF/services/dev.s7a.sqldelight.check.api.SqlDialectProvider") shouldBe
                "dev.s7a.sqldelight.oracle.check.dialect.OracleDialectProvider\n"
        }

        oracleStatementStarts.forEach { syntax ->
            test("recognizes Oracle statement start: ${syntax.expression}") {
                OracleDialectSourcePatterns.shouldMatchExactly(StatementStart, syntax)
            }
        }

        oracleExecutableStatementStarts.forEach { syntax ->
            test("recognizes Oracle executable statement start: ${syntax.expression}") {
                OracleDialectSourcePatterns.shouldMatchExactly(SqlDelightExecutableStatementStart, syntax)
            }
        }

        oracleClauseBoundaries.forEach { syntax ->
            test("recognizes Oracle clause boundary: ${syntax.expression}") {
                OracleDialectSourcePatterns.shouldMatchExactly(ClauseBoundary, syntax)
            }
        }

        oracleDataTypes.forEach { syntax ->
            test("recognizes Oracle data type: ${syntax.expression}") {
                OracleDialectSourcePatterns.shouldMatchExactly(DataTypeName, syntax)
            }
        }

        oracleCommonFunctions.forEach { syntax ->
            test("recognizes Oracle common function: ${syntax.expression}") {
                OracleDialectSourcePatterns.shouldMatchExactly(CommonFunctionName, syntax)
            }
        }

        test("recognizes Oracle CREATE SEQUENCE statement role") {
            OracleDialectSourcePatterns.shouldMatchExactly(CreateSequenceStatementStart, Syntax("CREATE SEQUENCE"))
        }

        test("recognizes Oracle CONNECT BY clause role") {
            OracleDialectSourcePatterns.shouldMatchExactly(ConnectByClause, Syntax("CONNECT BY"))
        }

        test("recognizes Oracle START WITH clause role") {
            OracleDialectSourcePatterns.shouldMatchExactly(StartWithClause, Syntax("START WITH"))
        }

        listOf("<->", "<=>", "<#>").forEach { operator ->
            test("recognizes Oracle vector distance operator: $operator") {
                OracleDialectSourcePatterns.shouldMatchExactly(VectorDistanceOperator, Syntax(operator))
            }
        }

        test("recognizes Oracle transaction start") {
            OracleDialectSourcePatterns.shouldMatchExactly(TransactionStartStatement, Syntax("SET TRANSACTION"))
        }

        test("recognizes Oracle transaction end") {
            OracleDialectSourcePatterns.shouldMatchExactly(TransactionEndStatement, Syntax("ROLLBACK"))
        }
    })

private fun SqlDialectSourcePatterns.shouldMatchExactly(
    role: SqlDialectSourcePatternRole,
    syntax: Syntax,
) {
    matchPrefix(role, syntax.terms) shouldBe syntax.terms.size
}

private val oracleStatementStarts =
    listOf(
        Syntax("ALTER INDEX"),
        Syntax("ALTER DOMAIN"),
        Syntax("ALTER MATERIALIZED VIEW"),
        Syntax("ALTER MATERIALIZED VIEW LOG"),
        Syntax("ALTER MATERIALIZED ZONEMAP"),
        Syntax("ALTER ANALYTIC VIEW"),
        Syntax("ALTER ASSERTION"),
        Syntax("ALTER ATTRIBUTE DIMENSION"),
        Syntax("ALTER CLUSTER"),
        Syntax("ALTER DATABASE"),
        Syntax("ALTER DATABASE DICTIONARY"),
        Syntax("ALTER DATABASE LINK"),
        Syntax("ALTER DIMENSION"),
        Syntax("ALTER DIRECTIVE"),
        Syntax("ALTER DISKGROUP"),
        Syntax("ALTER FLASHBACK ARCHIVE"),
        Syntax("ALTER FUNCTION"),
        Syntax("ALTER HIERARCHY"),
        Syntax("ALTER INDEXTYPE"),
        Syntax("ALTER INMEMORY JOIN GROUP"),
        Syntax("ALTER JAVA"),
        Syntax("ALTER JSON RELATIONAL DUALITY VIEW"),
        Syntax("ALTER LIBRARY"),
        Syntax("ALTER LOCKDOWN PROFILE"),
        Syntax("ALTER MLE ENV"),
        Syntax("ALTER MLE MODULE"),
        Syntax("ALTER OPERATOR"),
        Syntax("ALTER OUTLINE"),
        Syntax("ALTER PACKAGE"),
        Syntax("ALTER PLUGGABLE DATABASE"),
        Syntax("ALTER PMEM FILESTORE"),
        Syntax("ALTER PROCEDURE"),
        Syntax("ALTER PROFILE"),
        Syntax("ALTER PROPERTY GRAPH"),
        Syntax("ALTER RESOURCE COST"),
        Syntax("ALTER ROLE"),
        Syntax("ALTER ROLLBACK SEGMENT"),
        Syntax("ALTER SEQUENCE"),
        Syntax("ALTER SESSION"),
        Syntax("ALTER SYNONYM"),
        Syntax("ALTER SYSTEM"),
        Syntax("ALTER TABLE"),
        Syntax("ALTER TABLESPACE"),
        Syntax("ALTER TABLESPACE SET"),
        Syntax("ALTER TRIGGER"),
        Syntax("ALTER TYPE"),
        Syntax("ALTER USER"),
        Syntax("ALTER VIEW"),
        Syntax("ADMINISTER KEY MANAGEMENT"),
        Syntax("ANALYZE"),
        Syntax("ASSOCIATE STATISTICS"),
        Syntax("AUDIT"),
        Syntax("CALL"),
        Syntax("COMMENT"),
        Syntax("COMMIT"),
        Syntax("CREATE ANALYTIC VIEW"),
        Syntax("CREATE APPLICATION IDENTITY"),
        Syntax("CREATE ASSERTION"),
        Syntax("CREATE ATTRIBUTE DIMENSION"),
        Syntax("CREATE AUDIT POLICY"),
        Syntax("CREATE CLUSTER"),
        Syntax("CREATE CONTEXT"),
        Syntax("CREATE CONTROLFILE"),
        Syntax("CREATE DATA GRANT"),
        Syntax("CREATE DATA ROLE"),
        Syntax("CREATE DATABASE LINK"),
        Syntax("CREATE DATABASE"),
        Syntax("CREATE DIMENSION"),
        Syntax("CREATE DIRECTORY"),
        Syntax("CREATE DIRECTIVE"),
        Syntax("CREATE DISKGROUP"),
        Syntax("CREATE DOMAIN"),
        Syntax("CREATE EDITION"),
        Syntax("CREATE END USER"),
        Syntax("CREATE END USER CONTEXT"),
        Syntax("CREATE FLEXIBLE DOMAIN"),
        Syntax("CREATE FLASHBACK ARCHIVE"),
        Syntax("CREATE FUNCTION"),
        Syntax("CREATE HIERARCHY"),
        Syntax("CREATE HYBRID VECTOR INDEX"),
        Syntax("CREATE ICEBERG TABLE"),
        Syntax("CREATE INDEX"),
        Syntax("CREATE INDEXTYPE"),
        Syntax("CREATE INMEMORY JOIN GROUP"),
        Syntax("CREATE JAVA"),
        Syntax("CREATE JSON RELATIONAL DUALITY VIEW"),
        Syntax("CREATE LIBRARY"),
        Syntax("CREATE LOCKDOWN PROFILE"),
        Syntax("CREATE LOGICAL PARTITION TRACKING"),
        Syntax("CREATE MATERIALIZED VIEW"),
        Syntax("CREATE MATERIALIZED VIEW LOG"),
        Syntax("CREATE MATERIALIZED ZONEMAP"),
        Syntax("CREATE MLE ENV"),
        Syntax("CREATE MLE MODULE"),
        Syntax("CREATE OPERATOR"),
        Syntax("CREATE OUTLINE"),
        Syntax("CREATE PACKAGE"),
        Syntax("CREATE PACKAGE BODY"),
        Syntax("CREATE PFILE"),
        Syntax("CREATE PLUGGABLE DATABASE"),
        Syntax("CREATE PMEM FILESTORE"),
        Syntax("CREATE PROCEDURE"),
        Syntax("CREATE PROFILE"),
        Syntax("CREATE PROPERTY GRAPH"),
        Syntax("CREATE RESTORE POINT"),
        Syntax("CREATE ROLE"),
        Syntax("CREATE ROLLBACK SEGMENT"),
        Syntax("CREATE SCHEMA"),
        Syntax("CREATE SEQUENCE"),
        Syntax("CREATE SPFILE"),
        Syntax("CREATE SYNONYM"),
        Syntax("CREATE TRIGGER"),
        Syntax("CREATE TRUE CACHE"),
        Syntax("CREATE TYPE"),
        Syntax("CREATE TYPE BODY"),
        Syntax("CREATE USER"),
        Syntax("CREATE VECTOR INDEX"),
        Syntax("CREATE VIEW"),
        Syntax("DISASSOCIATE STATISTICS"),
        Syntax("DROP ANALYTIC VIEW"),
        Syntax("DROP APPLICATION IDENTITY"),
        Syntax("DROP ASSERTION"),
        Syntax("DROP ATTRIBUTE DIMENSION"),
        Syntax("DROP AUDIT POLICY"),
        Syntax("DROP CLUSTER"),
        Syntax("DROP CONTEXT"),
        Syntax("DROP DATA GRANT"),
        Syntax("DROP DATA ROLE"),
        Syntax("DROP DATABASE LINK"),
        Syntax("DROP DATABASE"),
        Syntax("DROP DIMENSION"),
        Syntax("DROP DIRECTORY"),
        Syntax("DROP DIRECTIVE"),
        Syntax("DROP DISKGROUP"),
        Syntax("DROP EDITION"),
        Syntax("DROP END USER"),
        Syntax("DROP END USER CONTEXT"),
        Syntax("DROP FLASHBACK ARCHIVE"),
        Syntax("DROP FUNCTION"),
        Syntax("DROP HIERARCHY"),
        Syntax("DROP ICEBERG TABLE"),
        Syntax("DROP INDEX"),
        Syntax("DROP DOMAIN"),
        Syntax("DROP INDEXTYPE"),
        Syntax("DROP INMEMORY JOIN GROUP"),
        Syntax("DROP JAVA"),
        Syntax("DROP LIBRARY"),
        Syntax("DROP LOCKDOWN PROFILE"),
        Syntax("DROP MATERIALIZED VIEW"),
        Syntax("DROP MATERIALIZED VIEW LOG"),
        Syntax("DROP MATERIALIZED ZONEMAP"),
        Syntax("DROP MLE ENV"),
        Syntax("DROP MLE MODULE"),
        Syntax("DROP OPERATOR"),
        Syntax("DROP OUTLINE"),
        Syntax("DROP PACKAGE"),
        Syntax("DROP PLUGGABLE DATABASE"),
        Syntax("DROP PMEM FILESTORE"),
        Syntax("DROP PROCEDURE"),
        Syntax("DROP PROFILE"),
        Syntax("DROP PROPERTY GRAPH"),
        Syntax("DROP RESTORE POINT"),
        Syntax("DROP ROLE"),
        Syntax("DROP ROLLBACK SEGMENT"),
        Syntax("DROP SEQUENCE"),
        Syntax("DROP SYNONYM"),
        Syntax("DROP TABLE"),
        Syntax("DROP TABLESPACE"),
        Syntax("DROP TABLESPACE SET"),
        Syntax("DROP TRIGGER"),
        Syntax("DROP TYPE"),
        Syntax("DROP TYPE BODY"),
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
        Syntax("REVOKE DATA ROLE"),
        Syntax("ROLLBACK"),
        Syntax("SAVEPOINT"),
        Syntax("SET CONSTRAINTS"),
        Syntax("SET ROLE"),
        Syntax("SET TRANSACTION"),
        Syntax("SET USE DATA GRANTS ONLY"),
        Syntax("TRUNCATE"),
        Syntax("TRUNCATE CLUSTER"),
        Syntax("UPDATE END USER CONTEXT"),
    )

private val oracleExecutableStatementStarts =
    listOf(
        Syntax("DELETE"),
        Syntax("INSERT"),
        Syntax("INSERT ALL"),
        Syntax("INSERT FIRST"),
        Syntax("MERGE"),
        Syntax("SELECT"),
        Syntax("UPDATE"),
    )

private val oracleClauseBoundaries =
    listOf(
        Syntax("AS OF SCN"),
        Syntax("AS OF TIMESTAMP"),
        Syntax("ADD MEASURES"),
        Syntax("ALTER DOMAIN"),
        Syntax("ANNOTATIONS"),
        Syntax("CHOOSE DOMAIN USING"),
        Syntax("CONNECT BY"),
        Syntax("COLUMNS"),
        Syntax("DEFAULT MEASURE"),
        Syntax("DEFAULT ON NULL"),
        Syntax("DESTINATION KEY"),
        Syntax("DISPLAY"),
        Syntax("DIMENSION BY"),
        Syntax("EDGE TABLES"),
        Syntax("ENUM"),
        Syntax("FETCH FIRST ROW"),
        Syntax("FETCH NEXT ROWS"),
        Syntax("FILTER FACT"),
        Syntax("FOR UPDATE"),
        Syntax("GRAPH_TABLE"),
        Syntax("GROUPS"),
        Syntax("HIERARCHIES"),
        Syntax("INSERT ALL"),
        Syntax("INSERT FIRST"),
        Syntax("JSON_TABLE"),
        Syntax("LATERAL"),
        Syntax("MATCH"),
        Syntax("MATCH_RECOGNIZE"),
        Syntax("MEASURES"),
        Syntax("MODEL"),
        Syntax("OFFSET"),
        Syntax("ONLY"),
        Syntax("ORDER"),
        Syntax("PARTITION"),
        Syntax("PIVOT"),
        Syntax("PROPERTIES"),
        Syntax("QUALIFY"),
        Syntax("RETURNING"),
        Syntax("SAMPLE"),
        Syntax("SEARCH"),
        Syntax("SHARDS"),
        Syntax("SOURCE KEY"),
        Syntax("START WITH"),
        Syntax("STRICT"),
        Syntax("UNPIVOT"),
        Syntax("USECASE"),
        Syntax("VERTEX TABLES"),
        Syntax("VERSIONS BETWEEN"),
        Syntax("XMLTABLE"),
    )

private val oracleDataTypes =
    listOf(
        Syntax("ANYDATA"),
        Syntax("ANYDATASET"),
        Syntax("ANYTYPE"),
        Syntax("BFILE"),
        Syntax("BINARY_DOUBLE"),
        Syntax("BINARY_FLOAT"),
        Syntax("BINARY_INTEGER"),
        Syntax("BLOB"),
        Syntax("BOOLEAN"),
        Syntax("CHAR"),
        Syntax("CHARACTER"),
        Syntax("CHARACTER VARYING"),
        Syntax("CLOB"),
        Syntax("DATE"),
        Syntax("DEC"),
        Syntax("DECIMAL"),
        Syntax("DBURITYPE"),
        Syntax("DOUBLE PRECISION"),
        Syntax("FLOAT"),
        Syntax("HTTPURITYPE"),
        Syntax("INT"),
        Syntax("INTEGER"),
        Syntax("INTERVAL"),
        Syntax("JSON"),
        Syntax("LONG"),
        Syntax("LONG RAW"),
        Syntax("NATIONAL CHAR"),
        Syntax("NATIONAL CHAR VARYING"),
        Syntax("NATIONAL CHARACTER"),
        Syntax("NATIONAL CHARACTER VARYING"),
        Syntax("NCHAR"),
        Syntax("NCLOB"),
        Syntax("NESTED TABLE"),
        Syntax("NUMBER"),
        Syntax("NUMERIC"),
        Syntax("NVARCHAR2"),
        Syntax("OBJECT"),
        Syntax("PLS_INTEGER"),
        Syntax("RAW"),
        Syntax("REF"),
        Syntax("REAL"),
        Syntax("ROWID"),
        Syntax("SDO_GEORASTER"),
        Syntax("SDO_GEOMETRY"),
        Syntax("SDO_TOPO_GEOMETRY"),
        Syntax("SMALLINT"),
        Syntax("TIMESTAMP"),
        Syntax("URITYPE"),
        Syntax("UROWID"),
        Syntax("VARCHAR"),
        Syntax("VARCHAR2"),
        Syntax("VARRAY"),
        Syntax("VARYING ARRAY"),
        Syntax("VECTOR"),
        Syntax("XMLTYPE"),
    )

private val oracleCommonFunctions =
    listOf(
        Syntax("ACOS"),
        Syntax("ASIN"),
        Syntax("ATAN"),
        Syntax("ATAN2"),
        Syntax("AVG"),
        Syntax("COALESCE"),
        Syntax("CLUSTER_DETAILS"),
        Syntax("CLUSTER_DISTANCE"),
        Syntax("CLUSTER_ID"),
        Syntax("CLUSTER_PROBABILITY"),
        Syntax("CLUSTER_SET"),
        Syntax("COS"),
        Syntax("COUNT"),
        Syntax("CURRENT_DATE"),
        Syntax("CURRENT_TIMESTAMP"),
        Syntax("CV"),
        Syntax("DECODE"),
        Syntax("DEREF"),
        Syntax("DENSE_RANK"),
        Syntax("DOMAIN_CHECK"),
        Syntax("DOMAIN_CHECK_TYPE"),
        Syntax("DOMAIN_DISPLAY"),
        Syntax("DOMAIN_NAME"),
        Syntax("DOMAIN_ORDER"),
        Syntax("EXP"),
        Syntax("FEATURE_COMPARE"),
        Syntax("FEATURE_DETAILS"),
        Syntax("FEATURE_ID"),
        Syntax("FEATURE_SET"),
        Syntax("FEATURE_VALUE"),
        Syntax("FROM_VECTOR"),
        Syntax("FUZZY_MATCH"),
        Syntax("GREATEST"),
        Syntax("HAMMING_DISTANCE"),
        Syntax("HEXTORAW"),
        Syntax("INSTR"),
        Syntax("INNER_PRODUCT"),
        Syntax("IS_UUID"),
        Syntax("ITERATION_NUMBER"),
        Syntax("JACCARD_DISTANCE"),
        Syntax("JSON"),
        Syntax("JSON_ARRAY"),
        Syntax("JSON_ARRAYAGG"),
        Syntax("JSON_DATAGUIDE"),
        Syntax("JSON_ID"),
        Syntax("JSON_MERGEPATCH"),
        Syntax("JSON_OBJECT"),
        Syntax("JSON_OBJECTAGG"),
        Syntax("JSON_QUERY"),
        Syntax("JSON_SCALAR"),
        Syntax("JSON_SERIALIZE"),
        Syntax("JSON_TRANSFORM"),
        Syntax("JSON_VALUE"),
        Syntax("L1_DISTANCE"),
        Syntax("L2_DISTANCE"),
        Syntax("LEAST"),
        Syntax("LENGTH"),
        Syntax("LISTAGG"),
        Syntax("LN"),
        Syntax("LOCALTIMESTAMP"),
        Syntax("LOG"),
        Syntax("MAKE_REF"),
        Syntax("MAX"),
        Syntax("MEDIAN"),
        Syntax("MIN"),
        Syntax("NANVL"),
        Syntax("NTILE"),
        Syntax("NVL"),
        Syntax("NVL2"),
        Syntax("ORA_DM_PARTITION_NAME"),
        Syntax("PHONIC_ENCODE"),
        Syntax("PRESENTNNV"),
        Syntax("PRESENTV"),
        Syntax("PREVIOUS"),
        Syntax("PREDICTION"),
        Syntax("PREDICTION_BOUNDS"),
        Syntax("PREDICTION_COST"),
        Syntax("PREDICTION_DETAILS"),
        Syntax("PREDICTION_PROBABILITY"),
        Syntax("PREDICTION_SET"),
        Syntax("RANK"),
        Syntax("RAW_TO_UUID"),
        Syntax("RAWTOHEX"),
        Syntax("REF"),
        Syntax("REGEXP_LIKE"),
        Syntax("ROW_NUMBER"),
        Syntax("SIN"),
        Syntax("SQRT"),
        Syntax("STDDEV"),
        Syntax("SUM"),
        Syntax("SYSDATE"),
        Syntax("SYSTIMESTAMP"),
        Syntax("TAN"),
        Syntax("TO_CHAR"),
        Syntax("TO_DATE"),
        Syntax("TO_LOB"),
        Syntax("TO_NUMBER"),
        Syntax("TO_TIMESTAMP"),
        Syntax("TO_TIMESTAMP_TZ"),
        Syntax("TO_VECTOR"),
        Syntax("UUID"),
        Syntax("UUID_TO_RAW"),
        Syntax("VARIANCE"),
        Syntax("VECTOR"),
        Syntax("VECTOR_CHUNKS"),
        Syntax("VECTOR_DIMS"),
        Syntax("VECTOR_DIMENSION_COUNT"),
        Syntax("VECTOR_DIMENSION_FORMAT"),
        Syntax("VECTOR_DISTANCE"),
        Syntax("VECTOR_EMBEDDING"),
        Syntax("VECTOR_NORM"),
        Syntax("VECTOR_SERIALIZE"),
        Syntax("XMLAGG"),
        Syntax("XMLELEMENT"),
        Syntax("XMLFOREST"),
        Syntax("XMLQUERY"),
        Syntax("XMLSERIALIZE"),
        Syntax("XMLTYPE"),
    )

private data class Syntax(
    val expression: String,
) {
    val terms: List<String> = expression.split(' ').map { term -> term.lowercase() }
}

private fun serviceResource(path: String): String =
    requireNotNull(OracleDialectProviderTest::class.java.classLoader.getResource(path)) {
        "Missing test resource $path"
    }.readText()
