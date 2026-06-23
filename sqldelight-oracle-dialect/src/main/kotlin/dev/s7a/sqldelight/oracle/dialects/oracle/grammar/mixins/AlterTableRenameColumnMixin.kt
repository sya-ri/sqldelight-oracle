package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleAlterTableRenameColumn

internal abstract class AlterTableRenameColumnMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableRenameColumn,
    AlterTableApplier {
    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        lazyQuery.withOracleColumns { columns ->
            val sourceColumn = oracleFirstColumnName()
            val targetColumn = oracleSingleColumnAlias()
            columns.replaceOracleColumn(sourceColumn, targetColumn.oracleQueryColumn())
        }

    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        super.annotate(annotationHolder)

        val sourceColumn = oracleFirstColumnName()
        if (tablesAvailable(this)
                .oracleColumnsFor(alterStmt.tableName)
                .hasOracleColumn(sourceColumn)
                .not()
        ) {
            annotationHolder.createErrorAnnotation(
                element = sourceColumn,
                message = "No column found to modify with name ${sourceColumn.text}",
            )
        }
    }
}
