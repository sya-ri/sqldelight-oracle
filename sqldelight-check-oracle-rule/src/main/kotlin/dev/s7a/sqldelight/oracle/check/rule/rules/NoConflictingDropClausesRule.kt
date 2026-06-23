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
 * Reports conflicting Oracle DROP statement clauses.
 */
public class NoConflictingDropClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-drop-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .dropStatements()
            .flatMap { statement -> statement.conflictingDropClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle DROP clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class DropToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class DropOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class DropConflict(
    val group: String,
    val first: DropOccurrence,
    val second: DropOccurrence,
)

private fun String.dropStatements(): List<List<DropToken>> {
    val statements = mutableListOf<List<DropToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).dropTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<DropToken>.conflictingDropClauses(): List<DropConflict> {
    if (!getOrNull(0).dropHasText("DROP")) return emptyList()
    val firstByGroup = linkedMapOf<String, DropOccurrence>()
    return dropOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            DropConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<DropToken>.dropOccurrences(): List<DropOccurrence> =
    when {
        getOrNull(1).dropHasText("TYPE") && !getOrNull(2).dropHasText("BODY") -> {
            filter { token -> token.dropHasText("FORCE") || token.dropHasText("VALIDATE") }
                .map { token ->
                    DropOccurrence(
                        group = "TYPE FINALITY",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                    )
                }
        }

        getOrNull(1).dropHasText("TABLE") -> {
            filter { token -> token.dropHasText("PURGE") }
                .map { token ->
                    DropOccurrence(
                        group = "PURGE",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                    )
                }
        }

        else -> {
            emptyList()
        }
    }

private val dropTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.dropTokens(offset: Int): List<DropToken> =
    dropTokenPattern
        .findAll(this)
        .map { match ->
            DropToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun DropToken?.dropHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
