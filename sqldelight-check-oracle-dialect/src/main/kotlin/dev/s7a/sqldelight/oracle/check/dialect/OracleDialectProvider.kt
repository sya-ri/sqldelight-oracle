package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider

/**
 * Resolves the published SQLDelight Oracle dialect artifact.
 */
public class OracleDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != "dev.s7a.sqldelight.oracle") return null
        if (coordinate.module != "sqldelight-oracle-dialect") return null
        return OracleDialect
    }
}
