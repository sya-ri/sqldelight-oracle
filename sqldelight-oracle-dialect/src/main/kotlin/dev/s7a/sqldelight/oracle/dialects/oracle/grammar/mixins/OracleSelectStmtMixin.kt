package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.impl.SqlSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal abstract class OracleSelectStmtMixin(
    node: ASTNode,
) : SqlSelectStmtImpl(node) {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
        val base = super.queryAvailable(child)
        val windowOffset = text.indexOfKeyword("WINDOW") ?: return base
        val childOffset = child.textRange.startOffset - textRange.startOffset
        if (childOffset < windowOffset) {
            return base
        }

        val fromSources =
            PsiTreeUtil
                .getChildOfType(this, SqlJoinClause::class.java)
                ?.queryExposed()
                ?: emptyList()
        return base + fromSources
    }
}

private fun String.indexOfKeyword(keyword: String): Int? {
    var index = 0
    while (index < length) {
        index = indexOf(keyword, startIndex = index, ignoreCase = true)
        if (index == -1) return null
        if (isIdentifierBoundary(index - 1) && isIdentifierBoundary(index + keyword.length)) return index
        index += keyword.length
    }
    return null
}

private fun String.isIdentifierBoundary(index: Int): Boolean =
    index !in indices ||
        (!this[index].isLetterOrDigit() && this[index] != '_' && this[index] != '$' && this[index] != '#')
