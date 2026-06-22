package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole

/**
 * Oracle `CREATE SEQUENCE` statement start.
 */
public data object CreateSequenceStatementStart : SqlDialectSourcePatternRole

/**
 * Oracle `CONNECT BY` hierarchical query clause.
 */
public data object ConnectByClause : SqlDialectSourcePatternRole

/**
 * Oracle `START WITH` hierarchical query clause.
 */
public data object StartWithClause : SqlDialectSourcePatternRole

/**
 * Oracle AI Vector Search shorthand distance operator.
 */
public data object VectorDistanceOperator : SqlDialectSourcePatternRole

/**
 * Oracle empty-string literal, which Oracle treats as null in SQL.
 */
public data object EmptyStringLiteral : SqlDialectSourcePatternRole
