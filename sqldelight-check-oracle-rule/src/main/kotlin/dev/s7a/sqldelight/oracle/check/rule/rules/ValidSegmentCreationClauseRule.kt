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
 * Reports duplicate Oracle SEGMENT CREATION clauses in one table statement.
 */
public class ValidSegmentCreationClauseRule : Rule {
    override val id: RuleId = RuleId("valid-segment-creation-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSegmentCreationCommentsAndQuotedTextPreservingOffsets()
            .segmentCreationStatements()
            .flatMap { statement -> statement.conflictingSegmentCreationClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use one Oracle SEGMENT CREATION clause per table statement.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SegmentCreationToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SegmentCreationOccurrence(
    val startOffset: Int,
    val endOffset: Int,
)

private data class SegmentCreationConflict(
    val first: SegmentCreationOccurrence,
    val second: SegmentCreationOccurrence,
)

private fun String.segmentCreationStatements(): List<List<SegmentCreationToken>> =
    split(';')
        .runningFold(0 to emptyList<SegmentCreationToken>()) { (offset, _), statement ->
            val endOffset = offset + statement.length + 1
            endOffset to statement.segmentCreationTokens(offset)
        }.drop(1)
        .map { (_, tokens) -> tokens }

private fun List<SegmentCreationToken>.conflictingSegmentCreationClauses(): List<SegmentCreationConflict> {
    if (!startsWithCreateTable()) return emptyList()
    val first = segmentCreationOccurrences().firstOrNull() ?: return emptyList()
    return segmentCreationOccurrences().drop(1).map { occurrence ->
        SegmentCreationConflict(first = first, second = occurrence)
    }
}

private fun List<SegmentCreationToken>.startsWithCreateTable(): Boolean =
    getOrNull(0).segmentCreationHasText("CREATE") && take(6).any { token -> token.segmentCreationHasText("TABLE") }

private fun List<SegmentCreationToken>.segmentCreationOccurrences(): List<SegmentCreationOccurrence> =
    mapIndexedNotNull { index, token ->
        if (
            token.segmentCreationHasText("SEGMENT") &&
            getOrNull(index + 1).segmentCreationHasText("CREATION") &&
            (
                getOrNull(index + 2).segmentCreationHasText("IMMEDIATE") ||
                    getOrNull(index + 2).segmentCreationHasText("DEFERRED")
            )
        ) {
            SegmentCreationOccurrence(
                startOffset = token.startOffset,
                endOffset = get(index + 2).endOffset,
            )
        } else {
            null
        }
    }

private val segmentCreationTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*""")

private fun String.segmentCreationTokens(offset: Int): List<SegmentCreationToken> =
    segmentCreationTokenPattern
        .findAll(this)
        .map { match ->
            SegmentCreationToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskSegmentCreationCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> segmentCreationMaskRange(chars, index, skipSegmentCreationLineComment(index))
                startsWith("/*", index) -> segmentCreationMaskRange(chars, index, skipSegmentCreationBlockComment(index))
                chars[index] == '\'' -> segmentCreationMaskRange(chars, index, skipSegmentCreationQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipSegmentCreationLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipSegmentCreationBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipSegmentCreationQuotedString(start: Int): Int {
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

private fun segmentCreationMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun SegmentCreationToken?.segmentCreationHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
