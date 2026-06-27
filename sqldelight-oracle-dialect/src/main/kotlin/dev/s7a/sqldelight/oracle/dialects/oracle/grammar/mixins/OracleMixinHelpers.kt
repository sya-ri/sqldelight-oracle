package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlQualifiedTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateSchemaQualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateUnqualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleLocalSynonymTarget
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleSynonymTarget

internal fun PsiElement.oracleColumnDefinitions(): List<SqlColumnDef> = PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnDef::class.java)

internal fun PsiElement.oracleSingleColumnDefinition(): SqlColumnDef = oracleColumnDefinitions().single()

internal fun PsiElement.oracleFirstColumnName(): SqlColumnName =
    PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnName::class.java).first()

internal fun PsiElement.oracleSingleColumnName(): SqlColumnName =
    PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnName::class.java).single()

internal fun PsiElement.oracleSingleColumnAlias(): SqlColumnAlias =
    PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnAlias::class.java).single()

internal fun SqlColumnDef.oracleQueryColumn(): QueryColumn = QueryColumn(columnName)

internal fun SqlColumnAlias.oracleQueryColumn(): QueryColumn = QueryColumn(this)

internal fun LazyQuery.withOracleColumns(transform: (List<QueryColumn>) -> List<QueryColumn>): LazyQuery =
    LazyQuery(
        tableName = tableName,
        query = {
            query.copy(columns = transform(query.columns))
        },
    )

internal fun PsiElement.oracleColumnAliasQueryColumn(): QueryColumn? =
    PsiTreeUtil.getChildOfType(this, SqlColumnAlias::class.java)?.let(::QueryColumn)

internal fun PsiElement.oracleColumnAliasQueryColumns(): List<QueryColumn> =
    PsiTreeUtil.findChildrenOfType(this, SqlColumnAlias::class.java).map(::QueryColumn)

internal fun Collection<LazyQuery>.oracleColumnsFor(tableName: SqlTableName): List<QueryColumn> =
    filter { it.tableName.textMatches(tableName) }.flatMap { it.query.columns }

internal fun Collection<QueryColumn>.hasOracleColumn(columnName: NamedElement): Boolean =
    any { (it.element as? NamedElement)?.textMatches(columnName) == true }

internal fun Collection<QueryResult>.oracleQueryResultFor(
    alias: SqlTableAlias,
    synthesizedColumns: List<SynthesizedColumn> = emptyList(),
): QueryResult =
    QueryResult(
        alias,
        flatMap { it.columns },
        flatMap { it.synthesizedColumns } + synthesizedColumns,
    )

internal fun List<QueryColumn>.replaceOracleColumn(
    columnName: NamedElement,
    replacement: QueryColumn,
): List<QueryColumn> =
    map { queryColumn ->
        if (queryColumn.matchesOracleNamedElement(columnName)) {
            replacement
        } else {
            queryColumn
        }
    }

internal fun PsiElement.oracleAliasNamedTarget(base: Collection<QueryResult>): Collection<QueryResult> {
    val qualifiedTableName = PsiTreeUtil.getChildOfType(this, SqlQualifiedTableName::class.java)
    val tableAlias = qualifiedTableName?.let { PsiTreeUtil.getChildOfType(it, SqlTableAlias::class.java) }
    if (qualifiedTableName == null || tableAlias == null) return base

    return base.map { query ->
        if (query.table?.name == qualifiedTableName.tableName.name) {
            query.copy(table = tableAlias)
        } else {
            query
        }
    }
}

internal fun PsiElement.oracleDmlTargetAvailable(
    base: Collection<QueryResult>,
    child: PsiElement,
    tableResolver: (PsiElement, String) -> Collection<QueryResult>,
): Collection<QueryResult> {
    val qualifiedTableName = PsiTreeUtil.getChildOfType(this, SqlQualifiedTableName::class.java)
    val synonymTarget =
        qualifiedTableName?.let {
            oracleSynonymTargetAvailable(it.tableName, tableResolver = tableResolver)
        } ?: return oracleAliasNamedTarget(base)
    val targetName = qualifiedTableName.tableName.name
    return oracleAliasNamedTarget(base.filterNot { it.table?.name == targetName } + synonymTarget)
}

internal fun oracleSynonymTargetAvailable(
    tableName: SqlTableName,
    exposedTableName: SqlTableName = tableName,
    tableResolver: (PsiElement, String) -> Collection<QueryResult>,
): Collection<QueryResult>? {
    val synonym =
        tableName.reference
            ?.resolve()
            ?.parent
            ?.oracleQuerySourceSynonym()
            ?: return null
    return synonym.oracleSynonymTargetAvailable(exposedTableName, mutableSetOf(), tableResolver)
}

private fun PsiElement.oracleSynonymTargetAvailable(
    exposedTableName: SqlTableName,
    seenTargetNames: MutableSet<String>,
    tableResolver: (PsiElement, String) -> Collection<QueryResult>,
): Collection<QueryResult> {
    val target =
        PsiTreeUtil.findChildOfType(this, OracleOracleSynonymTarget::class.java)
            ?: PsiTreeUtil.findChildOfType(this, OracleOracleLocalSynonymTarget::class.java)
            ?: return emptyList()
    val targetName = target.text.substringBefore("@").substringAfterLast(".")
    if (!seenTargetNames.add(targetName.lowercase())) return emptyList()

    val targetResult = tableResolver(target, targetName)
    val targetSynonym =
        targetResult.firstNotNullOfOrNull { result ->
            (result.table as? PsiElement)?.parent?.oracleQuerySourceSynonym()
        }
    if (targetSynonym != null && targetResult.all { it.columns.isEmpty() && it.synthesizedColumns.isEmpty() }) {
        return targetSynonym.oracleSynonymTargetAvailable(exposedTableName, seenTargetNames, tableResolver)
    }

    return targetResult.map { result ->
        result.copy(
            table = exposedTableName,
            synthesizedColumns =
                result.synthesizedColumns.map { column ->
                    column.copy(table = exposedTableName)
                },
        )
    }
}

private fun PsiElement.oracleQuerySourceSynonym(): PsiElement? =
    when (this) {
        is OracleCreateSchemaQualifiedSynonymStmt -> this
        is OracleCreateUnqualifiedSynonymStmt -> this
        else -> null
    }

internal fun QueryColumn.matchesOracleNamedElement(element: NamedElement): Boolean = (this.element as NamedElement).textMatches(element)
