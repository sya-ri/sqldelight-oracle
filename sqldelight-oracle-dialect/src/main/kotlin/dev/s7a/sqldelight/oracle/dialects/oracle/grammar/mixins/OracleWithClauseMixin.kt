package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCteTableName
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.alecstrong.sql.psi.core.psi.SqlWithClauseAuxiliaryStmt
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleWithClause

internal abstract class OracleWithClauseMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleWithClause,
    SqlWithClause {
    override fun getCteTableNameList(): List<SqlCteTableName> = PsiTreeUtil.getChildrenOfTypeAsList(this, SqlCteTableName::class.java)

    override fun getWithClauseAuxiliaryStmtList(): List<SqlWithClauseAuxiliaryStmt> =
        PsiTreeUtil.getChildrenOfTypeAsList(this, SqlWithClauseAuxiliaryStmt::class.java)
}
