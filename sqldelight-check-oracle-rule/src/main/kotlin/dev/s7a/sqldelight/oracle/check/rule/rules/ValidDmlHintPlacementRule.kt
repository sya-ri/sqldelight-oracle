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
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports static Oracle APPEND and APPEND_VALUES hints outside their DML placement.
 */
public class ValidDmlHintPlacementRule : Rule {
    override val id: RuleId = RuleId("valid-dml-hint-placement")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .forEach { statement ->
                val statementKind = statement.firstDmlKeyword() ?: return@forEach
                val statementRange = statement.first().startOffset until statement.last().endOffset
                val hasValuesClause = statement.any { token -> token.text.equals("VALUES", ignoreCase = true) }
                content.oracleHintComments(statementRange).forEach { hint ->
                    if (hint.hasHint("APPEND") && statementKind != "INSERT") {
                        reporter.reportHint(context, hint)
                    }
                    if (hint.hasHint("APPEND_VALUES") && (statementKind != "INSERT" || !hasValuesClause)) {
                        reporter.reportHint(context, hint)
                    }
                }
            }
    }
}

private fun List<SqlToken>.firstDmlKeyword(): String? =
    firstOrNull { token ->
        token.text.uppercase() in dmlStatementKeywords
    }?.text?.uppercase()

private val dmlStatementKeywords = setOf("SELECT", "INSERT", "UPDATE", "DELETE", "MERGE")

private data class OracleHintComment(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.oracleHintComments(range: IntRange): List<OracleHintComment> {
    val hints = mutableListOf<OracleHintComment>()
    var index = range.first
    while (index <= range.last && index < length) {
        index =
            when {
                startsWith("/*+", index) -> {
                    val end = indexOf("*/", startIndex = index + 3).let { if (it == -1) length else it + 2 }
                    hints += OracleHintComment(substring(index, end), index, end)
                    end
                }
                startsWith("--+", index) -> {
                    val end = indexOf('\n', startIndex = index + 3).let { if (it == -1) length else it }
                    hints += OracleHintComment(substring(index, end), index, end)
                    end
                }
                else -> index + 1
            }
    }
    return hints
}

private fun OracleHintComment.hasHint(name: String): Boolean =
    Regex("""(?i)(?:^|[^A-Z0-9_$])${Regex.escape(name)}(?:$|[^A-Z0-9_$])""").containsMatchIn(text)

private fun DiagnosticReporter.reportHint(
    context: RuleContext,
    hint: OracleHintComment,
) {
    report(
        RuleDiagnostic(
            severity = Severity.Warning,
            message = "Use Oracle APPEND only on INSERT statements and APPEND_VALUES only on INSERT statements with VALUES.",
            file = context.file,
            range = context.file.content.rangeAtOffsets(hint.startOffset, hint.endOffset),
            database = context.database,
        ),
    )
}
