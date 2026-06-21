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
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
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
                    "ALTER SESSION",
                    "ALTER SYSTEM",
                    "ANALYZE",
                    "CALL",
                    "COMMENT",
                    "CREATE CLUSTER",
                    "CREATE DATABASE LINK",
                    "CREATE DIRECTORY",
                    "CREATE INDEXTYPE",
                    "CREATE MATERIALIZED VIEW",
                    "CREATE MATERIALIZED VIEW LOG",
                    "CREATE PACKAGE",
                    "CREATE PROCEDURE",
                    "CREATE SEQUENCE",
                    "CREATE SYNONYM",
                    "CREATE TRIGGER",
                    "CREATE TYPE",
                    "EXPLAIN PLAN",
                    "FLASHBACK",
                    "GRANT",
                    "LOCK TABLE",
                    "MERGE",
                    "PURGE",
                    "REVOKE",
                    "TRUNCATE",
                    roles = setOf(StatementStart, SqlDelightStatementStart),
                ) +
                sourcePatterns("CREATE SEQUENCE", roles = setOf(CreateSequenceStatementStart)) +
                sourcePatterns(
                    "CONNECT BY",
                    "FETCH {FIRST|NEXT} [ROW|ROWS]",
                    "FOR UPDATE",
                    "MATCH_RECOGNIZE",
                    "MODEL",
                    "OFFSET",
                    "PIVOT",
                    "QUALIFY",
                    "RETURNING",
                    "START WITH",
                    "UNPIVOT",
                    roles = setOf(ClauseBoundary),
                ) +
                sourcePatterns("CONNECT BY", roles = setOf(ConnectByClause)) +
                sourcePatterns("START WITH", roles = setOf(StartWithClause)) +
                sourcePatterns("FETCH", "FOR", "OFFSET", "RETURNING", roles = setOf(OrderByBoundary)) +
                sourcePatterns(
                    "CONNECT",
                    "FETCH",
                    "FOR",
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
                    "NUMBER",
                    "NVARCHAR2",
                    "PLS_INTEGER",
                    "RAW",
                    "ROWID",
                    "SDO_GEOMETRY",
                    "TIMESTAMP",
                    "UROWID",
                    "VARCHAR2",
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
