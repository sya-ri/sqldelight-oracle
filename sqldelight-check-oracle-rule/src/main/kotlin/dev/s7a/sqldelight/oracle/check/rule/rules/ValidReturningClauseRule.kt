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
 * Reports invalid static Oracle RETURNING clause forms.
 */
public class ValidReturningClauseRule : Rule {
    override val id: RuleId = RuleId("valid-returning-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .returningStatements()
            .flatMap { statement -> statement.invalidReturningClauses() }
            .forEach { invalid ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a valid Oracle RETURNING clause: ${invalid.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(invalid.first.startOffset, invalid.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class ReturningToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class ReturningOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class InvalidReturningClause(
    val group: String,
    val first: ReturningOccurrence,
    val second: ReturningOccurrence,
)

private fun String.returningStatements(): List<List<ReturningToken>> {
    val statements = mutableListOf<List<ReturningToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).returningTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<ReturningToken>.invalidReturningClauses(): List<InvalidReturningClause> {
    if (!isReturningDmlStatement()) return emptyList()
    val firstReturningIndex = indexOfFirst { token -> token.isReturningKeyword() }
    if (firstReturningIndex == -1) return emptyList()
    return buildList {
        addAll(conflictingReturningOccurrences(returningKeywordOccurrences()))
        addAll(conflictingReturningOccurrences(intoOccurrencesAfter(firstReturningIndex)))
    }
}

private fun List<ReturningToken>.isReturningDmlStatement(): Boolean =
    getOrNull(0).let { token ->
        token.returningHasText("INSERT") ||
            token.returningHasText("UPDATE") ||
            token.returningHasText("DELETE") ||
            token.returningHasText("MERGE")
    }

private fun conflictingReturningOccurrences(occurrences: List<ReturningOccurrence>): List<InvalidReturningClause> {
    val firstByGroup = linkedMapOf<String, ReturningOccurrence>()
    return occurrences.mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            InvalidReturningClause(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<ReturningToken>.returningKeywordOccurrences(): List<ReturningOccurrence> =
    filter { token -> token.isReturningKeyword() }
        .map { token ->
            ReturningOccurrence(
                group = "RETURNING",
                startOffset = token.startOffset,
                endOffset = token.endOffset,
            )
        }

private fun List<ReturningToken>.intoOccurrencesAfter(returningIndex: Int): List<ReturningOccurrence> =
    subList(returningIndex + 1, endOfFirstReturningClause(returningIndex))
        .filter { token -> token.returningHasText("INTO") }
        .map { token ->
            ReturningOccurrence(
                group = "INTO",
                startOffset = token.startOffset,
                endOffset = token.endOffset,
            )
        }

private fun List<ReturningToken>.endOfFirstReturningClause(returningIndex: Int): Int =
    (returningIndex + 1 until size).firstOrNull { index -> get(index).isReturningKeyword() } ?: size

private val returningTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.returningTokens(offset: Int): List<ReturningToken> =
    returningTokenPattern
        .findAll(this)
        .map { match ->
            ReturningToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun ReturningToken?.returningHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private fun ReturningToken.isReturningKeyword(): Boolean = returningHasText("RETURN") || returningHasText("RETURNING")
