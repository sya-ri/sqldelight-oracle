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
 * Reports conflicting Oracle CREATE ROLE and ALTER ROLE clauses.
 */
public class NoConflictingRoleClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-role-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .roleClauseStatements()
            .flatMap { statement -> statement.conflictingRoleClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle role clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class RoleClauseToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class RoleClauseOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class RoleClauseConflict(
    val group: String,
    val first: RoleClauseOccurrence,
    val second: RoleClauseOccurrence,
)

private fun String.roleClauseStatements(): List<List<RoleClauseToken>> {
    val statements = mutableListOf<List<RoleClauseToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).roleClauseTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<RoleClauseToken>.conflictingRoleClauses(): List<RoleClauseConflict> {
    if (!isRoleStatement()) return emptyList()
    val firstByGroup = linkedMapOf<String, RoleClauseOccurrence>()
    return roleClauseOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            RoleClauseConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<RoleClauseToken>.isRoleStatement(): Boolean =
    (getOrNull(0).roleClauseHasText("CREATE") || getOrNull(0).roleClauseHasText("ALTER")) &&
        take(4).any { token -> token.roleClauseHasText("ROLE") }

private fun List<RoleClauseToken>.roleClauseOccurrences(): List<RoleClauseOccurrence> =
    buildList {
        this@roleClauseOccurrences.forEachIndexed { index, token ->
            when {
                token.roleClauseHasText("IDENTIFIED") &&
                    !this@roleClauseOccurrences.getOrNull(index - 1).roleClauseHasText("NOT") -> {
                    add(
                        RoleClauseOccurrence(
                            group = "IDENTIFICATION",
                            startOffset = token.startOffset,
                            endOffset =
                                if (this@roleClauseOccurrences.getOrNull(index + 1).roleClauseHasText("BY")) {
                                    this@roleClauseOccurrences.getOrNull(index + 2)?.endOffset
                                        ?: this@roleClauseOccurrences[index + 1].endOffset
                                } else {
                                    this@roleClauseOccurrences.getOrNull(index + 1)?.endOffset ?: token.endOffset
                                },
                        ),
                    )
                }

                token.roleClauseHasText("NOT") &&
                    this@roleClauseOccurrences.getOrNull(index + 1).roleClauseHasText("IDENTIFIED") -> {
                    add(
                        RoleClauseOccurrence(
                            group = "IDENTIFICATION",
                            startOffset = token.startOffset,
                            endOffset = this@roleClauseOccurrences[index + 1].endOffset,
                        ),
                    )
                }

                token.roleClauseHasText("CONTAINER") -> {
                    add(
                        RoleClauseOccurrence(
                            group = "CONTAINER",
                            startOffset = token.startOffset,
                            endOffset = this@roleClauseOccurrences.getOrNull(index + 2)?.endOffset ?: token.endOffset,
                        ),
                    )
                }
            }
        }
    }

private val roleClauseTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|=|;""")

private fun String.roleClauseTokens(offset: Int): List<RoleClauseToken> =
    roleClauseTokenPattern
        .findAll(this)
        .map { match ->
            RoleClauseToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun RoleClauseToken?.roleClauseHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
