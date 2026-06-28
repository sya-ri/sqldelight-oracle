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
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateSchemaQualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleCreateUnqualifiedSynonymStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleLocalSynonymTarget
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleSynonymTarget

internal fun PsiElement.oracleColumnDefinitions(): List<SqlColumnDef> = PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnDef::class.java)

internal fun PsiElement.oracleSingleColumnDefinition(): SqlColumnDef = oracleColumnDefinitions().single()

internal fun PsiElement.oracleFirstColumnName(): SqlColumnName =
    PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnName::class.java).first()

internal fun PsiElement.oracleColumnNames(): List<SqlColumnName> = PsiTreeUtil.getChildrenOfTypeAsList(this, SqlColumnName::class.java)

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

internal fun QueryResult.withOracleSynthesizedColumns(
    table: PsiNamedElement,
    columnNames: List<String>,
): QueryResult =
    QueryResult(
        table = table,
        columns = columns,
        synthesizedColumns = columnNames.map { columnName -> SynthesizedColumn(table, listOf(columnName)) },
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
            ?.let { resolved ->
                resolved.oracleQuerySourceSynonym() ?: resolved.parent?.oracleQuerySourceSynonym()
            }
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

internal fun String.oracleCreateViewColumnNames(
    viewNameText: String,
    skipObjectViewSyntax: Boolean,
    skipConstraintColumns: Boolean,
): List<String> {
    val beforeQuery = substringBeforeOracleKeyword("AS") ?: return emptyList()
    val afterViewName = beforeQuery.substringAfter(viewNameText, missingDelimiterValue = "")
    if (afterViewName.isBlank()) return emptyList()

    val openIndex = afterViewName.indexOf('(')
    if (openIndex == -1) return emptyList()

    if (skipObjectViewSyntax) {
        val prefix = afterViewName.substring(0, openIndex)
        if (prefix.contains(Regex("""\b(AS|OF)\b|\bWITH\s+OBJECT\b""", RegexOption.IGNORE_CASE))) {
            return emptyList()
        }
    }

    return afterViewName
        .oracleParenthesizedBodyAt(openIndex)
        .oracleTopLevelCommaParts()
        .mapNotNull { part ->
            val trimmed = part.trim()
            if (skipConstraintColumns && trimmed.startsWith("CONSTRAINT", ignoreCase = true)) {
                null
            } else {
                ORACLE_IDENTIFIER_REGEX.find(trimmed)?.value?.trimOracleIdentifier()
            }
        }
}

internal fun String.oracleParenthesizedBodyAfter(keyword: String): String? {
    val keywordOffset = indexOfKeyword(keyword) ?: return null
    val openOffset = indexOf('(', startIndex = keywordOffset + keyword.length).takeIf { it != -1 } ?: return null
    return oracleParenthesizedBodyAt(openOffset)
}

internal fun String.oracleParenthesizedBodyAt(openOffset: Int): String {
    if (openOffset !in indices || this[openOffset] != '(') return ""
    var depth = 0
    var index = openOffset
    while (index < length) {
        when {
            startsOracleAlternativeQuotedString(index) -> {
                index = skipOracleAlternativeQuotedString(index)
                continue
            }

            this[index] == '\'' -> {
                index = skipOracleQuotedString(index)
                continue
            }

            this[index] == '(' -> {
                depth++
            }

            this[index] == ')' -> {
                depth--
                if (depth == 0) return substring(openOffset + 1, index)
            }
        }
        index++
    }
    return ""
}

internal fun String.oracleTopLevelCommaParts(): List<String> {
    val parts = mutableListOf<String>()
    var start = 0
    var depth = 0
    var index = 0
    while (index < length) {
        when {
            startsOracleAlternativeQuotedString(index) -> {
                index = skipOracleAlternativeQuotedString(index)
                continue
            }

            this[index] == '\'' -> {
                index = skipOracleQuotedString(index)
                continue
            }

            this[index] == '(' -> {
                depth++
            }

            this[index] == ')' -> {
                depth--
            }

            depth == 0 && this[index] == ',' -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
        index++
    }
    parts += substring(start)
    return parts
}

internal fun String.indexOfKeyword(
    keyword: String,
    startIndex: Int = 0,
): Int? {
    var index = startIndex
    while (index < length) {
        when {
            startsOracleAlternativeQuotedString(index) -> {
                index = skipOracleAlternativeQuotedString(index)
                continue
            }

            this[index] == '\'' -> {
                index = skipOracleQuotedString(index)
                continue
            }

            this[index] == '"' -> {
                index = skipOracleQuotedIdentifier(index)
                continue
            }

            regionMatches(index, keyword, 0, keyword.length, ignoreCase = true) &&
                isOracleIdentifierBoundary(index - 1) &&
                isOracleIdentifierBoundary(index + keyword.length) -> {
                return index
            }
        }
        index++
    }
    return null
}

internal fun String.substringBeforeOracleKeyword(keyword: String): String? =
    indexOfKeyword(keyword)?.let { keywordOffset -> substring(0, keywordOffset) }

internal fun String.oracleFirstName(): String? =
    ORACLE_IDENTIFIER_REGEX
        .find(this)
        ?.value
        ?.trimOracleIdentifier()

internal fun String.trimOracleIdentifier(): String = trim().removeSurrounding("\"")

internal val ORACLE_IDENTIFIER_REGEX: Regex = Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")

private fun String.startsOracleAlternativeQuotedString(start: Int): Boolean {
    val qIndex =
        when {
            getOrNull(start)?.equals('q', ignoreCase = true) == true -> start

            getOrNull(start)?.equals('n', ignoreCase = true) == true &&
                getOrNull(start + 1)?.equals('q', ignoreCase = true) == true -> start + 1

            else -> return false
        }
    return getOrNull(qIndex + 1) == '\'' && getOrNull(qIndex + 2) != null
}

private fun String.skipOracleAlternativeQuotedString(start: Int): Int {
    val qIndex = if (getOrNull(start)?.equals('q', ignoreCase = true) == true) start else start + 1
    val openDelimiter = getOrNull(qIndex + 2) ?: return start + 1
    val closeDelimiter =
        when (openDelimiter) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '<' -> '>'
            else -> openDelimiter
        }
    var index = qIndex + 3
    while (index < length - 1) {
        if (this[index] == closeDelimiter && this[index + 1] == '\'') return index + 2
        index++
    }
    return length
}

private fun String.skipOracleQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'') {
            if (index + 1 < length && this[index + 1] == '\'') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun String.skipOracleQuotedIdentifier(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '"') return index + 1
        index++
    }
    return length
}

private fun String.isOracleIdentifierBoundary(index: Int): Boolean =
    index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_' && this[index] != '$' && this[index] != '#')
