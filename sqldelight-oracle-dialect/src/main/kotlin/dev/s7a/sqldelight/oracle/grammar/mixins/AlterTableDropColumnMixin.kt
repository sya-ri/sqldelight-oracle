package dev.s7a.sqldelight.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.grammar.psi.OracleAlterTableDropColumn

internal abstract class AlterTableDropColumnMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableDropColumn,
    AlterTableApplier {
    private val columnName: SqlColumnName
        get() = children.filterIsInstance<SqlColumnName>().single()

    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        LazyQuery(
            tableName = lazyQuery.tableName,
            query = {
                val columns = lazyQuery.query.columns.filter { it.element.text != columnName.name }
                lazyQuery.query.copy(columns = columns)
            },
        )
}
