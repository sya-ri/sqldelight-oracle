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
 * Reports legacy Oracle outer join operator usages that violate static restrictions.
 */
public class ValidOuterJoinOperatorRule : Rule {
    override val id: RuleId = RuleId("valid-outer-join-operator")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .outerJoinTokens()
            .invalidOuterJoinOperatorConditions()
            .forEach { condition ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = condition.message,
                        file = context.file,
                        range = content.rangeAtOffsets(condition.startOffset, condition.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class OuterJoinToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val depth: Int,
)

private data class OuterJoinCondition(
    val message: String,
    val startOffset: Int,
    val endOffset: Int,
)

private const val OUTER_JOIN_OR_IN_MESSAGE =
    "Avoid Oracle legacy outer join operator (+) with OR or IN conditions."

private const val MIXED_JOIN_SYNTAX_MESSAGE =
    "Avoid mixing Oracle legacy outer join operator (+) with FROM clause JOIN syntax."

private fun List<OuterJoinToken>.invalidOuterJoinOperatorConditions(): List<OuterJoinCondition> =
    invalidOuterJoinOrInConditions() + invalidMixedJoinSyntaxConditions()

private fun List<OuterJoinToken>.invalidOuterJoinOrInConditions(): List<OuterJoinCondition> =
    splitOuterJoinConditions()
        .filter { condition -> condition.hasOuterJoinOperator() && condition.hasForbiddenOuterJoinOperatorTerm() }
        .map { condition ->
            OuterJoinCondition(
                message = OUTER_JOIN_OR_IN_MESSAGE,
                startOffset = condition.first().startOffset,
                endOffset = condition.last().endOffset,
            )
        }

private fun List<OuterJoinToken>.invalidMixedJoinSyntaxConditions(): List<OuterJoinCondition> {
    val joinToken = firstOrNull { token -> token.depth == 0 && token.outerJoinHasText("JOIN") } ?: return emptyList()
    val outerJoinOperator = topLevelOuterJoinOperator() ?: return emptyList()
    return listOf(
        OuterJoinCondition(
            message = MIXED_JOIN_SYNTAX_MESSAGE,
            startOffset = minOf(joinToken.startOffset, outerJoinOperator.startOffset),
            endOffset = maxOf(joinToken.endOffset, outerJoinOperator.endOffset),
        ),
    )
}

private fun List<OuterJoinToken>.splitOuterJoinConditions(): List<List<OuterJoinToken>> {
    val conditions = mutableListOf<List<OuterJoinToken>>()
    var startIndex = 0
    var depth = 0
    forEachIndexed { index, token ->
        when {
            token.outerJoinHasText("(") -> {
                depth++
            }

            token.outerJoinHasText(")") && depth > 0 -> {
                depth--
            }

            depth == 0 && token.isOuterJoinConditionBoundary() -> {
                if (startIndex < index) conditions += subList(startIndex, index)
                startIndex = index + 1
            }
        }
    }
    if (startIndex < size) conditions += subList(startIndex, size)
    return conditions
}

private fun List<OuterJoinToken>.hasOuterJoinOperator(): Boolean =
    windowed(size = 3).any { tokens ->
        tokens[0].outerJoinHasText("(") && tokens[1].outerJoinHasText("+") && tokens[2].outerJoinHasText(")")
    }

private fun List<OuterJoinToken>.topLevelOuterJoinOperator(): OuterJoinToken? =
    windowed(size = 3)
        .firstOrNull { tokens ->
            tokens[1].depth <= 1 &&
                tokens[0].outerJoinHasText("(") &&
                tokens[1].outerJoinHasText("+") &&
                tokens[2].outerJoinHasText(")")
        }?.get(1)

private fun List<OuterJoinToken>.hasForbiddenOuterJoinOperatorTerm(): Boolean =
    any { token -> token.outerJoinHasText("OR") || token.outerJoinHasText("IN") }

private fun OuterJoinToken.isOuterJoinConditionBoundary(): Boolean =
    outerJoinHasText(";") ||
        outerJoinHasText("WHERE") ||
        outerJoinHasText("GROUP") ||
        outerJoinHasText("HAVING") ||
        outerJoinHasText("ORDER") ||
        outerJoinHasText("JOIN") ||
        outerJoinHasText("ON")

private val outerJoinTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|\+|;""")

private fun String.outerJoinTokens(): List<OuterJoinToken> {
    var depth = 0
    return outerJoinTokenPattern
        .findAll(this)
        .map { match ->
            if (match.value == ")" && depth > 0) depth--
            val token =
                OuterJoinToken(
                    text = match.value,
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    depth = depth,
                )
            if (match.value == "(") depth++
            token
        }.toList()
}

private fun OuterJoinToken?.outerJoinHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
