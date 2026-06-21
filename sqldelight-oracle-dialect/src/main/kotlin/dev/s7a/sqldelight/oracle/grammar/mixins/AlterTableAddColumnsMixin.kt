package dev.s7a.sqldelight.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.grammar.psi.OracleAlterTableAddColumns

internal abstract class AlterTableAddColumnsMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableAddColumns,
    AlterTableApplier {
    private val columnDefinitions: List<SqlColumnDef>
        get() = children.filterIsInstance<SqlColumnDef>()

    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        LazyQuery(
            tableName = lazyQuery.tableName,
            query = {
                val columns = columnDefinitions.map { QueryElement.QueryColumn(it.columnName) }
                lazyQuery.query.copy(columns = lazyQuery.query.columns + columns)
            },
        )
}
