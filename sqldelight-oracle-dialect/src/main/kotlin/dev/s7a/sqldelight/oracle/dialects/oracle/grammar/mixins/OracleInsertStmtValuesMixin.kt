package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.impl.SqlInsertStmtValuesImpl
import com.intellij.lang.ASTNode

internal abstract class OracleInsertStmtValuesMixin(
    node: ASTNode,
) : SqlInsertStmtValuesImpl(node) {
    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        if (valuesExpressionList.isNotEmpty()) {
            super.annotate(annotationHolder)
        }
    }
}
