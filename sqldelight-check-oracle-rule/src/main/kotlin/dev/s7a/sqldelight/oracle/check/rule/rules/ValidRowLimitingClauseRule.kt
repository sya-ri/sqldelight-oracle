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
import java.math.BigDecimal

/**
 * Reports statically invalid Oracle row limiting clauses.
 */
public class ValidRowLimitingClauseRule : Rule {
    override val id: RuleId = RuleId("valid-row-limiting-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .rowLimitTokens()
            .rowLimitingClauseDiagnostics()
            .forEach { diagnostic ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = diagnostic.message,
                        file = context.file,
                        range = content.rangeAtOffsets(diagnostic.startOffset, diagnostic.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class RowLimitToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val depth: Int,
)

private data class RowLimitDiagnostic(
    val message: String,
    val startOffset: Int,
    val endOffset: Int,
)

private const val POSITIVE_ROW_COUNT_MESSAGE =
    "Use a positive static row count in Oracle row limiting clauses."

private const val PERCENT_RANGE_MESSAGE =
    "Use a static Oracle row limiting percentage from 0 through 100."

private const val WITH_TIES_ORDER_BY_MESSAGE =
    "Use ORDER BY with Oracle FETCH ... WITH TIES row limiting clauses."

private fun List<RowLimitToken>.rowLimitingClauseDiagnostics(): List<RowLimitDiagnostic> = offsetDiagnostics() + fetchDiagnostics()

private fun List<RowLimitToken>.offsetDiagnostics(): List<RowLimitDiagnostic> =
    mapIndexedNotNull { index, token ->
        if (!token.rowLimitHasText("OFFSET")) return@mapIndexedNotNull null
        val amount = getOrNull(index + 1)?.staticNumber() ?: return@mapIndexedNotNull null
        if (amount > BigDecimal.ZERO) return@mapIndexedNotNull null
        RowLimitDiagnostic(
            message = POSITIVE_ROW_COUNT_MESSAGE,
            startOffset = token.startOffset,
            endOffset = get(index + 1).endOffset,
        )
    }

private fun List<RowLimitToken>.fetchDiagnostics(): List<RowLimitDiagnostic> =
    flatMapIndexed { index, token ->
        if (!token.rowLimitHasText("FETCH")) return@flatMapIndexed emptyList()
        val firstOrNextIndex =
            when {
                getOrNull(index + 1).rowLimitHasText("FIRST") -> index + 1
                getOrNull(index + 1).rowLimitHasText("NEXT") -> index + 1
                else -> return@flatMapIndexed emptyList()
            }
        val clauseEndIndex = indexOfNextRowLimitBoundary(firstOrNextIndex + 1)
        val diagnostics = mutableListOf<RowLimitDiagnostic>()
        val amountIndex = firstStaticNumberIndex(firstOrNextIndex + 1, clauseEndIndex)
        if (amountIndex != null) {
            val amount = get(amountIndex).staticNumber()
            when {
                containsTextBetween("PERCENT", amountIndex + 1, clauseEndIndex) && amount != null &&
                    (amount < BigDecimal.ZERO || amount > BigDecimal(100)) -> {
                    diagnostics +=
                        RowLimitDiagnostic(
                            message = PERCENT_RANGE_MESSAGE,
                            startOffset = get(amountIndex).startOffset,
                            endOffset = get(amountIndex).endOffset,
                        )
                }

                !containsTextBetween("PERCENT", amountIndex + 1, clauseEndIndex) && amount != null &&
                    amount <= BigDecimal.ZERO -> {
                    diagnostics +=
                        RowLimitDiagnostic(
                            message = POSITIVE_ROW_COUNT_MESSAGE,
                            startOffset = get(amountIndex).startOffset,
                            endOffset = get(amountIndex).endOffset,
                        )
                }
            }
        }
        val withIndex = indexOfSequence(firstOrNextIndex + 1, clauseEndIndex, "WITH", "TIES")
        if (withIndex != null && !hasOrderByBeforeFetch(index)) {
            diagnostics +=
                RowLimitDiagnostic(
                    message = WITH_TIES_ORDER_BY_MESSAGE,
                    startOffset = get(withIndex).startOffset,
                    endOffset = get(withIndex + 1).endOffset,
                )
        }
        diagnostics
    }

private fun List<RowLimitToken>.firstStaticNumberIndex(
    startIndex: Int,
    endIndex: Int,
): Int? = (startIndex until endIndex).firstOrNull { index -> this[index].staticNumber() != null }

private fun List<RowLimitToken>.containsTextBetween(
    text: String,
    startIndex: Int,
    endIndex: Int,
): Boolean = (startIndex until endIndex).any { index -> this[index].rowLimitHasText(text) }

private fun List<RowLimitToken>.indexOfSequence(
    startIndex: Int,
    endIndex: Int,
    vararg text: String,
): Int? =
    (startIndex until endIndex - text.size + 1).firstOrNull { index ->
        text.indices.all { offset -> this[index + offset].rowLimitHasText(text[offset]) }
    }

private fun List<RowLimitToken>.indexOfNextRowLimitBoundary(startIndex: Int): Int =
    (startIndex until size).firstOrNull { index ->
        this[index].rowLimitHasText(";") ||
            this[index].rowLimitHasText("UNION") ||
            this[index].rowLimitHasText("INTERSECT") ||
            this[index].rowLimitHasText("MINUS") ||
            this[index].rowLimitHasText("EXCEPT")
    } ?: size

private fun List<RowLimitToken>.hasOrderByBeforeFetch(fetchIndex: Int): Boolean {
    val fetchDepth = get(fetchIndex).depth
    val startIndex =
        (fetchIndex downTo 0).firstOrNull { index ->
            this[index].depth == fetchDepth && this[index].isRowLimitQueryBoundary()
        } ?: 0
    return (startIndex until fetchIndex - 1).any { index ->
        this[index].depth == fetchDepth &&
            this[index].rowLimitHasText("ORDER") &&
            this[index + 1].depth == fetchDepth &&
            this[index + 1].rowLimitHasText("BY")
    }
}

private val rowLimitTokenPattern = Regex("""-?\d+(?:\.\d+)?|[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|;""")

private fun RowLimitToken.isRowLimitQueryBoundary(): Boolean =
    rowLimitHasText("SELECT") ||
        rowLimitHasText(";") ||
        rowLimitHasText("UNION") ||
        rowLimitHasText("INTERSECT") ||
        rowLimitHasText("MINUS") ||
        rowLimitHasText("EXCEPT")

private fun String.rowLimitTokens(): List<RowLimitToken> {
    var depth = 0
    return rowLimitTokenPattern
        .findAll(this)
        .mapNotNull { match ->
            when (match.value) {
                "(" -> {
                    depth++
                    null
                }

                ")" -> {
                    if (depth > 0) depth--
                    null
                }

                else -> {
                    RowLimitToken(
                        text = match.value,
                        startOffset = match.range.first,
                        endOffset = match.range.last + 1,
                        depth = depth,
                    )
                }
            }
        }.toList()
}

private fun RowLimitToken.staticNumber(): BigDecimal? = text.toBigDecimalOrNull()

private fun RowLimitToken?.rowLimitHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
