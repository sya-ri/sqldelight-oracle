package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports duplicate static Oracle JSON_TABLE option groups.
 */
public class ValidJsonTableClausesRule : Rule {
    override val id: RuleId = RuleId("valid-json-table-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.maskSqlCommentsAndQuotedTextPreservingOffsets().jsonTableTokens()
        tokens.jsonTableConflicts().forEach { conflict ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use one Oracle JSON_TABLE ${conflict.group} clause in this scope.",
                    file = context.file,
                    range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class JsonTableToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class JsonTableOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class JsonTableConflict(
    val group: String,
    val first: JsonTableOccurrence,
    val second: JsonTableOccurrence,
)

private fun List<JsonTableToken>.jsonTableConflicts(): List<JsonTableConflict> =
    flatMapIndexed { index, token ->
        if (!token.jsonTableHasText("JSON_TABLE")) return@flatMapIndexed emptyList()
        val openIndex = indexOfNextJsonTableToken(index + 1, "(") ?: return@flatMapIndexed emptyList()
        val closeIndex = matchingJsonTableRightParenthesis(openIndex) ?: return@flatMapIndexed emptyList()
        subList(openIndex + 1, closeIndex).jsonTableBodyConflicts()
    }

private fun List<JsonTableToken>.jsonTableBodyConflicts(): List<JsonTableConflict> {
    val columnsIndex = indexOfTopLevelJsonTableColumns() ?: return jsonTableDuplicateConflicts()
    val columnsOpenIndex =
        indexOfNextJsonTableToken(columnsIndex + 1, "(")?.takeIf { openIndex ->
            openIndex < size && get(openIndex).jsonTableHasText("(")
        } ?: return subList(0, columnsIndex).jsonTableDuplicateConflicts()
    val columnsCloseIndex =
        matchingJsonTableRightParenthesis(columnsOpenIndex) ?: return subList(0, columnsIndex).jsonTableDuplicateConflicts()
    return subList(0, columnsIndex).jsonTableDuplicateConflicts() +
        subList(columnsOpenIndex + 1, columnsCloseIndex).jsonTableColumnConflicts()
}

private fun List<JsonTableToken>.jsonTableColumnConflicts(): List<JsonTableConflict> =
    jsonTableTopLevelRanges().flatMap { range ->
        val columnTokens = subList(range.first, range.last + 1)
        val nestedColumnsIndex = columnTokens.indexOfTopLevelJsonTableColumns()
        val currentScopeTokens =
            if (nestedColumnsIndex == null) {
                columnTokens
            } else {
                columnTokens.subList(0, nestedColumnsIndex)
            }
        currentScopeTokens.jsonTableDuplicateConflicts() + columnTokens.jsonTableNestedColumnConflicts()
    }

private fun List<JsonTableToken>.jsonTableNestedColumnConflicts(): List<JsonTableConflict> {
    val columnsIndex = indexOfTopLevelJsonTableColumns() ?: return emptyList()
    val columnsOpenIndex = indexOfNextJsonTableToken(columnsIndex + 1, "(") ?: return emptyList()
    val columnsCloseIndex = matchingJsonTableRightParenthesis(columnsOpenIndex) ?: return emptyList()
    return subList(columnsOpenIndex + 1, columnsCloseIndex).jsonTableColumnConflicts()
}

private fun List<JsonTableToken>.jsonTableDuplicateConflicts(): List<JsonTableConflict> {
    val firstByGroup = linkedMapOf<String, JsonTableOccurrence>()
    return mapIndexedNotNull { index, _ ->
        val occurrence = jsonTableOccurrence(index) ?: return@mapIndexedNotNull null
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            JsonTableConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<JsonTableToken>.jsonTableOccurrence(index: Int): JsonTableOccurrence? =
    when {
        getOrNull(index).jsonTableHasText("ON") && getOrNull(index + 1).jsonTableHasText("ERROR") -> {
            jsonTableOccurrence("ON ERROR", index, index + 1)
        }

        getOrNull(index).jsonTableHasText("ON") && getOrNull(index + 1).jsonTableHasText("EMPTY") -> {
            jsonTableOccurrence("ON EMPTY", index, index + 1)
        }

        getOrNull(index).jsonTableHasText("ON") && getOrNull(index + 1).jsonTableHasText("MISMATCH") -> {
            jsonTableOccurrence("ON MISMATCH", index, index + 1)
        }

        getOrNull(index).jsonTableHasText("WITH") && jsonTableWrapperEndIndex(index) != null -> {
            jsonTableOccurrence("wrapper", index, jsonTableWrapperEndIndex(index)!!)
        }

        getOrNull(index).jsonTableHasText("WITHOUT") && jsonTableWrapperEndIndex(index) != null -> {
            jsonTableOccurrence("wrapper", index, jsonTableWrapperEndIndex(index)!!)
        }

        else -> {
            null
        }
    }

private fun List<JsonTableToken>.jsonTableWrapperEndIndex(index: Int): Int? {
    var current = index + 1
    if (getOrNull(current).jsonTableHasText("UNCONDITIONAL") || getOrNull(current).jsonTableHasText("CONDITIONAL")) {
        current++
    }
    if (getOrNull(current).jsonTableHasText("ARRAY")) {
        current++
    }
    return current.takeIf { getOrNull(it).jsonTableHasText("WRAPPER") }
}

private fun List<JsonTableToken>.jsonTableOccurrence(
    group: String,
    startIndex: Int,
    endIndex: Int,
): JsonTableOccurrence =
    JsonTableOccurrence(
        group = group,
        startOffset = this[startIndex].startOffset,
        endOffset = this[endIndex].endOffset,
    )

private fun List<JsonTableToken>.jsonTableTopLevelRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var start = 0
    var depth = 0
    forEachIndexed { index, token ->
        when {
            token.jsonTableHasText("(") -> {
                depth++
            }

            token.jsonTableHasText(")") -> {
                depth--
            }

            depth == 0 && token.jsonTableHasText(",") -> {
                if (start < index) ranges += start until index
                start = index + 1
            }
        }
    }
    if (start < size) ranges += start until size
    return ranges
}

private fun List<JsonTableToken>.indexOfTopLevelJsonTableColumns(): Int? {
    var depth = 0
    forEachIndexed { index, token ->
        when {
            token.jsonTableHasText("(") -> depth++
            token.jsonTableHasText(")") -> depth--
            depth == 0 && token.jsonTableHasText("COLUMNS") -> return index
        }
    }
    return null
}

private fun List<JsonTableToken>.indexOfNextJsonTableToken(
    startIndex: Int,
    text: String,
): Int? = (startIndex until size).firstOrNull { index -> get(index).jsonTableHasText(text) }

private fun List<JsonTableToken>.matchingJsonTableRightParenthesis(openIndex: Int): Int? {
    var depth = 0
    for (index in openIndex until size) {
        when {
            get(index).jsonTableHasText("(") -> {
                depth++
            }

            get(index).jsonTableHasText(")") -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private val jsonTableTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|,""")

private fun String.jsonTableTokens(): List<JsonTableToken> =
    jsonTableTokenPattern
        .findAll(this)
        .map { match ->
            JsonTableToken(
                text = match.value,
                startOffset = match.range.first,
                endOffset = match.range.last + 1,
            )
        }.toList()

private fun JsonTableToken?.jsonTableHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
