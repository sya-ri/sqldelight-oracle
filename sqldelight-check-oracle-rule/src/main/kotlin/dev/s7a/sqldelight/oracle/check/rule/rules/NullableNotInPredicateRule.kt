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
            if (subqueryTokens.hasExplicitNotNullFilter(content)) return@forEachIndexed

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
    val openOffset =
        content
            .lastIndexOf('(', startIndex = selectToken.startOffset)
            .takeIf { offset -> offset >= inToken.endOffset }
            ?: return null
    val closeOffset = content.matchingSqlParenthesis(openOffset) ?: return null

    val endIndex =
        indexOfFirstAfter(selectIndex) { token -> token.startOffset >= closeOffset }
            .let { if (it == -1) size else it }
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

private fun List<SqlToken>.hasExplicitNotNullFilter(content: String): Boolean {
    val selectedColumn = selectedSubqueryColumnName(content)
    if (selectedColumn != null) {
        return indices.any { index ->
            selectedColumn.matchesNotNullFilterAt(this, index, content)
        }
    }

    return windowed(size = 3).any { tokens ->
        tokens[0].hasText("IS") && tokens[1].hasText("NOT") && tokens[2].hasText("NULL")
    }
}

private fun List<SqlToken>.selectedSubqueryColumnName(content: String): NotInColumnName? {
    if (!getOrNull(0).hasText("SELECT")) return null
    val fromIndex = indexOfFirstAfter(0) { token -> token.hasText("FROM") }.takeIf { index -> index > 1 } ?: return null
    val selectedTokens =
        subList(1, fromIndex)
            .filterNot { token -> token.hasText("DISTINCT") || token.hasText("ALL") }
    return when (selectedTokens.size) {
        1 -> {
            selectedTokens
                .single()
                .text
                .takeUnless { text -> text == "*" }
                ?.let { name -> NotInColumnName(qualifier = null, name = name) }
        }

        2 -> {
            selectedTokens
                .takeIf { (qualifier, name) -> content.hasDotBetween(qualifier, name) }
                ?.let { (qualifier, name) -> NotInColumnName(qualifier = qualifier.text, name = name.text) }
        }

        else -> {
            null
        }
    }
}

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private data class NotInColumnName(
    val qualifier: String?,
    val name: String,
) {
    fun matchesNotNullFilterAt(
        tokens: List<SqlToken>,
        index: Int,
        content: String,
    ): Boolean =
        matchesUnqualifiedNotNullFilterAt(tokens, index) ||
            matchesQualifiedNotNullFilterAt(tokens, index, content)

    private fun matchesUnqualifiedNotNullFilterAt(
        tokens: List<SqlToken>,
        index: Int,
    ): Boolean =
        tokens.getOrNull(index).hasText(name) &&
            tokens.getOrNull(index + 1).hasText("IS") &&
            tokens.getOrNull(index + 2).hasText("NOT") &&
            tokens.getOrNull(index + 3).hasText("NULL")

    private fun matchesQualifiedNotNullFilterAt(
        tokens: List<SqlToken>,
        index: Int,
        content: String,
    ): Boolean {
        val qualifier = qualifier ?: return false
        val qualifierToken = tokens.getOrNull(index)
        val nameToken = tokens.getOrNull(index + 1)
        return qualifierToken.hasText(qualifier) &&
            nameToken.hasText(name) &&
            qualifierToken != null &&
            nameToken != null &&
            content.hasDotBetween(qualifierToken, nameToken) &&
            tokens.getOrNull(index + 2).hasText("IS") &&
            tokens.getOrNull(index + 3).hasText("NOT") &&
            tokens.getOrNull(index + 4).hasText("NULL")
    }
}

private fun String.hasDotBetween(
    first: SqlToken,
    second: SqlToken,
): Boolean = substring(first.endOffset, second.startOffset).contains('.')
