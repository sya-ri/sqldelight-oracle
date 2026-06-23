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
 * Reports Oracle VALUES table aliases whose column list does not match the row arity.
 */
public class ValidValuesAliasColumnCountRule : Rule {
    override val id: RuleId = RuleId("valid-values-alias-column-count")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.maskValuesCommentsAndQuotedTextPreservingOffsets().valuesTables().forEach { table ->
            table.rowCounts.withIndex().firstOrNull { (_, rowCount) -> rowCount != table.expectedColumnCount }?.let { (index, rowCount) ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message =
                            "Oracle VALUES row ${index + 1} has $rowCount column(s), " +
                                "but the first row has ${table.expectedColumnCount}.",
                        file = context.file,
                        range = content.rangeAtOffsets(table.rowRanges[index].first, table.rowRanges[index].last + 1),
                        database = context.database,
                    ),
                )
            }

            if (table.aliasColumnCount != null && table.aliasColumnCount != table.expectedColumnCount) {
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message =
                            "Oracle VALUES alias declares ${table.aliasColumnCount} column(s), " +
                                "but VALUES rows have ${table.expectedColumnCount}.",
                        file = context.file,
                        range = content.rangeAtOffsets(table.aliasColumnRange!!.first, table.aliasColumnRange.last + 1),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private data class ValuesTable(
    val rowCounts: List<Int>,
    val rowRanges: List<IntRange>,
    val aliasColumnCount: Int?,
    val aliasColumnRange: IntRange?,
) {
    val expectedColumnCount: Int = rowCounts.first()
}

private fun String.valuesTables(): List<ValuesTable> {
    val tables = mutableListOf<ValuesTable>()
    var searchOffset = 0
    while (searchOffset < length) {
        val valuesOffset = indexOfValuesKeyword(searchOffset)
        if (valuesOffset == -1) break
        parseValuesTable(valuesOffset)?.let { table -> tables += table }
        searchOffset = valuesOffset + VALUES.length
    }
    return tables
}

private fun String.parseValuesTable(valuesOffset: Int): ValuesTable? {
    var index = skipWhitespace(valuesOffset + VALUES.length)
    val rowCounts = mutableListOf<Int>()
    val rowRanges = mutableListOf<IntRange>()
    while (index < length && this[index] == '(') {
        val rowEnd = matchingParenthesis(index) ?: return null
        rowCounts += countTopLevelItems(index + 1, rowEnd)
        rowRanges += index..rowEnd
        index = skipWhitespace(rowEnd + 1)
        if (index < length && this[index] == ',') {
            index = skipWhitespace(index + 1)
        } else {
            break
        }
    }
    if (rowCounts.isEmpty()) return null

    if (index < length && this[index] == ')') {
        index = skipWhitespace(index + 1)
    }

    val aliasStart = skipOptionalAs(skipWhitespace(index))
    val aliasEnd = identifierEnd(aliasStart)
    if (aliasEnd == aliasStart) return ValuesTable(rowCounts, rowRanges, aliasColumnCount = null, aliasColumnRange = null)

    val columnListStart = skipWhitespace(aliasEnd)
    if (columnListStart !in indices || this[columnListStart] != '(') {
        return ValuesTable(rowCounts, rowRanges, aliasColumnCount = null, aliasColumnRange = null)
    }

    val columnListEnd = matchingParenthesis(columnListStart) ?: return ValuesTable(rowCounts, rowRanges, null, null)
    return ValuesTable(
        rowCounts = rowCounts,
        rowRanges = rowRanges,
        aliasColumnCount = countTopLevelItems(columnListStart + 1, columnListEnd),
        aliasColumnRange = columnListStart..columnListEnd,
    )
}

private fun String.indexOfValuesKeyword(startIndex: Int): Int {
    var index = startIndex
    while (index < length) {
        index = indexOf(VALUES, startIndex = index, ignoreCase = true)
        if (index == -1) return -1
        if (isKeywordBoundary(index - 1) && isKeywordBoundary(index + VALUES.length)) return index
        index += VALUES.length
    }
    return -1
}

private fun String.isKeywordBoundary(index: Int): Boolean = index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_')

private fun String.skipWhitespace(index: Int): Int {
    var current = index
    while (current < length && this[current].isWhitespace()) current++
    return current
}

private fun String.skipOptionalAs(index: Int): Int =
    if (regionMatches(index, "AS", 0, "AS".length, ignoreCase = true) && isKeywordBoundary(index + "AS".length)) {
        skipWhitespace(index + "AS".length)
    } else {
        index
    }

private fun String.identifierEnd(index: Int): Int {
    if (index !in indices) return index
    if (this[index] == '"') {
        val end = indexOf('"', startIndex = index + 1)
        return if (end == -1) index else end + 1
    }

    var current = index
    while (current < length && (this[current].isLetterOrDigit() || this[current] == '_' || this[current] == '$' || this[current] == '#')) {
        current++
    }
    return current
}

private fun String.matchingParenthesis(openOffset: Int): Int? {
    var depth = 0
    for (index in openOffset until length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private fun String.countTopLevelItems(
    startOffset: Int,
    endOffset: Int,
): Int {
    var count = 1
    var depth = 0
    var hasContent = false
    for (index in startOffset until endOffset) {
        when (this[index]) {
            '(' -> {
                depth++
                hasContent = true
            }

            ')' -> {
                depth--
                hasContent = true
            }

            ',' -> {
                if (depth == 0) count++ else hasContent = true
            }

            else -> {
                if (!this[index].isWhitespace()) hasContent = true
            }
        }
    }
    return if (hasContent) count else 0
}

private fun String.maskValuesCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> {
                    val end = indexOf('\n', startIndex = index).let { if (it == -1) chars.size else it }
                    chars.fill(' ', index, end)
                    end
                }

                startsWith("/*", index) -> {
                    val end = indexOf("*/", startIndex = index + 2).let { if (it == -1) chars.size else it + 2 }
                    chars.fill(' ', index, end)
                    end
                }

                chars[index] == '\'' -> {
                    val end = skipValuesQuotedString(index)
                    chars.fill(' ', index, end)
                    end
                }

                else -> {
                    index + 1
                }
            }
    }
    return chars.concatToString()
}

private fun String.skipValuesQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'' && getOrNull(index + 1) == '\'') {
            index += 2
        } else if (this[index] == '\'') {
            return index + 1
        } else {
            index++
        }
    }
    return length
}

private const val VALUES = "VALUES"
