package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
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
        super.queryAvailable(child) + mergeAliasQueryAvailable(child, super.tablesAvailable(child))

    override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
        val base = super.tablesAvailable(child)
        return base + mergeAliasTablesAvailable(child, base)
    }

    private fun mergeAliasQueryAvailable(
        child: PsiElement,
        base: Collection<LazyQuery>,
    ): Collection<QueryResult> = mergeAliasTablesAvailable(child, base).map { it.query }

    private fun mergeAliasTablesAvailable(
        child: PsiElement,
        base: Collection<LazyQuery>,
    ): Collection<LazyQuery> {
        val subquerySources = PsiTreeUtil.findChildrenOfType(this, SqlCompoundSelectStmt::class.java)
        return PsiTreeUtil
            .findChildrenOfType(this, SqlTableAlias::class.java)
            .sortedBy { it.textOffset }
            .mapNotNull { alias ->
                mergeSubquerySourceBefore(alias, subquerySources)?.let { subquery ->
                    // A source subquery cannot reference its own alias, so skip it while resolving
                    // elements that live inside that subquery. This also breaks the resolution
                    // recursion that would otherwise occur when exposing the subquery columns.
                    if (PsiTreeUtil.isAncestor(subquery, child, false)) {
                        return@mapNotNull null
                    }
                    return@mapNotNull LazyQuery(alias) {
                        val exposed = subquery.queryExposed()
                        QueryResult(
                            table = alias,
                            columns = exposed.flatMap { it.columns },
                            synthesizedColumns =
                                exposed.flatMap { result ->
                                    result.synthesizedColumns.map { column -> column.copy(table = alias) }
                                },
                        )
                    }
                }

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

    /**
     * When a MERGE source is a subquery (`USING (SELECT ...) alias`), the alias directly follows the
     * closing parenthesis of a compound select statement. Returns that source subquery so the alias
     * can expose its columns. Scalar subqueries in the `ON`/`WHEN` clauses are not followed by a
     * table alias, so they are ignored.
     */
    private fun mergeSubquerySourceBefore(
        alias: SqlTableAlias,
        subquerySources: Collection<SqlCompoundSelectStmt>,
    ): SqlCompoundSelectStmt? {
        val mergeStart = textRange.startOffset
        val aliasStart = alias.textRange.startOffset - mergeStart
        if (aliasStart < 0 || aliasStart > text.length) return null
        val gap = Regex("""\s*\)\s*""")
        return subquerySources.firstOrNull { subquery ->
            val end = subquery.textRange.endOffset - mergeStart
            end in 0..aliasStart && gap.matches(text.substring(end, aliasStart))
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
