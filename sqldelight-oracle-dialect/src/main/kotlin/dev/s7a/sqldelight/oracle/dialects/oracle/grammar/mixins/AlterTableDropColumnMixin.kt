package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleAlterTableDropColumn

internal abstract class AlterTableDropColumnMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableDropColumn,
    AlterTableApplier {
    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        lazyQuery.withOracleColumns { columns ->
            val columnName = oracleSingleColumnName()
            columns.filter { it.element.text != columnName.name }
        }
}
