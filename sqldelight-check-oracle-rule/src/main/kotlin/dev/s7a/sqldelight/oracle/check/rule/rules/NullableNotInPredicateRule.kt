package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlTokens
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports `NOT IN (subquery)` predicates whose subquery does not explicitly filter null values.
 */
public class NullableNotInPredicateRule : Rule {
    override val id: RuleId = RuleId("nullable-not-in-predicate")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.text.equals("NOT", ignoreCase = true)) return@forEachIndexed
            val inToken = tokens.getOrNull(index + 1) ?: return@forEachIndexed
            if (!inToken.text.equals("IN", ignoreCase = true)) return@forEachIndexed
            val subqueryTokens = tokens.subqueryTokensAfterNotIn(index, content) ?: return@forEachIndexed
            if (subqueryTokens.hasExplicitNotNullFilter()) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Filter nullable values inside Oracle NOT IN subqueries with IS NOT NULL, or use NOT EXISTS.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, inToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.subqueryTokensAfterNotIn(
    notIndex: Int,
    content: String,
): List<SqlToken>? {
    val inToken = getOrNull(notIndex + 1) ?: return null
    val selectIndex = notIndex + 2
    val selectToken = getOrNull(selectIndex) ?: return null
    if (!selectToken.hasText("SELECT")) return null
    if ('(' !in content.substring(inToken.endOffset, selectToken.startOffset)) return null

    val endIndex = indexOfFirstAfter(selectIndex) { token -> token.text == ";" }.let { if (it == -1) size else it }
    return subList(selectIndex, endIndex)
}

private inline fun List<SqlToken>.indexOfFirstAfter(
    startIndex: Int,
    predicate: (SqlToken) -> Boolean,
): Int {
    for (index in startIndex + 1 until size) {
        if (predicate(this[index])) return index
    }
    return -1
}

private fun List<SqlToken>.hasExplicitNotNullFilter(): Boolean =
    windowed(size = 3).any { tokens ->
        tokens[0].hasText("IS") && tokens[1].hasText("NOT") && tokens[2].hasText("NULL")
    }

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
