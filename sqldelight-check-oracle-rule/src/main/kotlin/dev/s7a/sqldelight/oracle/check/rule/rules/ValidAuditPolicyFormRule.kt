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
 * Reports conflicting static Oracle unified audit policy clauses.
 */
public class ValidAuditPolicyFormRule : Rule {
    override val id: RuleId = RuleId("valid-audit-policy-form")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskAuditPolicyFormCommentsAndQuotedTextPreservingOffsets()
            .auditPolicyStatements()
            .flatMap { statement -> statement.conflictingAuditPolicyClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a valid Oracle unified audit policy form: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class AuditPolicyToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class AuditPolicyStatement(
    val tokens: List<AuditPolicyToken>,
)

private data class AuditPolicyOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class AuditPolicyConflict(
    val group: String,
    val first: AuditPolicyOccurrence,
    val second: AuditPolicyOccurrence,
)

private fun String.auditPolicyStatements(): List<AuditPolicyStatement> =
    auditPolicyStatementPattern
        .findAll(this)
        .mapNotNull { match ->
            val statementEnd = indexOf(';', startIndex = match.range.first).let { if (it == -1) length else it + 1 }
            val tokens = substring(match.range.first, statementEnd).auditPolicyTokens(offset = match.range.first)
            if (tokens.size >= 2) AuditPolicyStatement(tokens) else null
        }.toList()

private fun AuditPolicyStatement.conflictingAuditPolicyClauses(): List<AuditPolicyConflict> {
    val occurrences = tokens.auditPolicyOccurrences()
    val firstByGroup = linkedMapOf<String, AuditPolicyOccurrence>()
    return occurrences.mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            AuditPolicyConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<AuditPolicyToken>.auditPolicyOccurrences(): List<AuditPolicyOccurrence> =
    mapIndexedNotNull { index, token ->
        when {
            token.auditPolicyHasText("BY") -> {
                token.auditPolicyOccurrence("BY/EXCEPT")
            }

            token.auditPolicyHasText("EXCEPT") -> {
                token.auditPolicyOccurrence("BY/EXCEPT")
            }

            token.auditPolicyHasText("WHENEVER") -> {
                auditPolicyOccurrence(
                    group = "WHENEVER",
                    startIndex = index,
                    endIndex =
                        when {
                            getOrNull(index + 1).auditPolicyHasText("NOT") &&
                                getOrNull(index + 2).auditPolicyHasText("SUCCESSFUL") -> index + 2

                            getOrNull(index + 1).auditPolicyHasText("SUCCESSFUL") -> index + 1

                            else -> index
                        },
                )
            }

            else -> {
                null
            }
        }
    }

private fun AuditPolicyToken.auditPolicyOccurrence(group: String): AuditPolicyOccurrence =
    AuditPolicyOccurrence(
        group = group,
        startOffset = startOffset,
        endOffset = endOffset,
    )

private fun List<AuditPolicyToken>.auditPolicyOccurrence(
    group: String,
    startIndex: Int,
    endIndex: Int,
): AuditPolicyOccurrence =
    AuditPolicyOccurrence(
        group = group,
        startOffset = this[startIndex].startOffset,
        endOffset = this[endIndex].endOffset,
    )

private val auditPolicyStatementPattern = Regex("""(?im)^\s*(?:AUDIT|NOAUDIT)\s+POLICY\b""")

private val auditPolicyTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.auditPolicyTokens(offset: Int): List<AuditPolicyToken> =
    auditPolicyTokenPattern
        .findAll(this)
        .map { match ->
            AuditPolicyToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskAuditPolicyFormCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> auditPolicyFormMaskRange(chars, index, skipAuditPolicyFormLineComment(index))
                startsWith("/*", index) -> auditPolicyFormMaskRange(chars, index, skipAuditPolicyFormBlockComment(index))
                chars[index] == '\'' -> auditPolicyFormMaskRange(chars, index, skipAuditPolicyFormQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipAuditPolicyFormLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipAuditPolicyFormBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipAuditPolicyFormQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'') {
            if (index + 1 < length && this[index + 1] == '\'') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun auditPolicyFormMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun AuditPolicyToken?.auditPolicyHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
