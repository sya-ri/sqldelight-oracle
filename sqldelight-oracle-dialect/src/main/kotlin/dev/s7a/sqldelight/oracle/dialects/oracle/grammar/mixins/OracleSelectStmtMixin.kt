package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.impl.SqlSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleWindowClause

internal abstract class OracleSelectStmtMixin(
    node: ASTNode,
) : SqlSelectStmtImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
        val base = super.queryAvailable(child)
        val windowClause = PsiTreeUtil.getChildOfType(this, OracleOracleWindowClause::class.java)
        if (windowClause == null || !PsiTreeUtil.isAncestor(windowClause, child, false)) {
            return base
        }

        val fromSources =
            PsiTreeUtil
                .getChildOfType(this, SqlJoinClause::class.java)
                ?.queryExposed()
                ?: emptyList()
        return base + fromSources
    }
}
