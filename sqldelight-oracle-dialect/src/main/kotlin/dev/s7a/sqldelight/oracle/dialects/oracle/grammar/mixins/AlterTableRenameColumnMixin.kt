package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
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
            val replace = columns.singleOrNull { it.matchesOracleNamedElement(sourceColumn) }
            columns.map { if (it == replace) it.copy(element = targetColumn) else it }
        }

    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        super.annotate(annotationHolder)

        val sourceColumn = oracleFirstColumnName()
        if (tablesAvailable(this)
                .filter { it.tableName.textMatches(alterStmt.tableName) }
                .flatMap { it.query.columns }
                .none { (it.element as? NamedElement)?.textMatches(sourceColumn) == true }
        ) {
            annotationHolder.createErrorAnnotation(
                element = sourceColumn,
                message = "No column found to modify with name ${sourceColumn.text}",
            )
        }
    }
}
