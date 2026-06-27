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
 * Reports comparisons against `''`, because Oracle treats zero-length strings as null.
 */
public class NoEmptyStringComparisonRule : Rule {
    override val id: RuleId = RuleId("no-empty-string-comparison")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndNonEmptyQuotedTextPreservingOffsets()
        masked.emptyStringLiteralRanges().forEach { literalRange ->
            val range = masked.emptyStringComparisonRange(literalRange) ?: return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Avoid comparing with Oracle empty string literals; Oracle treats '' as NULL, so use IS NULL or IS NOT NULL.",
                    file = context.file,
                    range = content.rangeAtOffsets(range.first, range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.emptyStringComparisonRange(literalRange: IntRange): IntRange? =
    comparisonOperatorBefore(literalRange.first)?.let { operatorRange ->
        operatorRange.first..literalRange.last
    } ?: comparisonOperatorAfter(literalRange.last + 1)?.let { operatorRange ->
        literalRange.first..operatorRange.last
    }

private fun String.emptyStringLiteralRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var index = 0
    while (index < length) {
        index =
            when {
                startsSqlAlternativeQuotedString(index) -> {
                    val end = skipSqlAlternativeQuotedString(index)
                    val qIndex = if (getOrNull(index)?.equals('q', ignoreCase = true) == true) index else index + 1
                    if (qIndex + 3 == end - 2) ranges += index until end
                    end
                }

                getOrNull(index)?.equals('n', ignoreCase = true) == true &&
                    getOrNull(index + 1) == '\'' &&
                    getOrNull(index + 2) == '\'' -> {
                    ranges += index..index + 2
                    index + 3
                }

                getOrNull(index) == '\'' && getOrNull(index + 1) == '\'' -> {
                    ranges += index..index + 1
                    index + 2
                }

                else -> {
                    index + 1
                }
            }
    }
    return ranges
}

private fun String.comparisonOperatorBefore(offset: Int): IntRange? {
    val end = previousNonWhitespace(offset - 1) ?: return null
    return when {
        this[end] == '>' && getOrNull(end - 1) == '<' -> end - 1..end
        this[end] == '=' && getOrNull(end - 1) == '!' -> end - 1..end
        this[end] == '=' && getOrNull(end - 1) !in setOf('<', '>', '!') -> end..end
        else -> null
    }
}

private fun String.comparisonOperatorAfter(offset: Int): IntRange? {
    val start = nextNonWhitespace(offset) ?: return null
    return when {
        startsWith("<>", start) || startsWith("!=", start) -> start..start + 1
        this[start] == '=' && getOrNull(start + 1) != '>' -> start..start
        else -> null
    }
}

private fun String.previousNonWhitespace(offset: Int): Int? {
    var index = offset
    while (index >= 0) {
        if (!this[index].isWhitespace()) return index
        index--
    }
    return null
}

private fun String.nextNonWhitespace(offset: Int): Int? {
    var index = offset
    while (index < length) {
        if (!this[index].isWhitespace()) return index
        index++
    }
    return null
}

private fun String.maskSqlCommentsAndNonEmptyQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskLineComment(chars, index)
                startsWith("/*", index) -> maskBlockComment(chars, index)
                startsSqlAlternativeQuotedString(index) -> maskAlternativeQuotedTextUnlessEmpty(chars, index)
                chars[index] == '\'' -> maskQuotedTextUnlessEmpty(chars, index)
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.maskLineComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf('\n', startIndex = start).let { if (it == -1) length else it }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.maskBlockComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.maskQuotedTextUnlessEmpty(
    chars: CharArray,
    start: Int,
): Int {
    var index = start + 1
    while (index < length) {
        if (chars[index] == '\'') {
            if (index + 1 < length && chars[index + 1] == '\'') {
                index += 2
            } else {
                val end = index + 1
                if (end - start > 2) {
                    for (maskIndex in start until end) chars[maskIndex] = ' '
                }
                return end
            }
        } else {
            index++
        }
    }
    for (maskIndex in start until length) chars[maskIndex] = ' '
    return length
}

private fun String.maskAlternativeQuotedTextUnlessEmpty(
    chars: CharArray,
    start: Int,
): Int {
    val end = skipSqlAlternativeQuotedString(start)
    val qIndex = if (getOrNull(start)?.equals('q', ignoreCase = true) == true) start else start + 1
    if (end <= qIndex + 4 || qIndex + 3 == end - 2) return end
    for (maskIndex in start until end) chars[maskIndex] = ' '
    return end
}
