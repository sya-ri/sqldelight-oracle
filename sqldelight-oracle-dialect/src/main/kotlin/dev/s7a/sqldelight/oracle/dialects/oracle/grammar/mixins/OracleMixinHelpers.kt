package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlQualifiedTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal fun PsiElement.oracleColumnDefinitions(): List<SqlColumnDef> = children.filterIsInstance<SqlColumnDef>()

internal fun PsiElement.oracleSingleColumnDefinition(): SqlColumnDef = oracleColumnDefinitions().single()

internal fun PsiElement.oracleFirstColumnName(): SqlColumnName = children.filterIsInstance<SqlColumnName>().first()

internal fun PsiElement.oracleSingleColumnName(): SqlColumnName = children.filterIsInstance<SqlColumnName>().single()

internal fun PsiElement.oracleSingleColumnAlias(): SqlColumnAlias = children.filterIsInstance<SqlColumnAlias>().single()

internal fun LazyQuery.withOracleColumns(transform: (List<QueryColumn>) -> List<QueryColumn>): LazyQuery =
    LazyQuery(
        tableName = tableName,
        query = {
            query.copy(columns = transform(query.columns))
        },
    )

internal fun PsiElement.oracleColumnAliasQueryColumn(): QueryColumn? =
    PsiTreeUtil.getChildOfType(this, SqlColumnAlias::class.java)?.let(::QueryColumn)

internal fun PsiElement.oracleAliasNamedTarget(base: Collection<QueryResult>): Collection<QueryResult> {
    val qualifiedTableName = PsiTreeUtil.getChildOfType(this, SqlQualifiedTableName::class.java)
    val tableAlias = qualifiedTableName?.let { PsiTreeUtil.getChildOfType(it, SqlTableAlias::class.java) }
    if (qualifiedTableName == null || tableAlias == null) return base

    return base.map { query ->
        if (query.table?.name == qualifiedTableName.tableName.name) {
            query.copy(table = tableAlias)
        } else {
            query
        }
    }
}

internal fun QueryColumn.matchesOracleNamedElement(element: NamedElement): Boolean = (this.element as NamedElement).textMatches(element)
