package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.alecstrong.sql.psi.core.psi.SqlWithClauseAuxiliaryStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlCompoundSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal abstract class OracleCompoundSelectStmtMixin(
    node: ASTNode,
) : SqlCompoundSelectStmtImpl(node) {
    override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
        val baseTables = (parent as SqlCompositeElement).tablesAvailable(this)
        val localTables =
            withClause
                ?.takeIf { child != it }
                ?.oracleTablesExposed()
                ?: emptyList()

        val withClauseAuxiliaryStmt = parent as? SqlWithClauseAuxiliaryStmt ?: return baseTables + localTables
        val parentWithClause = withClauseAuxiliaryStmt.parent as SqlWithClause
        val myIndex =
            parentWithClause.withClauseAuxiliaryStmtList
                .mapNotNull { PsiTreeUtil.findChildOfAnyType(it, QueryElement::class.java) }
                .indexOf(this)

        return baseTables +
            localTables +
            parentWithClause.oracleTablesExposed().filterIndexed { index, _ -> index != myIndex }
    }

    private fun SqlWithClause.oracleTablesExposed(): List<LazyQuery> {
        val cteQueries =
            cteTableNameList.zip(withClauseAuxiliaryStmtList).mapNotNull { (name, withClauseAuxiliaryStmt) ->
                PsiTreeUtil.findChildOfAnyType(withClauseAuxiliaryStmt, QueryElement::class.java)?.let {
                    OracleCteQuery(name.tableName, name.columnAliasList, withClauseAuxiliaryStmt.parent, it)
                }
            }

        return cteQueries.map { cteQuery ->
            LazyQuery(cteQuery.tableName) {
                QueryResult(
                    table = cteQuery.tableName,
                    columns =
                        cteQuery.columnAliases.map(::QueryColumn).ifEmpty {
                            cteQuery.queryElement.queryExposed().flatMap(QueryResult::columns)
                        },
                    synthesizedColumns =
                        cteQuery.withClauseItem.oracleSearchCycleColumnNames().map { columnName ->
                            SynthesizedColumn(cteQuery.tableName, listOf(columnName))
                        },
                )
            }
        }
    }
}

private data class OracleCteQuery(
    val tableName: SqlTableName,
    val columnAliases: List<SqlColumnAlias>,
    val withClauseItem: PsiElement,
    val queryElement: QueryElement,
)

private fun PsiElement.oracleSearchCycleColumnNames(): List<String> = text.oracleSearchColumnNames() + text.oracleCycleColumnNames()

private fun String.oracleSearchColumnNames(): List<String> = oracleColumnNamesAfterSet("""\bSEARCH\b[\s\S]*?\bSET\s+""")

private fun String.oracleCycleColumnNames(): List<String> = oracleColumnNamesAfterSet("""\bCYCLE\b[\s\S]*?\bSET\s+""")

private fun String.oracleColumnNamesAfterSet(prefixPattern: String): List<String> =
    Regex("$prefixPattern($ORACLE_IDENTIFIER_PATTERN)", RegexOption.IGNORE_CASE)
        .findAll(this)
        .map { it.groupValues[1].trim('"') }
        .toList()

private const val ORACLE_IDENTIFIER_PATTERN = """"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*"""
