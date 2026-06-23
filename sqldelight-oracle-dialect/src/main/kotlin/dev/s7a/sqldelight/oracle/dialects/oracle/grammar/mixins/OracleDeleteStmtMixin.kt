package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlQualifiedTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.impl.SqlDeleteStmtImpl
import com.alecstrong.sql.psi.core.psi.impl.SqlDeleteStmtLimitedImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal abstract class OracleDeleteStmtMixin(
    node: ASTNode,
) : SqlDeleteStmtImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> = aliasDeleteTarget(super.queryAvailable(child))
}

internal abstract class OracleDeleteStmtLimitedMixin(
    node: ASTNode,
) : SqlDeleteStmtLimitedImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> = aliasDeleteTarget(super.queryAvailable(child))
}

private fun PsiElement.aliasDeleteTarget(base: Collection<QueryResult>): Collection<QueryResult> {
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
