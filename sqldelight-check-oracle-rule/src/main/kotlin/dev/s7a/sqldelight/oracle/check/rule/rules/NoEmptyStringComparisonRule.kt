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
    } ?: likePredicateBefore(literalRange.first)?.let { operatorRange ->
        operatorRange.first..literalRange.last
    } ?: likePredicateAfter(literalRange.last + 1)?.let { operatorRange ->
        literalRange.first..operatorRange.last
    } ?: inPredicateBefore(literalRange.first)?.let { operatorRange ->
        operatorRange.first..literalRange.last
    } ?: inPredicateAfter(literalRange.last + 1)?.let { operatorRange ->
        literalRange.first..operatorRange.last
    } ?: betweenPredicateBefore(literalRange.first)?.let { operatorRange ->
        operatorRange.first..literalRange.last
    } ?: betweenPredicateAfter(literalRange.last + 1)?.let { operatorRange ->
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

private fun String.likePredicateBefore(offset: Int): IntRange? {
    val likeRange = wordBefore(offset, likePredicates) ?: return null
    val notRange = wordBefore(likeRange.first, setOf("NOT"))
    return (notRange?.first ?: likeRange.first)..likeRange.last
}

private fun String.likePredicateAfter(offset: Int): IntRange? {
    val firstWord = wordAfter(offset) ?: return null
    val likeRange =
        when {
            firstWord.text.equals("NOT", ignoreCase = true) -> {
                val secondWord = wordAfter(firstWord.range.last + 1) ?: return null
                secondWord
                    .takeIf { word -> word.text.uppercase() in likePredicates }
                    ?.range
                    ?.let { range -> firstWord.range.first..range.last }
            }

            firstWord.text.uppercase() in likePredicates -> {
                firstWord.range
            }

            else -> {
                null
            }
        }
    return likeRange
}

private fun String.inPredicateBefore(offset: Int): IntRange? {
    val listStart = enclosingParenthesizedListStart(offset) ?: return null
    val inRange = wordBefore(listStart, setOf("IN")) ?: return null
    val notRange = wordBefore(inRange.first, setOf("NOT"))
    return (notRange?.first ?: inRange.first)..inRange.last
}

private fun String.inPredicateAfter(offset: Int): IntRange? {
    val firstWord = wordAfter(offset) ?: return null
    return when {
        firstWord.text.equals("NOT", ignoreCase = true) -> {
            val secondWord = wordAfter(firstWord.range.last + 1) ?: return null
            secondWord
                .takeIf { word -> word.text.equals("IN", ignoreCase = true) }
                ?.let { word -> firstWord.range.first..word.range.last }
        }

        firstWord.text.equals("IN", ignoreCase = true) -> {
            firstWord.range
        }

        else -> {
            null
        }
    }
}

private fun String.enclosingParenthesizedListStart(offset: Int): Int? {
    var index = offset - 1
    var depth = 0
    while (index >= 0) {
        when (this[index]) {
            ')' -> {
                depth++
            }

            '(' -> {
                if (depth == 0) return index
                depth--
            }
        }
        index--
    }
    return null
}

private fun String.betweenPredicateBefore(offset: Int): IntRange? {
    val betweenRange = betweenWordBefore(offset) ?: return null
    val notRange = wordBefore(betweenRange.first, setOf("NOT"))
    return (notRange?.first ?: betweenRange.first)..betweenRange.last
}

private fun String.betweenWordBefore(offset: Int): IntRange? {
    val word = wordBefore(offset) ?: return null
    return when {
        word.text.equals("BETWEEN", ignoreCase = true) -> {
            word.range
        }

        word.text.equals("AND", ignoreCase = true) -> {
            wordBefore(word.range.first, setOf("BETWEEN"))
        }

        else -> {
            null
        }
    }
}

private fun String.betweenPredicateAfter(offset: Int): IntRange? {
    val firstWord = wordAfter(offset) ?: return null
    return when {
        firstWord.text.equals("NOT", ignoreCase = true) -> {
            val secondWord = wordAfter(firstWord.range.last + 1) ?: return null
            secondWord
                .takeIf { word -> word.text.equals("BETWEEN", ignoreCase = true) }
                ?.let { word -> firstWord.range.first..word.range.last }
        }

        firstWord.text.equals("BETWEEN", ignoreCase = true) -> {
            firstWord.range
        }

        else -> {
            null
        }
    }
}

private data class EmptyStringWord(
    val text: String,
    val range: IntRange,
)

private fun String.wordBefore(
    offset: Int,
    candidates: Set<String>,
): IntRange? {
    val word = wordBefore(offset) ?: return null
    return word.range.takeIf { word.text.uppercase() in candidates }
}

private fun String.wordBefore(offset: Int): EmptyStringWord? {
    var end = previousNonWhitespace(offset - 1) ?: return null
    if (!this[end].isLetterOrDigit() && this[end] != '_') return null
    while (end >= 0 && (this[end].isLetterOrDigit() || this[end] == '_')) end--
    val start = end + 1
    val last = previousNonWhitespace(offset - 1) ?: return null
    return EmptyStringWord(substring(start, last + 1), start..last)
}

private fun String.wordAfter(offset: Int): EmptyStringWord? {
    val start = nextNonWhitespace(offset) ?: return null
    if (!this[start].isLetterOrDigit() && this[start] != '_') return null
    var end = start
    while (end < length && (this[end].isLetterOrDigit() || this[end] == '_')) end++
    return EmptyStringWord(substring(start, end), start until end)
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

private val likePredicates = setOf("LIKE", "LIKE2", "LIKE4", "LIKEC")

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
