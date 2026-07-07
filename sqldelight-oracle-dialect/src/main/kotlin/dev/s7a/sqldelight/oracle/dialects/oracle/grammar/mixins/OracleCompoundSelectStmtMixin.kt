package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import app.cash.sqldelight.dialect.api.ExposableType
import app.cash.sqldelight.dialect.api.IntermediateType
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.alecstrong.sql.psi.core.psi.SqlWithClauseAuxiliaryStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlCompoundSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType

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
            parentWithClause.oracleTablesExposed(
                excludeIndex = myIndex,
                includeExcludedWithColumnAliases = true,
            )
    }

    private fun SqlWithClause.oracleTablesExposed(
        excludeIndex: Int? = null,
        includeExcludedWithColumnAliases: Boolean = false,
    ): List<LazyQuery> {
        val cteQueries =
            cteTableNameList.zip(withClauseAuxiliaryStmtList).mapNotNull { (name, withClauseAuxiliaryStmt) ->
                PsiTreeUtil.findChildOfAnyType(withClauseAuxiliaryStmt, QueryElement::class.java)?.let {
                    OracleCteQuery(name.tableName, name.columnAliasList, withClauseAuxiliaryStmt.parent, it)
                }
            }

        return cteQueries.withIndex().mapNotNull { (index, cteQuery) ->
            if (index == excludeIndex && (!includeExcludedWithColumnAliases || cteQuery.columnAliases.isEmpty())) {
                return@mapNotNull null
            }
            LazyQuery(cteQuery.tableName) {
                QueryResult(
                    table = cteQuery.tableName,
                    columns =
                        cteQuery.columnAliases.map(::QueryColumn).ifEmpty {
                            cteQuery.queryElement.queryExposed().flatMap(QueryResult::columns)
                        } + cteQuery.withClauseItem.oracleSearchCycleQueryColumns(),
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

private fun PsiElement.oracleSearchCycleQueryColumns(): List<QueryColumn> =
    text.oracleSearchColumnNames().map { columnName ->
        QueryColumn(OracleSearchCycleColumnElement(this, columnName, IntermediateType(OracleType.LONG_NUMBER)))
    } +
        text.oracleCycleColumnNames().map { columnName ->
            QueryColumn(OracleSearchCycleColumnElement(this, columnName, IntermediateType(OracleType.TEXT)))
        }

private fun String.oracleSearchColumnNames(): List<String> = oracleColumnNamesAfterSet("""\bSEARCH\b[\s\S]*?\bSET\s+""")

private fun String.oracleCycleColumnNames(): List<String> = oracleColumnNamesAfterSet("""\bCYCLE\b[\s\S]*?\bSET\s+""")

private fun String.oracleColumnNamesAfterSet(prefixPattern: String): List<String> =
    Regex("$prefixPattern($ORACLE_IDENTIFIER_PATTERN)", RegexOption.IGNORE_CASE)
        .findAll(this)
        .map { it.groupValues[1].trim('"') }
        .toList()

private const val ORACLE_IDENTIFIER_PATTERN = """"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*"""

private class OracleSearchCycleColumnElement(
    private val anchor: PsiElement,
    private val columnName: String,
    private val columnType: IntermediateType,
) : LightElement(anchor.manager, anchor.language),
    AliasElement,
    ExposableType {
    override fun type(): IntermediateType = columnType.copy(name = columnName)

    override fun annotate(annotationHolder: SqlAnnotationHolder) = Unit

    override fun source(): PsiElement = anchor

    override fun getName(): String = columnName

    override fun setName(name: String): PsiElement = this

    override fun getText(): String = columnName

    override fun getContainingFile(): PsiFile = anchor.containingFile

    override fun getParent(): PsiElement = anchor

    override fun getNameIdentifier(): PsiElement? = null

    override fun toString(): String = "Oracle SEARCH/CYCLE column: $columnName"
}
