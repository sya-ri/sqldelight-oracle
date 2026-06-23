package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleAlterTableModifyColumn

internal abstract class AlterTableModifyColumnMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableModifyColumn,
    AlterTableApplier {
    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        lazyQuery.withOracleColumns { columns ->
            val columnDef = oracleSingleColumnDefinition()
            columns.replaceOracleColumn(columnDef.columnName, columnDef.oracleQueryColumn())
        }

    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        super.annotate(annotationHolder)

        val columnDef = oracleSingleColumnDefinition()
        if (tablesAvailable(this)
                .oracleColumnsFor(alterStmt.tableName)
                .hasOracleColumn(columnDef.columnName)
                .not()
        ) {
            annotationHolder.createErrorAnnotation(
                element = columnDef.columnName,
                message = "No column found to modify with name ${columnDef.columnName.text}",
            )
        }
    }
}
