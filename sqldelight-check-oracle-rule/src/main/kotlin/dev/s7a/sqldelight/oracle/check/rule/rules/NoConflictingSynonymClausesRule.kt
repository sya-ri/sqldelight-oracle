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
 * Reports mutually exclusive or repeated Oracle CREATE SYNONYM clauses.
 */
public class NoConflictingSynonymClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-synonym-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .synonymStatements()
            .flatMap { statement -> statement.conflictingSynonymClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle CREATE SYNONYM clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SynonymToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SynonymOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SynonymConflict(
    val group: String,
    val first: SynonymOccurrence,
    val second: SynonymOccurrence,
)

private fun String.synonymStatements(): List<List<SynonymToken>> {
    val statements = mutableListOf<List<SynonymToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).synonymTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<SynonymToken>.conflictingSynonymClauses(): List<SynonymConflict> {
    if (!isCreateSynonymStatement()) return emptyList()
    val firstByGroup = linkedMapOf<String, SynonymOccurrence>()
    return synonymOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            SynonymConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<SynonymToken>.isCreateSynonymStatement(): Boolean =
    getOrNull(0).synonymHasText("CREATE") && take(8).any { token -> token.synonymHasText("SYNONYM") }

private fun List<SynonymToken>.synonymOccurrences(): List<SynonymOccurrence> =
    buildList {
        this@synonymOccurrences.forEachIndexed { index, token ->
            when {
                token.synonymHasText("EDITIONABLE") || token.synonymHasText("NONEDITIONABLE") -> {
                    add(
                        SynonymOccurrence(
                            group = "EDITIONABLE",
                            startOffset = token.startOffset,
                            endOffset = token.endOffset,
                        ),
                    )
                }

                token.synonymHasText("PUBLIC") -> {
                    add(
                        SynonymOccurrence(
                            group = "PUBLIC",
                            startOffset = token.startOffset,
                            endOffset = token.endOffset,
                        ),
                    )
                }

                token.synonymHasText("SHARING") && this@synonymOccurrences.getOrNull(index + 1).synonymHasText("=") -> {
                    add(
                        SynonymOccurrence(
                            group = "SHARING",
                            startOffset = token.startOffset,
                            endOffset =
                                this@synonymOccurrences.getOrNull(index + 2)?.endOffset
                                    ?: this@synonymOccurrences[index + 1].endOffset,
                        ),
                    )
                }
            }
        }
    }

private val synonymTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|=|;""")

private fun String.synonymTokens(offset: Int): List<SynonymToken> =
    synonymTokenPattern
        .findAll(this)
        .map { match ->
            SynonymToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun SynonymToken?.synonymHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
