package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.SqlQualifiedTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableOrSubquery
import com.alecstrong.sql.psi.core.psi.impl.SqlUpdateStmtImpl
import com.alecstrong.sql.psi.core.psi.impl.SqlUpdateStmtLimitedImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleFromUsingClause

internal abstract class OracleUpdateStmtMixin(
    node: ASTNode,
) : SqlUpdateStmtImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> = oracleUpdateQueryAvailable(super.queryAvailable(child))
}

internal abstract class OracleUpdateStmtLimitedMixin(
    node: ASTNode,
) : SqlUpdateStmtLimitedImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> = oracleUpdateQueryAvailable(super.queryAvailable(child))
}

private fun PsiElement.oracleUpdateQueryAvailable(base: Collection<QueryResult>): Collection<QueryResult> =
    aliasUpdateTarget(base) + fromUsingQueryExposed()

private fun PsiElement.aliasUpdateTarget(base: Collection<QueryResult>): Collection<QueryResult> {
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

private fun PsiElement.fromUsingQueryExposed(): Collection<QueryResult> {
    val fromUsingClause = PsiTreeUtil.getChildOfType(this, OracleOracleFromUsingClause::class.java) ?: return emptyList()

    val tableSources =
        PsiTreeUtil
            .getChildrenOfTypeAsList(fromUsingClause, SqlTableOrSubquery::class.java)
            .flatMap { it.queryExposed() }
    val joinSources =
        PsiTreeUtil
            .getChildrenOfTypeAsList(fromUsingClause, SqlJoinClause::class.java)
            .flatMap { it.queryExposed() }

    return tableSources + joinSources
}
