package dev.s7a.sqldelight.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTableConstraint
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.grammar.psi.OracleAlterTableAddConstraint

internal abstract class AlterTableAddConstraintMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableAddConstraint,
    AlterTableApplier {
    private val tableConstraint: SqlTableConstraint
        get() = notNullChild(PsiTreeUtil.getChildOfType(this, SqlTableConstraint::class.java))

    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        if (tableConstraint.node.findChildByType(SqlTypes.PRIMARY) != null &&
            tableConstraint.node.findChildByType(SqlTypes.KEY) != null
        ) {
            LazyQuery(
                tableName = lazyQuery.tableName,
                query = {
                    val columns =
                        lazyQuery.query.columns.map { queryColumn ->
                            val primaryKeyColumn =
                                tableConstraint.indexedColumnList.find { indexedColumn ->
                                    queryColumn.element.textMatches(indexedColumn)
                                }
                            queryColumn.copy(nullable = if (primaryKeyColumn != null) false else queryColumn.nullable)
                        }
                    lazyQuery.query.copy(columns = columns)
                },
            )
        } else {
            lazyQuery
        }
}
