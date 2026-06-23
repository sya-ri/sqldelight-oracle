package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.impl.SqlDeleteStmtImpl
import com.alecstrong.sql.psi.core.psi.impl.SqlDeleteStmtLimitedImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

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

private fun PsiElement.aliasDeleteTarget(base: Collection<QueryResult>): Collection<QueryResult> = oracleAliasNamedTarget(base)
