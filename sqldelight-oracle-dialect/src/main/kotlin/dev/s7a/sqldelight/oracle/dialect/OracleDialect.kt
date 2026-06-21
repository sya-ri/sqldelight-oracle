package dev.s7a.sqldelight.oracle.dialect

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect

/**
 * SQLDelight dialect entry point for Oracle Database.
 *
 * The first implementation delegates parser setup to SQLDelight's JDBC-backed PostgreSQL dialect while Oracle grammar
 * support is expanded. Oracle-specific sqldelight-check metadata lives in `sqldelight-check-oracle-dialect`.
 */
public class OracleDialect : SqlDelightDialect by PostgreSqlDialect()
