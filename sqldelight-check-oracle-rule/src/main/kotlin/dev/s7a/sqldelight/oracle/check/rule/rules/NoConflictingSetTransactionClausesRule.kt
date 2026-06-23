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
 * Reports conflicting Oracle SET TRANSACTION clauses.
 */
public class NoConflictingSetTransactionClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-set-transaction-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSetTransactionCommentsAndQuotedTextPreservingOffsets()
            .setTransactionStatements()
            .flatMap { statement -> statement.conflictingSetTransactionClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle SET TRANSACTION clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SetTransactionToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetTransactionOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetTransactionConflict(
    val group: String,
    val first: SetTransactionOccurrence,
    val second: SetTransactionOccurrence,
)

private fun String.setTransactionStatements(): List<List<SetTransactionToken>> {
    val statements = mutableListOf<List<SetTransactionToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).setTransactionTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<SetTransactionToken>.conflictingSetTransactionClauses(): List<SetTransactionConflict> {
    if (!getOrNull(0).setTransactionHasText("SET") || !getOrNull(1).setTransactionHasText("TRANSACTION")) return emptyList()
    val firstByGroup = linkedMapOf<String, SetTransactionOccurrence>()
    return setTransactionOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            SetTransactionConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<SetTransactionToken>.setTransactionOccurrences(): List<SetTransactionOccurrence> =
    buildList {
        this@setTransactionOccurrences.forEachIndexed { index, token ->
            when {
                token.setTransactionHasText("READ") &&
                    (
                        this@setTransactionOccurrences.getOrNull(index + 1).setTransactionHasText("ONLY") ||
                            this@setTransactionOccurrences.getOrNull(index + 1).setTransactionHasText("WRITE")
                    ) -> {
                    add(
                        SetTransactionOccurrence(
                            group = "READ MODE",
                            startOffset = token.startOffset,
                            endOffset = this@setTransactionOccurrences[index + 1].endOffset,
                        ),
                    )
                }

                token.setTransactionHasText("ISOLATION") &&
                    this@setTransactionOccurrences.getOrNull(index + 1).setTransactionHasText("LEVEL") -> {
                    add(
                        SetTransactionOccurrence(
                            group = "ISOLATION LEVEL",
                            startOffset = token.startOffset,
                            endOffset =
                                this@setTransactionOccurrences.getOrNull(index + 3)?.endOffset
                                    ?: this@setTransactionOccurrences[index + 1].endOffset,
                        ),
                    )
                }

                token.setTransactionHasText("ROLLBACK") &&
                    this@setTransactionOccurrences.getOrNull(index + 1).setTransactionHasText("SEGMENT") -> {
                    add(
                        SetTransactionOccurrence(
                            group = "ROLLBACK SEGMENT",
                            startOffset = token.startOffset,
                            endOffset =
                                this@setTransactionOccurrences.getOrNull(index + 2)?.endOffset
                                    ?: this@setTransactionOccurrences[index + 1].endOffset,
                        ),
                    )
                }
            }
        }
    }

private val setTransactionTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.setTransactionTokens(offset: Int): List<SetTransactionToken> =
    setTransactionTokenPattern
        .findAll(this)
        .map { match ->
            SetTransactionToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskSetTransactionCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> setTransactionMaskRange(chars, index, skipSetTransactionLineComment(index))
                startsWith("/*", index) -> setTransactionMaskRange(chars, index, skipSetTransactionBlockComment(index))
                chars[index] == '\'' -> setTransactionMaskRange(chars, index, skipSetTransactionQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipSetTransactionLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipSetTransactionBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipSetTransactionQuotedString(start: Int): Int {
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

private fun setTransactionMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun SetTransactionToken?.setTransactionHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
