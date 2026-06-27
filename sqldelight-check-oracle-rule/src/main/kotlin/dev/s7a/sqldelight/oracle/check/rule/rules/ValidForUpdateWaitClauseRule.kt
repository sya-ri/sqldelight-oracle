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
 * Reports invalid static Oracle FOR UPDATE wait clauses.
 */
public class ValidForUpdateWaitClauseRule : Rule {
    override val id: RuleId = RuleId("valid-for-update-wait-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .forUpdateWaitTokens()
            .negativeForUpdateWaitClauses()
            .forEach { clause ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a non-negative static value in Oracle FOR UPDATE WAIT clauses.",
                        file = context.file,
                        range = content.rangeAtOffsets(clause.startOffset, clause.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class ForUpdateWaitToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val depth: Int,
)

private data class ForUpdateWaitClause(
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<ForUpdateWaitToken>.negativeForUpdateWaitClauses(): List<ForUpdateWaitClause> =
    flatMapIndexed { index, token ->
        if (!token.forUpdateWaitHasText("FOR") || !getOrNull(index + 1).forUpdateWaitHasText("UPDATE")) {
            return@flatMapIndexed emptyList()
        }
        val clauseEnd = indexOfForUpdateWaitClauseBoundary(index + 2)
        (index + 2 until clauseEnd)
            .mapNotNull { waitIndex ->
                val waitToken = get(waitIndex)
                val valueToken = getOrNull(waitIndex + 1)
                if (
                    waitToken.depth == token.depth &&
                    waitToken.forUpdateWaitHasText("WAIT") &&
                    valueToken?.depth == token.depth &&
                    valueToken.text.toLongOrNull()?.let { value -> value < 0 } == true
                ) {
                    ForUpdateWaitClause(waitToken.startOffset, valueToken.endOffset)
                } else {
                    null
                }
            }
    }

private fun List<ForUpdateWaitToken>.indexOfForUpdateWaitClauseBoundary(startIndex: Int): Int =
    (startIndex until size).firstOrNull { index ->
        get(index).forUpdateWaitHasText(";") ||
            get(index).forUpdateWaitHasText("UNION") ||
            get(index).forUpdateWaitHasText("INTERSECT") ||
            get(index).forUpdateWaitHasText("MINUS") ||
            get(index).forUpdateWaitHasText("EXCEPT")
    } ?: size

private val forUpdateWaitTokenPattern = Regex("""-?\d+|[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|;""")

private fun String.forUpdateWaitTokens(): List<ForUpdateWaitToken> {
    var depth = 0
    return forUpdateWaitTokenPattern
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
                    ForUpdateWaitToken(
                        text = match.value,
                        startOffset = match.range.first,
                        endOffset = match.range.last + 1,
                        depth = depth,
                    )
                }
            }
        }.toList()
}

private fun ForUpdateWaitToken?.forUpdateWaitHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
