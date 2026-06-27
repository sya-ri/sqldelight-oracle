package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import app.cash.sqldelight.dialect.api.ExposableType
import app.cash.sqldelight.dialect.api.IntermediateType
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlDatabaseName
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlIndexName
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTableOrSubquery
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableColumn
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableColumnsClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableReference
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleRowPatternClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleValuesTableReference
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
                val result =
                    oracleSynonymTargetAvailable(tableNameElement) { target, targetName ->
                        tableAvailable(target, targetName)
                    }
                        ?: tableAvailable(tableNameElement, tableNameElement.name)
                if (result.isEmpty()) {
                    return@lazy emptyList()
                }
                val containersColumns = oracleContainersSynthesizedColumns()
                tableAlias?.let { alias ->
                    return@lazy listOf(
                        result.oracleQueryResultFor(
                            alias,
                            containersColumns.map { name -> SynthesizedColumn(alias, listOf(name)) },
                        ),
                    )
                }
                return@lazy result.map { query ->
                    query.copy(
                        synthesizedColumns =
                            query.synthesizedColumns +
                                containersColumns.map { name -> SynthesizedColumn(query.table ?: tableNameElement, listOf(name)) },
                    )
                }
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

    override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
        val queryAvailable =
            if (child == compoundSelectStmt && oracleCanReferenceLeftQuerySources()) {
                super.queryAvailable(child) + oracleLateralLeftQueryExposed()
            } else if (oracleTableFunctionCanReferenceLeftQuerySources(child)) {
                super.queryAvailable(child) + oracleLateralLeftQueryExposed()
            } else {
                super.queryAvailable(child)
            }
        val pivotAggregateAvailable = oraclePivotAggregateAvailable(child)
        if (pivotAggregateAvailable.isNotEmpty()) {
            return queryAvailable + pivotAggregateAvailable
        }
        val rowPatternClause = PsiTreeUtil.findChildOfType(this, OracleOracleRowPatternClause::class.java)
        if (rowPatternClause == null || !PsiTreeUtil.isAncestor(rowPatternClause, child, false)) {
            return queryAvailable
        }
        return queryAvailable + oracleRowPatternVariableResults(rowPatternClause)
    }

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
                ?.let { columns -> oracleColumnResultFor(jsonTable, columns) }
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleXmltableReference::class.java)?.let { xmltable ->
            return xmltable.oracleXmltableColumnsClause
                ?.oracleXmltableColumnList
                ?.mapNotNull { column -> column.oracleColumnAliasQueryColumn() }
                ?.ifEmpty { null }
                ?.let { columns -> oracleColumnResultFor(xmltable, columns) }
                ?: QueryResult(
                    table = xmltable.tableAlias() ?: tableAlias(),
                    columns =
                        listOf(
                            QueryColumn(
                                OracleGeneratedColumnElement(
                                    xmltable,
                                    "COLUMN_VALUE",
                                    IntermediateType(OracleType.TEXT),
                                ),
                            ),
                        ),
                )
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleValuesTableReference::class.java)?.let { valuesTable ->
            return valuesTable
                .oracleColumnAliasQueryColumns()
                .ifEmpty { null }
                ?.let { columns -> oracleColumnResultFor(valuesTable, columns) }
        }

        oracleCollectionTableColumnResult()?.let {
            return it
        }

        oracleInlineExternalColumns().ifEmpty { null }?.let { columns ->
            return oracleGeneratedSynthesizedColumnResult(columns)
        }

        oracleGraphTableColumns().ifEmpty { null }?.let { columns ->
            return oracleGeneratedSynthesizedColumnResult(columns)
        }

        oraclePivotColumnResults()?.let { result ->
            return QueryResult(
                table = tableAlias,
                columns = oraclePivotSourceColumns() + result.columns,
                synthesizedColumns = result.synthesizedColumnNames.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
            )
        }

        oracleUnpivotColumnResults()?.let { result ->
            return QueryResult(
                table = tableAlias,
                columns = oracleUnpivotSourceColumns() + result.columns,
                synthesizedColumns = result.synthesizedColumnNames.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
            )
        }

        return oracleRowPatternResult()
    }

    private fun oracleGeneratedSynthesizedColumnResult(
        columnNames: List<String>,
        columns: List<QueryColumn> = emptyList(),
    ): QueryResult =
        QueryResult(
            table = tableAlias,
            columns = columns,
            synthesizedColumns = columnNames.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
        )

    private fun oracleCollectionTableColumnResult(): QueryResult? {
        val body = text.trimStart()
        if (!body.startsWithOracleCollectionTableReference()) {
            return null
        }

        body.oracleCollectionTableType()?.let { type ->
            return QueryResult(
                table = tableAlias,
                columns = listOf(QueryColumn(OracleGeneratedColumnElement(this, "COLUMN_VALUE", IntermediateType(type)))),
            )
        }
        return QueryResult(
            table = tableAlias,
            columns = emptyList(),
            synthesizedColumns = listOf(SynthesizedColumn(tableAlias ?: this, listOf("COLUMN_VALUE"))),
        )
    }

    private fun oracleInlineExternalColumns(): List<String> {
        if (!text.trimStart().startsWith("EXTERNAL", ignoreCase = true)) return emptyList()
        val externalBody = text.oracleParenthesizedBodyAfter("EXTERNAL") ?: return emptyList()
        return externalBody
            .oracleParenthesizedBodyAt(externalBody.indexOf('('))
            .oracleTopLevelCommaParts()
            .mapNotNull { column -> column.oracleFirstName() }
    }

    private fun PsiElement.tableAlias(): SqlTableAlias? = PsiTreeUtil.findChildOfType(this, SqlTableAlias::class.java)

    private fun oracleColumnResultFor(
        source: PsiElement,
        columns: List<QueryColumn>,
    ): QueryResult = QueryResult(source.tableAlias() ?: tableAlias, columns)

    private fun oracleCanReferenceLeftQuerySources(): Boolean {
        if (text.trimStart().startsWith("LATERAL", ignoreCase = true)) return true

        val joinClause = parent as? SqlJoinClause ?: return false
        val index = joinClause.tableOrSubqueryList.indexOf(this)
        if (index <= 0) return false

        return joinClause.joinOperatorList
            .getOrNull(index - 1)
            ?.text
            ?.contains("APPLY", ignoreCase = true) == true
    }

    private fun oracleLateralLeftQueryExposed(): Collection<QueryResult> {
        val joinClause = parent as? SqlJoinClause ?: return emptyList()
        val siblings = joinClause.tableOrSubqueryList
        val index = siblings.indexOf(this)
        if (index <= 0) return emptyList()

        return siblings.take(index).flatMap { it.queryExposed() }
    }

    private fun oracleTableFunctionCanReferenceLeftQuerySources(child: PsiElement): Boolean {
        val jsonTable = PsiTreeUtil.findChildOfType(this, OracleOracleJsonTableReference::class.java)
        if (jsonTable != null && PsiTreeUtil.isAncestor(jsonTable, child, false)) return true

        val xmltable = PsiTreeUtil.findChildOfType(this, OracleOracleXmltableReference::class.java)
        return xmltable != null && PsiTreeUtil.isAncestor(xmltable, child, false)
    }

    private fun oraclePivotAggregateAvailable(child: PsiElement): Collection<QueryResult> {
        val tableNameElement = tableName ?: return emptyList()
        val pivotOffset = text.indexOfKeyword("PIVOT") ?: return emptyList()
        val pivotOpenOffset = text.indexOf('(', startIndex = pivotOffset + "PIVOT".length).takeIf { it != -1 } ?: return emptyList()
        val pivotBody = text.oracleParenthesizedBodyAt(pivotOpenOffset)
        val forOffset = pivotBody.indexOfKeyword("FOR") ?: return emptyList()
        val childOffset = child.textRange.startOffset - textRange.startOffset
        if (childOffset !in (pivotOpenOffset + 1) until (pivotOpenOffset + 1 + forOffset)) return emptyList()

        return oracleSynonymTargetAvailable(tableNameElement) { target, targetName ->
            tableAvailable(target, targetName)
        } ?: tableAvailable(tableNameElement, tableNameElement.name)
    }

    private fun OracleOracleJsonTableColumnsClause.queryColumns(): List<QueryColumn> =
        PsiTreeUtil
            .findChildrenOfType(this, OracleOracleJsonTableColumn::class.java)
            .mapNotNull { column -> column.oracleJsonTableQueryColumn() }

    private fun OracleOracleJsonTableColumn.oracleJsonTableQueryColumn(): QueryColumn? {
        val source =
            oracleJsonTableOrdinalityColumn
                ?: oracleJsonTableRegularColumn
                ?: oracleJsonTableExistsColumn
                ?: return null
        val alias = PsiTreeUtil.getChildOfType(source, SqlColumnAlias::class.java) ?: return null
        val type =
            when {
                oracleJsonTableOrdinalityColumn != null -> {
                    IntermediateType(OracleType.LONG_NUMBER)
                }

                oracleJsonTableRegularColumn != null -> {
                    source.text.oracleJsonTableColumnType(alias.text)?.let { typeName ->
                        IntermediateType(OracleType.fromSqlTypeName(typeName)).asNullable()
                    }
                }

                oracleJsonTableExistsColumn != null -> {
                    source.text.oracleJsonTableColumnType(alias.text)?.let { typeName ->
                        IntermediateType(OracleType.fromSqlTypeName(typeName)).asNullable()
                    } ?: IntermediateType(OracleType.TEXT).asNullable()
                }

                else -> {
                    null
                }
            } ?: return null
        return QueryColumn(OracleGeneratedColumnElement(source, alias.name, type))
    }

    private fun oracleContainersSynthesizedColumns(): List<String> =
        if (text.trimStart().startsWith("CONTAINERS", ignoreCase = true)) {
            listOf("CON_ID")
        } else {
            emptyList()
        }

    private fun oracleGraphTableColumns(): List<String> {
        if (!text.trimStart().startsWith("GRAPH_TABLE", ignoreCase = true)) return emptyList()
        return text
            .oracleParenthesizedBodyAfter("COLUMNS")
            ?.oracleAliasesAfterAs()
            ?: emptyList()
    }

    private fun oracleRowPatternResult(): QueryResult? {
        val rowPatternClause = PsiTreeUtil.findChildOfType(this, OracleOracleRowPatternClause::class.java) ?: return null
        val sourceColumns =
            if (rowPatternClause.text.contains("ALL ROWS PER MATCH", ignoreCase = true)) {
                oracleRowPatternSourceResults().flatMap { it.columns }
            } else {
                emptyList()
            }
        val measureColumns =
            rowPatternClause
                .oracleRowPatternMeasuresClause
                ?.oracleRowPatternMeasureColumnList
                ?.mapNotNull { column -> column.oracleRowPatternMeasureQueryColumn() }
                ?: emptyList()
        return (sourceColumns + measureColumns)
            .ifEmpty { null }
            ?.let { columns -> QueryResult(tableAlias, columns) }
    }

    private fun PsiElement.oracleRowPatternMeasureQueryColumn(): QueryColumn? {
        val alias = PsiTreeUtil.getChildOfType(this, SqlColumnAlias::class.java) ?: return null
        PsiTreeUtil.getChildOfType(this, SqlExpr::class.java)?.let { source ->
            return QueryColumn(OracleRowPatternMeasureElement(this, alias.name, source))
        }
        val type =
            when (text.withoutOracleTrailingAlias().trim().uppercase()) {
                "CLASSIFIER()" -> IntermediateType(OracleType.TEXT)
                "MATCH_NUMBER()", "ROW_NUMBER()" -> IntermediateType(OracleType.LONG_NUMBER)
                else -> return null
            }
        return QueryColumn(OracleRowPatternMeasureTypeElement(this, alias.name, type))
    }

    private fun oracleRowPatternVariableResults(rowPatternClause: OracleOracleRowPatternClause): Collection<QueryResult> {
        val sourceResults = oracleRowPatternSourceResults()
        val sourceColumns = sourceResults.flatMap { it.columns }
        val sourceSynthesizedColumns = sourceResults.flatMap { it.synthesizedColumns }
        if (sourceColumns.isEmpty() && sourceSynthesizedColumns.isEmpty()) return emptyList()

        return rowPatternClause
            .let(::oracleRowPatternVariables)
            .map { variable ->
                QueryResult(
                    table = OraclePatternVariableElement(rowPatternClause, variable),
                    columns = sourceColumns,
                    synthesizedColumns = sourceSynthesizedColumns,
                )
            }
    }

    private fun oracleRowPatternSourceResults(): Collection<QueryResult> =
        tableName?.let { tableNameElement ->
            oracleSynonymTargetAvailable(tableNameElement) { target, targetName ->
                tableAvailable(target, targetName)
            }
                ?: tableAvailable(tableNameElement, tableNameElement.name)
        } ?: emptyList()

    private data class OracleGeneratedColumns(
        val columns: List<QueryColumn>,
        val synthesizedColumnNames: List<String>,
    )

    private fun oraclePivotColumnResults(): OracleGeneratedColumns? {
        val pivotBody = text.oracleParenthesizedBodyAfter("PIVOT") ?: return null
        val forOffset = pivotBody.indexOfKeyword("FOR")
        val inOffset = pivotBody.indexOfKeyword("IN", startIndex = forOffset?.let { it + "FOR".length } ?: 0) ?: return null
        val aggregateParts = forOffset?.let { pivotBody.substring(0, it).oracleTopLevelCommaParts() } ?: emptyList()
        val aggregateAliases = aggregateParts.mapNotNull { part -> part.oracleTrailingAlias() }
        val aggregateTypes = aggregateParts.map { part -> oraclePivotAggregateType(part) }
        val pivotInBody = pivotBody.oracleParenthesizedBodyAt(pivotBody.indexOf('(', startIndex = inOffset))
        val pivotAliases = pivotInBody.oracleTopLevelTrailingAliases().ifEmpty { pivotInBody.oraclePivotImplicitValueNames() }
        if (pivotAliases.isEmpty()) return null

        val pivotColumns =
            if (aggregateAliases.isEmpty()) {
                pivotAliases.map { pivotAlias -> pivotAlias to aggregateTypes.firstOrNull() }
            } else {
                pivotAliases.flatMap { pivotAlias ->
                    aggregateAliases.mapIndexed { index, aggregateAlias ->
                        "${pivotAlias}_$aggregateAlias" to aggregateTypes.getOrNull(index)
                    }
                }
            }
        val columns = mutableListOf<QueryColumn>()
        val synthesizedColumnNames = mutableListOf<String>()
        pivotColumns.forEach { (name, type) ->
            if (type == null) {
                synthesizedColumnNames += name
            } else {
                columns += QueryColumn(OraclePivotColumnElement(this, name, type))
            }
        }
        return OracleGeneratedColumns(columns, synthesizedColumnNames)
    }

    private fun oraclePivotAggregateType(aggregatePart: String): IntermediateType? {
        val sourceText = aggregatePart.withoutOracleTrailingAlias()
        val functionName =
            Regex("""(?i)^\s*([A-Z_][A-Z0-9_$#]*)\s*\(""")
                .find(sourceText)
                ?.groupValues
                ?.get(1)
                ?: return null
        if (functionName.equals("COUNT", ignoreCase = true)) {
            return IntermediateType(OracleType.LONG_NUMBER)
        }

        val argumentName = sourceText.oracleParenthesizedBodyAt(sourceText.indexOf('(')).oracleNameList().singleOrNull() ?: return null
        val argumentType =
            oracleTableColumns()
                .firstNotNullOfOrNull { column ->
                    val columnDef = column.element.parent as? SqlColumnDef ?: return@firstNotNullOfOrNull null
                    if (columnDef.columnName.name.equals(argumentName, ignoreCase = true)) {
                        OracleType.fromSqlTypeName(columnDef.columnType.typeName.text)
                    } else {
                        null
                    }
                } ?: return OracleType.fromFunctionName(functionName)?.let { type -> IntermediateType(type).asNullable() }

        return OracleType
            .fromAggregateFunctionType(functionName, argumentType)
            ?.let { type -> IntermediateType(type).asNullable() }
            ?: when (functionName.trim().uppercase()) {
                "MIN", "MAX" -> IntermediateType(argumentType).asNullable()
                else -> OracleType.fromFunctionName(functionName)?.let { type -> IntermediateType(type).asNullable() }
            }
    }

    private class OraclePivotColumnElement(
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

        override fun toString(): String = "Oracle PIVOT column: $columnName"
    }

    private fun String.withoutOracleTrailingAlias(): String {
        val trimmed = trim()
        val match =
            Regex("""(?is)(?:\bAS\s+)?("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)\s*$""")
                .find(trimmed)
                ?: return trimmed
        val prefix = trimmed.substring(0, match.range.first).trim()
        return if (prefix.isEmpty()) {
            trimmed
        } else {
            prefix
        }
    }

    private fun oraclePivotSourceColumns(): List<QueryColumn> {
        val pivotBody = text.oracleParenthesizedBodyAfter("PIVOT") ?: return emptyList()
        val forOffset = pivotBody.indexOfKeyword("FOR") ?: return emptyList()
        val inOffset = pivotBody.indexOfKeyword("IN", startIndex = forOffset + "FOR".length) ?: return emptyList()
        val consumedColumns =
            (
                pivotBody.substring(0, forOffset).oracleNameList() +
                    pivotBody.substring(forOffset + "FOR".length, inOffset).oracleNameList()
            ).toSet()
        return oracleSourceColumnsExcept(consumedColumns)
    }

    private fun oracleUnpivotColumns(): List<String> {
        val unpivotBody = text.oracleParenthesizedBodyAfter("UNPIVOT") ?: return emptyList()
        val forOffset = unpivotBody.indexOfKeyword("FOR") ?: return emptyList()
        val measureColumns = unpivotBody.substring(0, forOffset).oracleNameList()
        val forColumns =
            unpivotBody
                .substring(forOffset + "FOR".length)
                .substringBeforeKeyword("IN")
                .oracleNameList()

        return (measureColumns + forColumns).distinct()
    }

    private fun oracleUnpivotColumnResults(): OracleGeneratedColumns? {
        val unpivotBody = text.oracleParenthesizedBodyAfter("UNPIVOT") ?: return null
        val forOffset = unpivotBody.indexOfKeyword("FOR") ?: return null
        val inOffset = unpivotBody.indexOfKeyword("IN", startIndex = forOffset + "FOR".length) ?: return null
        val measureColumns = unpivotBody.substring(0, forOffset).oracleNameList()
        val forColumns =
            unpivotBody
                .substring(forOffset + "FOR".length, inOffset)
                .oracleNameList()
        val sourceTypes = unpivotBody.substring(inOffset + "IN".length).oracleNameList().mapNotNull(::oracleColumnType)
        val measureType = sourceTypes.oracleUnpivotMeasureType()
        val columns = mutableListOf<QueryColumn>()
        val synthesizedColumnNames = mutableListOf<String>()

        measureColumns.forEach { name ->
            if (measureType == null) {
                synthesizedColumnNames += name
            } else {
                columns += QueryColumn(OraclePivotColumnElement(this, name, IntermediateType(measureType).asNullable()))
            }
        }
        forColumns.forEach { name ->
            columns += QueryColumn(OraclePivotColumnElement(this, name, IntermediateType(OracleType.TEXT)))
        }

        return OracleGeneratedColumns(columns, synthesizedColumnNames)
    }

    private fun oracleColumnType(columnName: String): OracleType? =
        oracleTableColumns().firstNotNullOfOrNull { column ->
            val columnDef = column.element.parent as? SqlColumnDef ?: return@firstNotNullOfOrNull null
            if (columnDef.columnName.name.equals(columnName, ignoreCase = true)) {
                OracleType.fromSqlTypeName(columnDef.columnType.typeName.text)
            } else {
                null
            }
        }

    private fun List<OracleType>.oracleUnpivotMeasureType(): OracleType? {
        if (isEmpty()) return null
        return when {
            any { it == OracleType.BINARY_DOUBLE } -> OracleType.BINARY_DOUBLE
            any { it == OracleType.BINARY_FLOAT } -> OracleType.BINARY_FLOAT
            any { it == OracleType.DECIMAL_NUMBER } -> OracleType.DECIMAL_NUMBER
            any { it == OracleType.LONG_NUMBER } -> OracleType.LONG_NUMBER
            any { it == OracleType.INTEGER_NUMBER } -> OracleType.INTEGER_NUMBER
            all { it == OracleType.TEXT } -> OracleType.TEXT
            all { it == OracleType.BINARY } -> OracleType.BINARY
            else -> first()
        }
    }

    private fun oracleUnpivotSourceColumns(): List<QueryColumn> {
        val unpivotBody = text.oracleParenthesizedBodyAfter("UNPIVOT") ?: return emptyList()
        val inOffset = unpivotBody.indexOfKeyword("IN") ?: return emptyList()
        return oracleSourceColumnsExcept(unpivotBody.substring(inOffset + "IN".length).oracleNameList().toSet())
    }

    private fun oracleSourceColumnsExcept(consumedColumns: Set<String>): List<QueryColumn> {
        return oracleTableColumns()
            .filterNot { column ->
                val name = (column.element as? NamedElement)?.name ?: return@filterNot false
                consumedColumns.any { consumed -> consumed.equals(name, ignoreCase = true) }
            }
    }

    private fun oracleTableColumns(): List<QueryColumn> {
        val tableNameElement = tableName ?: return emptyList()
        return tableAvailable(tableNameElement, tableNameElement.name)
            .flatMap { result -> result.columns }
    }
}

private fun String.startsWithOracleCollectionTableReference(): Boolean =
    startsWithOracleKeywordCall("TABLE") || startsWithOracleKeywordCall("THE")

private fun String.startsWithOracleKeywordCall(keyword: String): Boolean {
    if (!startsWith(keyword, ignoreCase = true)) return false
    return drop(keyword.length).trimStart().startsWith("(")
}

private fun String.oracleCollectionTableType(): OracleType? {
    val tableBody = oracleParenthesizedBodyAfter("TABLE") ?: return null
    val collectionName =
        Regex("""(?i)^\s*(?:[A-Z_][A-Z0-9_$#]*\s*\.\s*)*([A-Z_][A-Z0-9_$#]*)\s*\(""")
            .find(tableBody)
            ?.groupValues
            ?.get(1)
            ?: return null
    return OracleType.fromFunctionName(collectionName)
}

private class OraclePatternVariableElement(
    private val anchor: PsiElement,
    private val variableName: String,
) : LightElement(anchor.manager, anchor.language),
    PsiNamedElement {
    override fun getName(): String = variableName

    override fun setName(name: String): PsiElement = this

    override fun getText(): String = variableName

    override fun getContainingFile(): PsiFile = anchor.containingFile

    override fun getParent(): PsiElement = anchor

    override fun toString(): String = "Oracle pattern variable: $variableName"
}

private class OracleRowPatternMeasureElement(
    private val anchor: PsiElement,
    private val columnName: String,
    private val source: PsiElement,
) : LightElement(anchor.manager, anchor.language),
    AliasElement {
    override fun source(): PsiElement = source

    override fun getName(): String = columnName

    override fun setName(name: String): PsiElement = this

    override fun getText(): String = columnName

    override fun getContainingFile(): PsiFile = anchor.containingFile

    override fun getParent(): PsiElement = anchor

    override fun getNameIdentifier(): PsiElement? = null

    override fun toString(): String = "Oracle row pattern measure: $columnName"
}

private class OracleGeneratedColumnElement(
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

    override fun toString(): String = "Oracle generated column: $columnName"
}

private class OracleRowPatternMeasureTypeElement(
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

    override fun toString(): String = "Oracle row pattern measure: $columnName"
}

private fun String.substringBeforeKeyword(keyword: String): String =
    indexOfKeyword(keyword)?.let { keywordOffset -> substring(0, keywordOffset) } ?: this

private fun String.substringAfterKeyword(keyword: String): String =
    indexOfKeyword(keyword)?.let { keywordOffset -> substring(keywordOffset + keyword.length) } ?: ""

private fun String.oracleAliasesAfterAs(): List<String> =
    Regex("""(?i)\bAS\s+("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)""")
        .findAll(this)
        .map { match -> match.groupValues[1].trimOracleIdentifier() }
        .toList()

private fun String.oracleJsonTableColumnType(aliasText: String): String? {
    val afterAlias = trim().removePrefix(aliasText).trimStart()
    if (afterAlias.isBlank() || afterAlias.startsWith("FOR ", ignoreCase = true)) return null
    return afterAlias
        .substringBeforeKeyword("TRUNCATE")
        .substringBeforeKeyword("FORMAT")
        .substringBeforeKeyword("PATH")
        .substringBeforeKeyword("EXISTS")
        .substringBeforeKeyword("ERROR")
        .substringBeforeKeyword("NULL")
        .substringBeforeKeyword("DEFAULT")
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun String.oracleTopLevelTrailingAliases(): List<String> =
    oracleTopLevelCommaParts().mapNotNull { part -> part.oracleTrailingAlias() }

private fun String.oracleTrailingAlias(): String? {
    val trimmed = trim()
    if (
        trimmed.isEmpty() ||
        trimmed.equals("ANY", ignoreCase = true) ||
        trimmed.startsWith("SELECT", ignoreCase = true)
    ) {
        return null
    }

    val match =
        Regex("""(?is)(?:\bAS\s+)?("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)\s*$""")
            .find(trimmed)
            ?: return null
    val prefix = trimmed.substring(0, match.range.first).trim()
    if (prefix.isEmpty()) return null
    return match.groupValues[1].trimOracleIdentifier()
}

private fun oracleRowPatternVariables(rowPatternClause: OracleOracleRowPatternClause): List<String> {
    val patternVariables =
        rowPatternClause.text
            .oracleParenthesizedBodyAfter("PATTERN")
            ?.oracleNameList()
            ?.filterNot { it.equals("PERMUTE", ignoreCase = true) }
            ?: emptyList()
    val defineVariables =
        Regex("""(?is)(?:^|,)\s*("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)\s+AS\b""")
            .findAll(rowPatternClause.text.substringAfterKeyword("DEFINE"))
            .map { match -> match.groupValues[1].trimOracleIdentifier() }
    val subsetVariables =
        rowPatternClause.text
            .substringAfterKeyword("SUBSET")
            .substringBeforeKeyword("DEFINE")
            .oracleTopLevelCommaParts()
            .mapNotNull { part ->
                part
                    .substringBefore("=")
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.trimOracleIdentifier()
            }

    return (patternVariables.asSequence() + defineVariables + subsetVariables.asSequence())
        .distinctBy { it.lowercase() }
        .toList()
}

private fun String.oracleNameList(): List<String> {
    val body = trim().removeSurrounding("(", ")")
    return Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
        .findAll(body)
        .map { match -> match.value.trimOracleIdentifier() }
        .filterNot { name ->
            name.equals("INCLUDE", ignoreCase = true) || name.equals("EXCLUDE", ignoreCase = true) ||
                name.equals("NULLS", ignoreCase = true)
        }.toList()
}

private fun String.oraclePivotImplicitValueNames(): List<String> =
    oracleTopLevelCommaParts()
        .mapNotNull { part ->
            part
                .trim()
                .takeUnless { it.startsWith("(") && it.endsWith(")") }
                ?.trimOracleIdentifier()
                ?.trimOracleStringLiteral()
                ?.takeIf { name -> name.matches(Regex("""[A-Za-z_][A-Za-z0-9_$#]*""")) }
        }.toList()

private fun String.trimOracleStringLiteral(): String = trim().removeSurrounding("'").replace("''", "'")
