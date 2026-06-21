package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * Oracle Database dialect ID.
 */
public val OracleDialectId: DialectId = DialectId("oracle")

/**
 * sqldelight-check metadata for Oracle Database SQL.
 */
public val OracleDialect: SqlDialect =
    SqlDialect(
        ids = setOf(OracleDialectId),
        sourcePatterns = OracleDialectSourcePatterns,
    )
