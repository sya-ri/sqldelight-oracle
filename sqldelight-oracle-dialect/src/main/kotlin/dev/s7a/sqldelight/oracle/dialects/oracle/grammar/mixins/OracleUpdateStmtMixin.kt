package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
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
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> =
        oracleUpdateQueryAvailable(this, super.queryAvailable(child), child) { target, targetName ->
            tableAvailable(target, targetName)
        }
}

internal abstract class OracleUpdateStmtLimitedMixin(
    node: ASTNode,
) : SqlUpdateStmtLimitedImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> =
        oracleUpdateQueryAvailable(this, super.queryAvailable(child), child) { target, targetName ->
            tableAvailable(target, targetName)
        }
}

private fun oracleUpdateQueryAvailable(
    statement: PsiElement,
    base: Collection<QueryResult>,
    child: PsiElement,
    tableResolver: (PsiElement, String) -> Collection<QueryResult>,
): Collection<QueryResult> = statement.oracleDmlTargetAvailable(base, child, tableResolver) + statement.fromUsingQueryExposed()

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
