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
 * Reports duplicate Oracle flashback query clauses on one table reference.
 */
public class NoConflictingFlashbackClauseRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-flashback-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .flashbackTokens()
            .conflictingFlashbackClauses()
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid duplicate Oracle flashback query clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class FlashbackToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class FlashbackOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class FlashbackConflict(
    val group: String,
    val first: FlashbackOccurrence,
    val second: FlashbackOccurrence,
)

private fun List<FlashbackToken>.conflictingFlashbackClauses(): List<FlashbackConflict> {
    val conflicts = mutableListOf<FlashbackConflict>()
    var index = 0
    while (index < size) {
        val occurrence = flashbackOccurrenceAt(index)
        if (occurrence == null) {
            index++
            continue
        }
        val boundary = indexOfNextFlashbackBoundary(index + 1)
        val duplicate =
            ((index + 1) until boundary)
                .mapNotNull { candidateIndex -> flashbackOccurrenceAt(candidateIndex) }
                .firstOrNull { candidate -> candidate.group == occurrence.group }
        if (duplicate != null) {
            conflicts +=
                FlashbackConflict(
                    group = occurrence.group,
                    first = occurrence,
                    second = duplicate,
                )
        }
        index = boundary
    }
    return conflicts
}

private fun List<FlashbackToken>.flashbackOccurrenceAt(index: Int): FlashbackOccurrence? =
    when {
        getOrNull(index).flashbackHasText("AS") && getOrNull(index + 1).flashbackHasText("OF") -> {
            FlashbackOccurrence(
                group = "AS OF",
                startOffset = get(index).startOffset,
                endOffset = get(index + 1).endOffset,
            )
        }

        getOrNull(index).flashbackHasText("VERSIONS") && getOrNull(index + 1).flashbackHasText("BETWEEN") -> {
            FlashbackOccurrence(
                group = "VERSIONS",
                startOffset = get(index).startOffset,
                endOffset = get(index + 1).endOffset,
            )
        }

        getOrNull(index).flashbackHasText("VERSIONS") && getOrNull(index + 1).flashbackHasText("PERIOD") -> {
            FlashbackOccurrence(
                group = "VERSIONS",
                startOffset = get(index).startOffset,
                endOffset = get(index + 1).endOffset,
            )
        }

        else -> {
            null
        }
    }

private fun List<FlashbackToken>.indexOfNextFlashbackBoundary(startIndex: Int): Int {
    var depth = 0
    for (index in startIndex until size) {
        val token = this[index]
        when {
            token.flashbackHasText("(") -> depth++
            token.flashbackHasText(")") -> if (depth == 0) return index else depth--
            depth == 0 && token.isFlashbackTableReferenceBoundary() -> return index
        }
    }
    return size
}

private fun FlashbackToken.isFlashbackTableReferenceBoundary(): Boolean =
    flashbackHasText(",") ||
        flashbackHasText(";") ||
        flashbackHasText("WHERE") ||
        flashbackHasText("GROUP") ||
        flashbackHasText("HAVING") ||
        flashbackHasText("ORDER") ||
        flashbackHasText("JOIN") ||
        flashbackHasText("INNER") ||
        flashbackHasText("LEFT") ||
        flashbackHasText("RIGHT") ||
        flashbackHasText("FULL") ||
        flashbackHasText("CROSS") ||
        flashbackHasText("OUTER") ||
        flashbackHasText("ON")

private val flashbackTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|,|;""")

private fun String.flashbackTokens(): List<FlashbackToken> =
    flashbackTokenPattern
        .findAll(this)
        .map { match ->
            FlashbackToken(
                text = match.value,
                startOffset = match.range.first,
                endOffset = match.range.last + 1,
            )
        }.toList()

private fun FlashbackToken?.flashbackHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
