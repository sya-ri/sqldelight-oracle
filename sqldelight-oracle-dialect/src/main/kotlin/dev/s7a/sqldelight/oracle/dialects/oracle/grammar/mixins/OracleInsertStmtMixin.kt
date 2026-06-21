package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlDatabaseName
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlInsertStmtValues
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleInsertStmt

internal abstract class OracleInsertStmtMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleInsertStmt,
    SqlInsertStmt {
    override fun getColumnNameList(): List<SqlColumnName> = PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnName::class.java)

    override fun getDatabaseName(): SqlDatabaseName? = PsiTreeUtil.getChildOfType(this, SqlDatabaseName::class.java)

    override fun getInsertStmtValues(): SqlInsertStmtValues? = null

    override fun queryAvailable(child: PsiElement): Collection<QueryResult> =
        if (child !is SqlWithClause) {
            super.queryAvailable(child) + tableAvailable(child, tableName.name)
        } else {
            super.queryAvailable(child)
        }

    override fun getTableAlias(): SqlTableAlias? = PsiTreeUtil.getChildOfType(this, SqlTableAlias::class.java)

    override fun getTableName(): SqlTableName = notNullChild(PsiTreeUtil.getChildOfType(this, SqlTableName::class.java))

    override fun getWithClause(): SqlWithClause? = PsiTreeUtil.getChildOfType(this, SqlWithClause::class.java)
}
