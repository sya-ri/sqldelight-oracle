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
 * Reports conflicting Oracle SET ROLE forms.
 */
public class NoConflictingSetRoleClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-set-role-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSetRoleCommentsAndQuotedTextPreservingOffsets()
            .setRoleStatements()
            .mapNotNull { statement -> statement.conflictingSetRoleClause() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle SET ROLE clauses: MODE.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SetRoleToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetRoleOccurrence(
    val startOffset: Int,
    val endOffset: Int,
)

private data class SetRoleConflict(
    val first: SetRoleOccurrence,
    val second: SetRoleOccurrence,
)

private fun String.setRoleStatements(): List<List<SetRoleToken>> {
    val statements = mutableListOf<List<SetRoleToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).setRoleTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<SetRoleToken>.conflictingSetRoleClause(): SetRoleConflict? {
    if (!getOrNull(0).setRoleHasText("SET") || !getOrNull(1).setRoleHasText("ROLE")) return null
    val forms = setRoleForms()
    val first = forms.firstOrNull() ?: return null
    val second = forms.drop(1).firstOrNull() ?: return null
    return SetRoleConflict(first, second)
}

private fun List<SetRoleToken>.setRoleForms(): List<SetRoleOccurrence> =
    buildList {
        var index = 2
        while (index < this@setRoleForms.size) {
            val token = this@setRoleForms[index]
            when {
                token.setRoleHasText("ALL") -> {
                    val endOffset =
                        if (this@setRoleForms.getOrNull(index + 1).setRoleHasText("EXCEPT")) {
                            val exceptRole = this@setRoleForms.getOrNull(index + 2)
                            index += if (exceptRole == null) 2 else 3
                            exceptRole?.endOffset ?: this@setRoleForms[index - 1].endOffset
                        } else {
                            index++
                            token.endOffset
                        }
                    add(SetRoleOccurrence(token.startOffset, endOffset))
                }

                token.setRoleHasText("NONE") -> {
                    add(SetRoleOccurrence(token.startOffset, token.endOffset))
                    index++
                }

                !token.setRoleHasText("IDENTIFIED") &&
                    !token.setRoleHasText("BY") &&
                    !token.setRoleHasText("EXCEPT") &&
                    token.text != ";" -> {
                    add(SetRoleOccurrence(token.startOffset, token.endOffset))
                    index++
                }

                else -> {
                    index++
                }
            }
        }
    }

private val setRoleTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.setRoleTokens(offset: Int): List<SetRoleToken> =
    setRoleTokenPattern
        .findAll(this)
        .map { match ->
            SetRoleToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskSetRoleCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> setRoleMaskRange(chars, index, skipSetRoleLineComment(index))
                startsWith("/*", index) -> setRoleMaskRange(chars, index, skipSetRoleBlockComment(index))
                chars[index] == '\'' -> setRoleMaskRange(chars, index, skipSetRoleQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipSetRoleLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipSetRoleBlockComment(start: Int): Int = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipSetRoleQuotedString(start: Int): Int {
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

private fun setRoleMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun SetRoleToken?.setRoleHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
