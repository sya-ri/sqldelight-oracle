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
 * Reports invalid static Oracle DML WAIT clause values.
 */
public class ValidDmlWaitClauseRule : Rule {
    override val id: RuleId = RuleId("valid-dml-wait-clause")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val maskedContent = content.maskSqlCommentsAndQuotedTextPreservingOffsets()
        content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .mapNotNull { statement -> statement.invalidDmlWaitClause(maskedContent) }
            .forEach { clause ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a non-negative static value in Oracle DML WAIT clauses.",
                        file = context.file,
                        range = content.rangeAtOffsets(clause.startOffset, clause.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class DmlWaitClause(
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<SqlToken>.invalidDmlWaitClause(maskedContent: String): DmlWaitClause? {
    if (!isDmlWaitRuleStatement()) return null
    val token =
        asReversed()
            .firstOrNull { token -> token.isTopLevelDmlWaitToken(this, maskedContent) }
            ?: return null
    if (token.hasDmlWaitText("NOWAIT")) return null
    val value = maskedContent.dmlWaitValueAfter(token.endOffset)
    return when {
        value != null && value.isValid -> null
        else -> DmlWaitClause(token.startOffset, value?.endOffset ?: token.endOffset)
    }
}

private fun List<SqlToken>.isDmlWaitRuleStatement(): Boolean =
    any { token ->
        token.hasDmlWaitText("INSERT") ||
            token.hasDmlWaitText("UPDATE") ||
            token.hasDmlWaitText("DELETE") ||
            token.hasDmlWaitText("MERGE")
    }

private fun SqlToken.isTopLevelDmlWaitToken(
    statement: List<SqlToken>,
    maskedContent: String,
): Boolean {
    if (!hasDmlWaitText("WAIT") && !hasDmlWaitText("NOWAIT")) return false
    if (maskedContent.dmlWaitParenthesisDepthBetween(statement.first().startOffset, startOffset) != 0) return false
    if (maskedContent.previousNonWhitespaceChar(startOffset) == '.') return false
    val nextChar = maskedContent.nextNonWhitespaceChar(endOffset)
    if (nextChar == '.' || nextChar == '=') return false
    if (
        hasDmlWaitText("WAIT") &&
        nextChar != ';' &&
        nextChar != '-' &&
        nextChar?.isSqlIdentifierStart() != true &&
        nextChar?.isDigit() != true
    ) {
        return false
    }
    if (statement.nextTopLevelDmlWaitToken(this, maskedContent)?.text?.uppercase() in dmlWaitClauseFollowers) return false
    return statement.previousTopLevelDmlWaitToken(this, maskedContent)?.text?.uppercase() !in dmlWaitIdentifierPredecessors
}

private fun List<SqlToken>.previousTopLevelDmlWaitToken(
    token: SqlToken,
    maskedContent: String,
): SqlToken? {
    val index = indexOf(token)
    if (index <= 0) return null
    return subList(0, index)
        .asReversed()
        .firstOrNull { previous ->
            maskedContent.dmlWaitParenthesisDepthBetween(first().startOffset, previous.startOffset) == 0
        }
}

private fun List<SqlToken>.nextTopLevelDmlWaitToken(
    token: SqlToken,
    maskedContent: String,
): SqlToken? {
    val index = indexOf(token)
    if (index == -1 || index >= lastIndex) return null
    return subList(index + 1, size)
        .firstOrNull { next ->
            maskedContent.dmlWaitParenthesisDepthBetween(first().startOffset, next.startOffset) == 0
        }
}

private data class DmlWaitValue(
    val isValid: Boolean,
    val endOffset: Int,
)

private fun String.dmlWaitValueAfter(offset: Int): DmlWaitValue? {
    var index = offset
    while (index < length && this[index].isWhitespace()) index++
    if (index >= length || this[index] == ';') return null
    if (wordAt(index, "FOREVER")) return DmlWaitValue(isValid = true, endOffset = index + "FOREVER".length)

    val valueStart = index
    val negative = getOrNull(index) == '-'
    if (negative) index++
    val digitStart = index
    while (index < length && this[index].isDigit()) index++
    if (index > digitStart) return DmlWaitValue(isValid = !negative, endOffset = index)

    if (getOrNull(index)?.isSqlIdentifierStart() == true) {
        index++
        while (index < length && this[index].isSqlIdentifierPart()) index++
        return DmlWaitValue(isValid = false, endOffset = index)
    }

    return DmlWaitValue(isValid = false, endOffset = valueStart + 1)
}

private fun String.wordAt(
    offset: Int,
    text: String,
): Boolean {
    if (!regionMatches(offset, text, 0, text.length, ignoreCase = true)) return false
    return !getOrNull(offset + text.length).isSqlIdentifierPartOrFalse()
}

private fun String.dmlWaitParenthesisDepthBetween(
    startOffset: Int,
    endOffset: Int,
): Int {
    var depth = 0
    for (index in startOffset until endOffset) {
        when (this[index]) {
            '(' -> depth++
            ')' -> if (depth > 0) depth--
        }
    }
    return depth
}

private fun String.previousNonWhitespaceChar(offset: Int): Char? {
    var index = offset - 1
    while (index >= 0 && this[index].isWhitespace()) index--
    return getOrNull(index)
}

private fun String.nextNonWhitespaceChar(offset: Int): Char? {
    var index = offset
    while (index < length && this[index].isWhitespace()) index++
    return getOrNull(index)
}

private fun Char?.isSqlIdentifierPartOrFalse(): Boolean = this?.isSqlIdentifierPart() == true

private fun Char.isSqlIdentifierStart(): Boolean = isLetter() || this == '_' || this == '$' || this == '#'

private fun Char.isSqlIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$' || this == '#'

private fun SqlToken.hasDmlWaitText(text: String): Boolean = this.text.equals(text, ignoreCase = true)

private val dmlWaitIdentifierPredecessors =
    setOf(
        "AS",
        "DELETE",
        "FROM",
        "INTO",
        "JOIN",
        "MERGE",
        "OF",
        "ON",
        "SET",
        "TABLE",
        "THEN",
        "UPDATE",
        "USING",
    )

private val dmlWaitClauseFollowers =
    setOf(
        "CONNECT",
        "ERROR",
        "FROM",
        "GROUP",
        "HAVING",
        "LOG",
        "MODEL",
        "ON",
        "ORDER",
        "RETURNING",
        "SET",
        "UNION",
        "WHEN",
        "WHERE",
    )
