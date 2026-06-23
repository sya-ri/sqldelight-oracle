package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleAlterTableModifyColumns

internal abstract class AlterTableModifyColumnsMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableModifyColumns,
    AlterTableApplier {
    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        lazyQuery.withOracleColumns { columns ->
            val columnDefinitions = oracleColumnDefinitions()
            columnDefinitions.fold(columns) { currentColumns, columnDef ->
                currentColumns.replaceOracleColumn(columnDef.columnName, columnDef.oracleQueryColumn())
            }
        }

    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        super.annotate(annotationHolder)

        val columnDefinitions = oracleColumnDefinitions()
        val availableColumns = tablesAvailable(this).oracleColumnsFor(alterStmt.tableName)

        columnDefinitions.forEach { columnDef ->
            if (!availableColumns.hasOracleColumn(columnDef.columnName)) {
                annotationHolder.createErrorAnnotation(
                    element = columnDef.columnName,
                    message = "No column found to modify with name ${columnDef.columnName.text}",
                )
            }
        }
    }
}
