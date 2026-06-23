package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleMergeStmt

internal abstract class OracleMergeStmtMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleMergeStmt {
    override fun queryAvailable(child: PsiElement): Collection<QueryResult> =
        super.queryAvailable(child) + mergeAliasQueryAvailable(super.tablesAvailable(child))

    override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
        val base = super.tablesAvailable(child)
        return base + mergeAliasTablesAvailable(base)
    }

    private fun mergeAliasQueryAvailable(base: Collection<LazyQuery>): Collection<QueryResult> =
        mergeAliasTablesAvailable(base).map { it.query }

    private fun mergeAliasTablesAvailable(base: Collection<LazyQuery>): Collection<LazyQuery> {
        return PsiTreeUtil
            .findChildrenOfType(this, SqlTableAlias::class.java)
            .sortedBy { it.textOffset }
            .mapNotNull { alias ->
                val tableName = tableNameBefore(alias) ?: return@mapNotNull null
                val sourceTables = base.filter { it.tableName.name == tableName }
                if (sourceTables.isEmpty()) {
                    null
                } else {
                    LazyQuery(alias) {
                        QueryResult(
                            table = alias,
                            columns = sourceTables.flatMap { it.query.columns },
                            synthesizedColumns =
                                sourceTables.flatMap { table ->
                                    table.query.synthesizedColumns.map { column ->
                                        column.copy(table = alias)
                                    }
                                },
                        )
                    }
                }
            }
    }

    private fun tableNameBefore(alias: SqlTableAlias): String? {
        val aliasOffset = alias.textRange.startOffset - textRange.startOffset
        val beforeAlias = text.take(aliasOffset).trimEnd()
        val token =
            if (beforeAlias.endsWith(")")) {
                beforeAlias.substringAfterLast("(").substringBefore(")")
            } else {
                beforeAlias.split(Regex("\\s+")).lastOrNull()
            } ?: return null
        return token
            .substringBefore("@")
            .substringAfterLast(".")
            .trim('"')
            .ifEmpty { null }
    }
}
