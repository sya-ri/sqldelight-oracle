package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.GroupByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinConditionBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OrderByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionEndStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionStartStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns

/**
 * Conservative source scanner patterns for Oracle Database SQL.
 */
public val OracleDialectSourcePatterns: SqlDialectSourcePatterns =
    SqlDialectSourcePatterns(
        patterns =
            SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                sourcePatterns(
                    "ALTER INDEX",
                    "ALTER MATERIALIZED VIEW",
                    "ALTER ROLE",
                    "ALTER SEQUENCE",
                    "ALTER SESSION",
                    "ALTER SYSTEM",
                    "ALTER TABLE",
                    "ALTER USER",
                    "ALTER VIEW",
                    "ANALYZE",
                    "CALL",
                    "COMMENT",
                    "COMMIT",
                    "CREATE ANALYTIC VIEW",
                    "CREATE CLUSTER",
                    "CREATE DATABASE LINK",
                    "CREATE DIRECTORY",
                    "CREATE INDEXTYPE",
                    "CREATE MATERIALIZED VIEW",
                    "CREATE MATERIALIZED VIEW LOG",
                    "CREATE PACKAGE",
                    "CREATE PROCEDURE",
                    "CREATE PROPERTY GRAPH",
                    "CREATE ROLE",
                    "CREATE SEQUENCE",
                    "CREATE SYNONYM",
                    "CREATE TRIGGER",
                    "CREATE TYPE",
                    "CREATE USER",
                    "DROP INDEX",
                    "DROP MATERIALIZED VIEW",
                    "DROP ROLE",
                    "DROP SEQUENCE",
                    "DROP SYNONYM",
                    "DROP TABLE",
                    "DROP USER",
                    "DROP VIEW",
                    "EXPLAIN PLAN",
                    "FLASHBACK",
                    "GRANT",
                    "LOCK TABLE",
                    "MERGE",
                    "PURGE",
                    "RENAME",
                    "REVOKE",
                    "ROLLBACK",
                    "SAVEPOINT",
                    "SET CONSTRAINT",
                    "SET TRANSACTION",
                    "TRUNCATE",
                    roles = setOf(StatementStart, SqlDelightStatementStart),
                ) +
                sourcePatterns(
                    "INSERT ALL",
                    "INSERT FIRST",
                    "MERGE",
                    roles = setOf(SqlDelightExecutableStatementStart),
                ) +
                sourcePatterns("SET TRANSACTION", roles = setOf(TransactionStartStatement)) +
                sourcePatterns("COMMIT", "ROLLBACK", roles = setOf(TransactionEndStatement)) +
                sourcePatterns("CREATE SEQUENCE", roles = setOf(CreateSequenceStatementStart)) +
                sourcePatterns(
                    "AS OF SCN",
                    "AS OF TIMESTAMP",
                    "ADD MEASURES",
                    "CONNECT BY",
                    "COLUMNS",
                    "DEFAULT MEASURE",
                    "DESTINATION KEY",
                    "DIMENSION BY",
                    "EDGE TABLES",
                    "FETCH {FIRST|NEXT} [ROW|ROWS]",
                    "FILTER FACT",
                    "FOR UPDATE",
                    "GRAPH_TABLE",
                    "HIERARCHIES",
                    "INSERT ALL",
                    "INSERT FIRST",
                    "JSON_TABLE",
                    "MATCH",
                    "MATCH_RECOGNIZE",
                    "MEASURES",
                    "MODEL",
                    "OFFSET",
                    "PIVOT",
                    "PROPERTIES",
                    "QUALIFY",
                    "RETURNING",
                    "SOURCE KEY",
                    "START WITH",
                    "UNPIVOT",
                    "VERTEX TABLES",
                    "VERSIONS BETWEEN",
                    "XMLTABLE",
                    roles = setOf(ClauseBoundary),
                ) +
                sourcePatterns("CONNECT BY", roles = setOf(ConnectByClause)) +
                sourcePatterns("START WITH", roles = setOf(StartWithClause)) +
                sourcePatterns("FETCH", "FOR", "OFFSET", "RETURNING", roles = setOf(OrderByBoundary)) +
                sourcePatterns(
                    "CONNECT",
                    "FETCH",
                    "FOR",
                    "GRAPH_TABLE",
                    "MATCH",
                    "MODEL",
                    "OFFSET",
                    "RETURNING",
                    "START",
                    roles = setOf(GroupByBoundary, PredicateBoundary, JoinConditionBoundary),
                ) +
                sourcePatterns("CONNECT", "FETCH", "FOR", "MODEL", "OFFSET", "START", roles = setOf(TableReferenceBoundary)) +
                sourcePatterns(
                    "ANYDATA",
                    "BFILE",
                    "BINARY_DOUBLE",
                    "BINARY_FLOAT",
                    "BINARY_INTEGER",
                    "BLOB",
                    "BOOLEAN",
                    "CHAR",
                    "CLOB",
                    "DATE",
                    "INTERVAL",
                    "JSON",
                    "LONG",
                    "LONG RAW",
                    "NCHAR",
                    "NCLOB",
                    "NESTED TABLE",
                    "OBJECT",
                    "NUMBER",
                    "NVARCHAR2",
                    "PLS_INTEGER",
                    "RAW",
                    "REF",
                    "ROWID",
                    "SDO_GEOMETRY",
                    "TIMESTAMP",
                    "URITYPE",
                    "UROWID",
                    "VARCHAR2",
                    "VARRAY",
                    "VARYING ARRAY",
                    "VECTOR",
                    "XMLTYPE",
                    roles = setOf(DataTypeName),
                ) +
                sourcePatterns(
                    "COALESCE",
                    "CURRENT_DATE",
                    "CURRENT_TIMESTAMP",
                    "DECODE",
                    "JSON_ARRAY",
                    "JSON_OBJECT",
                    "LISTAGG",
                    "NANVL",
                    "NVL",
                    "NVL2",
                    "REGEXP_LIKE",
                    "SYSTIMESTAMP",
                    "TO_CHAR",
                    "TO_DATE",
                    "TO_NUMBER",
                    roles = setOf(CommonFunctionName),
                ) +
                sourcePatterns(
                    "CONNECT",
                    "MATCH_RECOGNIZE",
                    "MODEL",
                    "PIVOT",
                    "QUALIFY",
                    "UNPIVOT",
                    roles = setOf(AliasBoundary, KeywordCaseTarget),
                ) +
                sourcePatterns("''", roles = setOf(EmptyStringLiteral)),
    )
