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
 * Reports quoted uppercase ROWID identifiers when they are used as Oracle column names.
 */
public class NoUppercaseRowidColumnRule : Rule {
    override val id: RuleId = RuleId("no-uppercase-rowid-column")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedTextPreservingOffsets(maskDoubleQuotedIdentifiers = false)
        uppercaseRowidIdentifierPattern
            .findAll(masked)
            .filter { match -> masked.isQuotedRowidColumnName(match.range.first, match.range.last + 1) }
            .forEach { match ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Do not use quoted uppercase \"ROWID\" as an Oracle column name.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

private val uppercaseRowidIdentifierPattern = Regex(""""ROWID"""")

private val rowidColumnFollowingWords =
    setOf(
        "BINARY_DOUBLE",
        "BINARY_FLOAT",
        "BLOB",
        "BOOLEAN",
        "CHAR",
        "CLOB",
        "DATE",
        "DECIMAL",
        "DOUBLE",
        "FLOAT",
        "GENERATED",
        "INTEGER",
        "INTERVAL",
        "JSON",
        "LONG",
        "NCHAR",
        "NCLOB",
        "NUMBER",
        "NUMERIC",
        "RAW",
        "REAL",
        "ROWID",
        "TIMESTAMP",
        "UROWID",
        "VARCHAR",
        "VARCHAR2",
    )

private fun String.isQuotedRowidColumnName(
    startOffset: Int,
    endOffset: Int,
): Boolean {
    if (isRenameColumnTarget(startOffset, endOffset)) return true
    if (isAlterColumnOperationTarget(startOffset)) return true
    if (!isCreateTableColumnDefinition(startOffset)) return false
    val nextWord = nextRowidRuleWord(endOffset) ?: return false
    return nextWord in rowidColumnFollowingWords || nextWord.startsWith("VECTOR")
}

private fun String.isRenameColumnTarget(
    startOffset: Int,
    endOffset: Int,
): Boolean {
    val before = previousRowidRuleWords(startOffset, count = 6)
    if (before.takeLast(2) == listOf("RENAME", "COLUMN")) return true
    if (Regex("""(?is)\bRENAME\s+COLUMN\b.+\bTO\s*$""").containsMatchIn(statementPrefixBefore(startOffset))) return true
    val after = nextRowidRuleWord(endOffset)
    return before.lastOrNull() == "COLUMN" && after == "TO"
}

private fun String.isAlterColumnOperationTarget(startOffset: Int): Boolean {
    val before = previousRowidRuleWords(startOffset, count = 4)
    return before.lastOrNull() == "ADD" ||
        before.lastOrNull() == "MODIFY" ||
        before.takeLast(2) == listOf("ADD", "COLUMN") ||
        before.takeLast(2) == listOf("MODIFY", "COLUMN")
}

private fun String.isCreateTableColumnDefinition(startOffset: Int): Boolean {
    if (!Regex("""(?is)\bCREATE\b.+\bTABLE\b""").containsMatchIn(statementPrefixBefore(startOffset))) return false
    val previous = previousNonWhitespaceIndex(startOffset) ?: return false
    return this[previous] == '(' || this[previous] == ','
}

private fun String.statementPrefixBefore(startOffset: Int): String {
    val statementStart = lastIndexOf(';', startIndex = startOffset).let { index -> if (index == -1) 0 else index + 1 }
    return substring(statementStart, startOffset)
}

private fun String.previousRowidRuleWords(
    startOffset: Int,
    count: Int,
): List<String> {
    val words = mutableListOf<String>()
    var index = startOffset - 1
    while (index >= 0 && words.size < count) {
        while (index >= 0 && !this[index].isLetterOrDigit() && this[index] != '_') index--
        val end = index + 1
        while (index >= 0 && (this[index].isLetterOrDigit() || this[index] == '_')) index--
        if (end > index + 1) words += substring(index + 1, end).uppercase()
    }
    return words.asReversed()
}

private fun String.nextRowidRuleWord(endOffset: Int): String? {
    var index = endOffset
    while (index < length && !this[index].isLetterOrDigit() && this[index] != '_') {
        if (this[index] == ')' || this[index] == ',') return null
        index++
    }
    val start = index
    while (index < length && (this[index].isLetterOrDigit() || this[index] == '_')) index++
    return if (start < index) substring(start, index).uppercase() else null
}

private fun String.previousNonWhitespaceIndex(startOffset: Int): Int? {
    var index = startOffset - 1
    while (index >= 0) {
        if (!this[index].isWhitespace()) return index
        index--
    }
    return null
}
