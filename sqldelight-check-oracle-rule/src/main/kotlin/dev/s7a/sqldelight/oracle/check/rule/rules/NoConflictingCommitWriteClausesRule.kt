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
 * Reports conflicting Oracle COMMIT WRITE clauses.
 */
public class NoConflictingCommitWriteClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-commit-write-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskCommitCommentsAndQuotedTextPreservingOffsets()
            .commitStatements()
            .flatMap { statement -> statement.conflictingCommitWriteClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle COMMIT WRITE clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class CommitToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class CommitOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class CommitConflict(
    val group: String,
    val first: CommitOccurrence,
    val second: CommitOccurrence,
)

private fun String.commitStatements(): List<List<CommitToken>> {
    val statements = mutableListOf<List<CommitToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).commitTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<CommitToken>.conflictingCommitWriteClauses(): List<CommitConflict> {
    if (!getOrNull(0).commitHasText("COMMIT")) return emptyList()
    val firstByGroup = linkedMapOf<String, CommitOccurrence>()
    return commitOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            CommitConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<CommitToken>.commitOccurrences(): List<CommitOccurrence> =
    mapNotNull { token ->
        val group =
            when {
                token.commitHasText("WRITE") -> "WRITE"
                token.commitHasText("WAIT") || token.commitHasText("NOWAIT") -> "WAIT"
                token.commitHasText("IMMEDIATE") || token.commitHasText("BATCH") -> "MODE"
                else -> null
            }
        group?.let {
            CommitOccurrence(
                group = it,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
            )
        }
    }

private val commitTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.commitTokens(offset: Int): List<CommitToken> =
    commitTokenPattern
        .findAll(this)
        .map { match ->
            CommitToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskCommitCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> commitMaskRange(chars, index, skipCommitLineComment(index))
                startsWith("/*", index) -> commitMaskRange(chars, index, skipCommitBlockComment(index))
                chars[index] == '\'' -> commitMaskRange(chars, index, skipCommitQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipCommitLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipCommitBlockComment(start: Int): Int = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipCommitQuotedString(start: Int): Int {
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

private fun commitMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun CommitToken?.commitHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
