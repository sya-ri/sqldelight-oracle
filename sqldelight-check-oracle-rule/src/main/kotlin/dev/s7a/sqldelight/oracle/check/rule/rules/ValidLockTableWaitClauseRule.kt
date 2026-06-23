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
 * Reports invalid static Oracle LOCK TABLE wait clauses.
 */
public class ValidLockTableWaitClauseRule : Rule {
    override val id: RuleId = RuleId("valid-lock-table-wait-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .lockWaitStatements()
            .flatMap { statement -> statement.invalidLockWaitClauses() }
            .forEach { invalid ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a valid Oracle LOCK TABLE wait clause: ${invalid.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(invalid.first.startOffset, invalid.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class LockWaitToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class LockWaitOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class InvalidLockWaitClause(
    val group: String,
    val first: LockWaitOccurrence,
    val second: LockWaitOccurrence,
)

private fun String.lockWaitStatements(): List<List<LockWaitToken>> {
    val statements = mutableListOf<List<LockWaitToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).lockWaitTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<LockWaitToken>.invalidLockWaitClauses(): List<InvalidLockWaitClause> {
    if (!getOrNull(0).lockWaitHasText("LOCK") || !getOrNull(1).lockWaitHasText("TABLE")) return emptyList()
    val firstByGroup = linkedMapOf<String, LockWaitOccurrence>()
    return lockWaitOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            InvalidLockWaitClause(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<LockWaitToken>.lockWaitOccurrences(): List<LockWaitOccurrence> =
    buildList {
        this@lockWaitOccurrences.forEachIndexed { index, token ->
            when {
                token.lockWaitHasText("NOWAIT") -> {
                    add(
                        LockWaitOccurrence(
                            group = "WAIT",
                            startOffset = token.startOffset,
                            endOffset = token.endOffset,
                        ),
                    )
                }

                token.lockWaitHasText("WAIT") -> {
                    val waitValue = this@lockWaitOccurrences.getOrNull(index + 1)
                    val endOffset = waitValue?.endOffset ?: token.endOffset
                    add(
                        LockWaitOccurrence(
                            group = "WAIT",
                            startOffset = token.startOffset,
                            endOffset = endOffset,
                        ),
                    )
                    if (waitValue == null || waitValue.text.toLongOrNull() == null) {
                        add(
                            LockWaitOccurrence(
                                group = "WAIT VALUE",
                                startOffset = token.startOffset,
                                endOffset = endOffset,
                            ),
                        )
                        add(
                            LockWaitOccurrence(
                                group = "WAIT VALUE",
                                startOffset = token.startOffset,
                                endOffset = endOffset,
                            ),
                        )
                    }
                }
            }
        }
    }

private val lockWaitTokenPattern = Regex("""-?\d+|[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.lockWaitTokens(offset: Int): List<LockWaitToken> =
    lockWaitTokenPattern
        .findAll(this)
        .map { match ->
            LockWaitToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun LockWaitToken?.lockWaitHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
