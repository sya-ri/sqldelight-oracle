package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlDatabaseName
import com.alecstrong.sql.psi.core.psi.SqlIndexName
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTableOrSubquery
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableColumnsClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableReference
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleRowPatternClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleRowPatternMeasureColumn
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleValuesTableReference
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleXmltableColumn
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleXmltableReference
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleTableOrSubquery

internal abstract class OracleTableOrSubqueryMixin(
    node: ASTNode,
) : SqlCompositeElementImpl(node),
    OracleTableOrSubquery,
    SqlTableOrSubquery {
    private val queryExposed =
        ModifiableFileLazy lazy@{
            oracleGeneratedColumnResult()?.let {
                return@lazy listOf(it)
            }

            tableName?.let { tableNameElement ->
                val result = tableAvailable(tableNameElement, tableNameElement.name)
                if (result.isEmpty()) {
                    return@lazy emptyList<QueryResult>()
                }
                tableAlias?.let { alias ->
                    return@lazy listOf(
                        QueryResult(
                            alias,
                            result.flatMap { it.columns },
                            result.flatMap { it.synthesizedColumns },
                        ),
                    )
                }
                return@lazy result
            }

            compoundSelectStmt?.let {
                val result = it.queryExposed()
                tableAlias?.let { alias ->
                    return@lazy result.map { query -> query.copy(table = alias) }
                }
                return@lazy result
            }

            joinClause?.let {
                return@lazy it.queryExposed()
            }

            return@lazy tableOrSubqueryList.flatMap { it.queryExposed() }
        }

    override fun queryExposed() = queryExposed.forFile(containingFile)

    override fun getCompoundSelectStmt(): SqlCompoundSelectStmt? = PsiTreeUtil.getChildOfType(this, SqlCompoundSelectStmt::class.java)

    override fun getDatabaseName(): SqlDatabaseName? = PsiTreeUtil.getChildOfType(this, SqlDatabaseName::class.java)

    override fun getIndexName(): SqlIndexName? = PsiTreeUtil.getChildOfType(this, SqlIndexName::class.java)

    override fun getJoinClause(): SqlJoinClause? = PsiTreeUtil.getChildOfType(this, SqlJoinClause::class.java)

    override fun getTableAlias(): SqlTableAlias? = PsiTreeUtil.getChildOfType(this, SqlTableAlias::class.java)

    override fun getTableName(): SqlTableName? = PsiTreeUtil.getChildOfType(this, SqlTableName::class.java)

    override fun getTableOrSubqueryList(): List<SqlTableOrSubquery> =
        PsiTreeUtil.getChildrenOfTypeAsList(this, SqlTableOrSubquery::class.java)

    private fun oracleGeneratedColumnResult(): QueryResult? {
        PsiTreeUtil.findChildOfType(this, OracleOracleJsonTableReference::class.java)?.let { jsonTable ->
            return jsonTable.oracleJsonTableColumnsClause
                .queryColumns()
                .ifEmpty { null }
                ?.let { columns -> QueryResult(jsonTable.tableAlias() ?: tableAlias(), columns) }
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleXmltableReference::class.java)?.let { xmltable ->
            return xmltable.oracleXmltableColumnsClause
                ?.oracleXmltableColumnList
                ?.mapNotNull { it.columnAliasQueryColumn() }
                ?.ifEmpty { null }
                ?.let { columns -> QueryResult(xmltable.tableAlias() ?: tableAlias(), columns) }
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleValuesTableReference::class.java)?.let { valuesTable ->
            return PsiTreeUtil
                .findChildrenOfType(valuesTable, SqlColumnAlias::class.java)
                .map(::QueryColumn)
                .ifEmpty { null }
                ?.let { columns -> QueryResult(valuesTable.tableAlias() ?: tableAlias(), columns) }
        }

        return PsiTreeUtil
            .findChildOfType(this, OracleOracleRowPatternClause::class.java)
            ?.oracleRowPatternMeasuresClause
            ?.oracleRowPatternMeasureColumnList
            ?.mapNotNull { it.columnAliasQueryColumn() }
            ?.ifEmpty { null }
            ?.let { columns -> QueryResult(tableAlias, columns) }
    }

    private fun PsiElement.tableAlias(): SqlTableAlias? = PsiTreeUtil.findChildOfType(this, SqlTableAlias::class.java)

    private fun OracleOracleJsonTableColumnsClause.queryColumns(): List<QueryColumn> =
        PsiTreeUtil.findChildrenOfType(this, SqlColumnAlias::class.java).map(::QueryColumn)

    private fun OracleOracleXmltableColumn.columnAliasQueryColumn(): QueryColumn? =
        PsiTreeUtil.getChildOfType(this, SqlColumnAlias::class.java)?.let(::QueryColumn)

    private fun OracleOracleRowPatternMeasureColumn.columnAliasQueryColumn(): QueryColumn? =
        PsiTreeUtil.getChildOfType(this, SqlColumnAlias::class.java)?.let(::QueryColumn)
}
