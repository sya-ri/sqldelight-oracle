package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleAlterTableModifyColumn

internal abstract class AlterTableModifyColumnMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleAlterTableModifyColumn,
    AlterTableApplier {
    private val columnDef: SqlColumnDef
        get() = children.filterIsInstance<SqlColumnDef>().single()

    override fun applyTo(lazyQuery: LazyQuery): LazyQuery =
        LazyQuery(
            tableName = lazyQuery.tableName,
            query = {
                val columns =
                    lazyQuery.query.columns.map { queryColumn ->
                        val columnName = queryColumn.element as NamedElement
                        if (columnName.textMatches(columnDef.columnName)) {
                            QueryElement.QueryColumn(columnDef.columnName)
                        } else {
                            queryColumn
                        }
                    }
                lazyQuery.query.copy(columns = columns)
            },
        )

    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        super.annotate(annotationHolder)

        if (tablesAvailable(this)
                .filter { it.tableName.textMatches(alterStmt.tableName) }
                .flatMap { it.query.columns }
                .none { (it.element as? SqlColumnName)?.textMatches(columnDef.columnName) == true }
        ) {
            annotationHolder.createErrorAnnotation(
                element = columnDef.columnName,
                message = "No column found to modify with name ${columnDef.columnName.text}",
            )
        }
    }
}
