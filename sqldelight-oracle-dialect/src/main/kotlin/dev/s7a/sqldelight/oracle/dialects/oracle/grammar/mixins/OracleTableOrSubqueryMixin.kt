package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
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
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateSchemaQualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateUnqualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableColumnsClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleJsonTableReference
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleLocalSynonymTarget
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleRowPatternClause
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleSynonymTarget
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
                    oracleSynonymTargetResult(tableNameElement)
                        ?: tableAvailable(tableNameElement, tableNameElement.name)
                if (result.isEmpty()) {
                    return@lazy emptyList()
                }
                val containersColumns = oracleContainersSynthesizedColumns()
                tableAlias?.let { alias ->
                    return@lazy listOf(
                        QueryResult(
                            alias,
                            result.flatMap { it.columns },
                            result.flatMap { it.synthesizedColumns } +
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
        if (child == compoundSelectStmt && oracleCanReferenceLeftQuerySources()) {
            return super.queryAvailable(child) + oracleLateralLeftQueryExposed()
        }
        return super.queryAvailable(child)
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
                ?.let { columns -> QueryResult(jsonTable.tableAlias() ?: tableAlias(), columns) }
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleXmltableReference::class.java)?.let { xmltable ->
            return xmltable.oracleXmltableColumnsClause
                ?.oracleXmltableColumnList
                ?.mapNotNull { column -> column.oracleColumnAliasQueryColumn() }
                ?.ifEmpty { null }
                ?.let { columns -> QueryResult(xmltable.tableAlias() ?: tableAlias(), columns) }
        }

        PsiTreeUtil.findChildOfType(this, OracleOracleValuesTableReference::class.java)?.let { valuesTable ->
            return valuesTable
                .oracleColumnAliasQueryColumns()
                .ifEmpty { null }
                ?.let { columns -> QueryResult(valuesTable.tableAlias() ?: tableAlias(), columns) }
        }

        oracleCollectionTableColumnResult()?.let {
            return it
        }

        oracleInlineExternalColumns().ifEmpty { null }?.let { columns ->
            return QueryResult(
                table = tableAlias,
                columns = emptyList(),
                synthesizedColumns = columns.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
            )
        }

        oraclePivotColumns().ifEmpty { null }?.let { columns ->
            return QueryResult(
                table = tableAlias,
                columns = emptyList(),
                synthesizedColumns = columns.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
            )
        }

        oracleUnpivotColumns().ifEmpty { null }?.let { columns ->
            return QueryResult(
                table = tableAlias,
                columns = emptyList(),
                synthesizedColumns = columns.map { name -> SynthesizedColumn(tableAlias ?: this, listOf(name)) },
            )
        }

        return oracleRowPatternResult()
    }

    private fun oracleCollectionTableColumnResult(): QueryResult? {
        val body = text.trimStart()
        if (!body.startsWithOracleCollectionTableReference()) {
            return null
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

    private fun oracleSynonymTargetResult(tableNameElement: SqlTableName): Collection<QueryResult>? {
        val synonym =
            tableNameElement.reference
                ?.resolve()
                ?.parent
                ?.oracleQuerySourceSynonym()
                ?: return null
        return synonym.oracleSynonymTargetResult(tableNameElement, mutableSetOf())
    }

    private fun PsiElement.oracleSynonymTargetResult(
        tableNameElement: SqlTableName,
        seenTargetNames: MutableSet<String>,
    ): Collection<QueryResult> {
        val target =
            PsiTreeUtil.findChildOfType(this, OracleOracleSynonymTarget::class.java)
                ?: PsiTreeUtil.findChildOfType(this, OracleOracleLocalSynonymTarget::class.java)
                ?: return emptyList()
        val targetName = target.text.substringBefore("@").substringAfterLast(".")
        if (!seenTargetNames.add(targetName.lowercase())) return emptyList()

        val targetResult = tableAvailable(target, targetName)
        val targetSynonym =
            targetResult.firstNotNullOfOrNull { result ->
                (result.table as? PsiElement)?.parent?.oracleQuerySourceSynonym()
            }
        if (targetSynonym != null && targetResult.all { it.columns.isEmpty() && it.synthesizedColumns.isEmpty() }) {
            return targetSynonym.oracleSynonymTargetResult(tableNameElement, seenTargetNames)
        }

        return targetResult.map { result ->
            result.copy(
                table = tableNameElement,
                synthesizedColumns =
                    result.synthesizedColumns.map { column ->
                        column.copy(table = tableNameElement)
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

    private fun PsiElement.tableAlias(): SqlTableAlias? = PsiTreeUtil.findChildOfType(this, SqlTableAlias::class.java)

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

    private fun OracleOracleJsonTableColumnsClause.queryColumns(): List<QueryColumn> = oracleColumnAliasQueryColumns()

    private fun oracleContainersSynthesizedColumns(): List<String> =
        if (text.trimStart().startsWith("CONTAINERS", ignoreCase = true)) {
            listOf("CON_ID")
        } else {
            emptyList()
        }

    private fun oracleRowPatternResult(): QueryResult? {
        val rowPatternClause = PsiTreeUtil.findChildOfType(this, OracleOracleRowPatternClause::class.java) ?: return null
        val sourceColumns =
            if (rowPatternClause.text.contains("ALL ROWS PER MATCH", ignoreCase = true)) {
                tableName?.let { tableNameElement ->
                    tableAvailable(tableNameElement, tableNameElement.name).flatMap { it.columns }
                } ?: emptyList()
            } else {
                emptyList()
            }
        val measureColumns =
            rowPatternClause
                .oracleRowPatternMeasuresClause
                ?.oracleRowPatternMeasureColumnList
                ?.mapNotNull { column -> column.oracleColumnAliasQueryColumn() }
                ?: emptyList()
        return (sourceColumns + measureColumns)
            .ifEmpty { null }
            ?.let { columns -> QueryResult(tableAlias, columns) }
    }

    private fun oraclePivotColumns(): List<String> {
        val pivotBody = text.oracleParenthesizedBodyAfter("PIVOT") ?: return emptyList()
        val forOffset = pivotBody.indexOfKeyword("FOR")
        val inOffset = pivotBody.indexOfKeyword("IN", startIndex = forOffset?.let { it + "FOR".length } ?: 0) ?: return emptyList()
        val aggregateAliases = pivotBody.substring(0, inOffset).oracleAliasesAfterAs()
        val pivotInBody = pivotBody.oracleParenthesizedBodyAt(pivotBody.indexOf('(', startIndex = inOffset))
        val pivotAliases = pivotInBody.oracleAliasesAfterAs().ifEmpty { pivotInBody.oraclePivotImplicitValueNames() }
        if (pivotAliases.isEmpty()) return emptyList()

        return if (aggregateAliases.isEmpty()) {
            pivotAliases
        } else {
            pivotAliases.flatMap { pivotAlias -> aggregateAliases.map { aggregateAlias -> "${pivotAlias}_$aggregateAlias" } }
        }
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
}

private fun String.startsWithOracleCollectionTableReference(): Boolean =
    startsWithOracleKeywordCall("TABLE") || startsWithOracleKeywordCall("THE")

private fun String.startsWithOracleKeywordCall(keyword: String): Boolean {
    if (!startsWith(keyword, ignoreCase = true)) return false
    return drop(keyword.length).trimStart().startsWith("(")
}

private fun String.oracleParenthesizedBodyAfter(keyword: String): String? {
    val keywordOffset = indexOfKeyword(keyword) ?: return null
    val openOffset = indexOf('(', startIndex = keywordOffset + keyword.length).takeIf { it != -1 } ?: return null
    return oracleParenthesizedBodyAt(openOffset)
}

private fun String.oracleParenthesizedBodyAt(openOffset: Int): String {
    if (openOffset !in indices || this[openOffset] != '(') return ""
    var depth = 0
    for (index in openOffset until length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
                if (depth == 0) return substring(openOffset + 1, index)
            }
        }
    }
    return ""
}

private fun String.indexOfKeyword(
    keyword: String,
    startIndex: Int = 0,
): Int? {
    var index = startIndex
    while (index < length) {
        index = indexOf(keyword, startIndex = index, ignoreCase = true)
        if (index == -1) return null
        if (isOracleIdentifierBoundary(index - 1) && isOracleIdentifierBoundary(index + keyword.length)) return index
        index += keyword.length
    }
    return null
}

private fun String.substringBeforeKeyword(keyword: String): String =
    indexOfKeyword(keyword)?.let { keywordOffset -> substring(0, keywordOffset) } ?: this

private fun String.oracleAliasesAfterAs(): List<String> =
    Regex("""(?i)\bAS\s+("[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)""")
        .findAll(this)
        .map { match -> match.groupValues[1].trimOracleIdentifier() }
        .toList()

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

private fun String.oracleFirstName(): String? =
    Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
        .find(this)
        ?.value
        ?.trimOracleIdentifier()

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

private fun String.oracleTopLevelCommaParts(): List<String> {
    val parts = mutableListOf<String>()
    var start = 0
    var depth = 0
    var index = 0
    var inString = false
    while (index < length) {
        when {
            inString && this[index] == '\'' && index + 1 < length && this[index + 1] == '\'' -> {
                index++
            }

            this[index] == '\'' -> {
                inString = !inString
            }

            !inString && this[index] == '(' -> {
                depth++
            }

            !inString && this[index] == ')' -> {
                depth--
            }

            !inString && depth == 0 && this[index] == ',' -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
        index++
    }
    parts += substring(start)
    return parts
}

private fun String.trimOracleIdentifier(): String = trim().removeSurrounding("\"")

private fun String.trimOracleStringLiteral(): String = trim().removeSurrounding("'").replace("''", "'")

private fun String.isOracleIdentifierBoundary(index: Int): Boolean =
    index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_' && this[index] != '$' && this[index] != '#')
