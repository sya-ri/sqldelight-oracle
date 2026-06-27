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
 * Reports static Oracle CTAS column alias list count and duplicate problems.
 */
public class ValidCreateTableAsColumnAliasesRule : Rule {
    override val id: RuleId = RuleId("valid-create-table-as-column-aliases")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val maskedContent =
            content.maskSqlCommentsAndQuotedTextPreservingOffsets(
                maskQuoteDelimiters = false,
                maskDoubleQuotedIdentifiers = false,
            )
        maskedContent.createTableAsAliasChecks().forEach { check ->
            check.duplicateAlias()?.let { duplicate ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Oracle CTAS column alias '${duplicate.text}' is declared more than once.",
                        file = context.file,
                        range = content.rangeAtOffsets(duplicate.range.first, duplicate.range.last + 1),
                        database = context.database,
                    ),
                )
            }

            if (check.aliases.size != check.selectColumnCount) {
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message =
                            "Oracle CTAS declares ${check.aliases.size} column alias(es), " +
                                "but the SELECT list has ${check.selectColumnCount} column(s).",
                        file = context.file,
                        range = content.rangeAtOffsets(check.aliasListRange.first, check.aliasListRange.last + 1),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private data class CtasAlias(
    val text: String,
    val range: IntRange,
)

private data class CreateTableAsAliasCheck(
    val aliases: List<CtasAlias>,
    val aliasListRange: IntRange,
    val selectColumnCount: Int,
) {
    fun duplicateAlias(): CtasAlias? {
        val seen = mutableSetOf<String>()
        return aliases.firstOrNull { alias -> !seen.add(alias.text.ctasAliasKey()) }
    }
}

private fun String.ctasAliasKey(): String =
    if (startsWith("\"") && endsWith("\"")) {
        removeSurrounding("\"").replace("\"\"", "\"")
    } else {
        uppercase()
    }

private fun String.createTableAsAliasChecks(): List<CreateTableAsAliasCheck> {
    val checks = mutableListOf<CreateTableAsAliasCheck>()
    var statementStart = 0
    while (statementStart < length) {
        val statementEnd = indexOf(';', startIndex = statementStart).let { if (it == -1) length else it + 1 }
        parseCreateTableAsAliasCheck(statementStart until statementEnd)?.let { checks += it }
        statementStart = statementEnd
    }
    return checks
}

private fun String.parseCreateTableAsAliasCheck(statementRange: IntRange): CreateTableAsAliasCheck? {
    val createOffset = ctasIndexOfKeyword("CREATE", statementRange.first, statementRange.last + 1) ?: return null
    val tableOffset = ctasIndexOfKeyword("TABLE", createOffset + "CREATE".length, statementRange.last + 1) ?: return null
    val tableNameStart = ctasSkipIfNotExists(tableOffset + "TABLE".length, statementRange.last + 1)
    var index = ctasSkipQualifiedTableName(tableNameStart, statementRange.last + 1) ?: return null
    index = ctasSkipWhitespace(index, statementRange.last + 1)
    index = ctasSkipSharingClause(index, statementRange.last + 1)
    index = ctasSkipWhitespace(index, statementRange.last + 1)
    if (index !in indices || this[index] != '(') return null

    val aliasListEnd = matchingSqlParenthesis(index) ?: return null
    val asOffset = ctasIndexOfKeyword("AS", aliasListEnd + 1, statementRange.last + 1) ?: return null
    val selectOffset = ctasIndexOfKeyword("SELECT", asOffset + "AS".length, statementRange.last + 1) ?: return null
    val fromOffset = ctasIndexOfTopLevelKeyword("FROM", selectOffset + "SELECT".length, statementRange.last + 1) ?: return null
    val aliases = ctasAliasesIn(index + 1, aliasListEnd)
    if (aliases.isEmpty()) return null

    return CreateTableAsAliasCheck(
        aliases = aliases,
        aliasListRange = index..aliasListEnd,
        selectColumnCount = countTopLevelSqlItems(selectOffset + "SELECT".length, fromOffset),
    )
}

private fun String.ctasSkipIfNotExists(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = ctasSkipWhitespace(startOffset, endOffset)
    if (ctasIndexOfKeyword("IF", index, endOffset) != index) return index
    index = ctasSkipWhitespace(index + "IF".length, endOffset)
    if (ctasIndexOfKeyword("NOT", index, endOffset) != index) return startOffset
    index = ctasSkipWhitespace(index + "NOT".length, endOffset)
    if (ctasIndexOfKeyword("EXISTS", index, endOffset) != index) return startOffset
    return ctasSkipWhitespace(index + "EXISTS".length, endOffset)
}

private fun String.ctasSkipQualifiedTableName(
    startOffset: Int,
    endOffset: Int,
): Int? {
    var index = ctasSkipWhitespace(startOffset, endOffset)
    val firstNameEnd = ctasIdentifierEnd(index, endOffset)
    if (firstNameEnd == index) return null
    index = ctasSkipWhitespace(firstNameEnd, endOffset)
    if (index < endOffset && this[index] == '.') {
        index = ctasSkipWhitespace(index + 1, endOffset)
        val secondNameEnd = ctasIdentifierEnd(index, endOffset)
        if (secondNameEnd == index) return null
        index = secondNameEnd
    }
    return index
}

private fun String.ctasSkipSharingClause(
    startOffset: Int,
    endOffset: Int,
): Int {
    val sharingOffset = ctasIndexOfKeyword("SHARING", startOffset, endOffset)
    if (sharingOffset != startOffset) return startOffset
    val equalsOffset =
        indexOf(
            '=',
            startIndex = sharingOffset + "SHARING".length,
        ).takeIf { offset ->
            offset in startOffset until endOffset
        } ?: return startOffset
    var index = ctasSkipWhitespace(equalsOffset + 1, endOffset)
    val valueEnd = ctasIdentifierEnd(index, endOffset)
    if (valueEnd == index) return startOffset
    index = ctasSkipWhitespace(valueEnd, endOffset)
    if (regionMatches(index, "DATA", 0, "DATA".length, ignoreCase = true) && ctasIsBoundary(index + "DATA".length)) {
        index = ctasSkipWhitespace(index + "DATA".length, endOffset)
    }
    return index
}

private fun String.ctasAliasesIn(
    startOffset: Int,
    endOffset: Int,
): List<CtasAlias> {
    val aliases = mutableListOf<CtasAlias>()
    var index = startOffset
    while (index < endOffset) {
        index = ctasSkipWhitespaceAndCommas(index, endOffset)
        val aliasEnd = ctasIdentifierEnd(index, endOffset)
        if (aliasEnd > index) {
            aliases += CtasAlias(substring(index, aliasEnd), index until aliasEnd)
        }
        index = aliasEnd + 1
    }
    return aliases
}

private fun String.ctasIndexOfKeyword(
    keyword: String,
    startOffset: Int,
    endOffset: Int,
): Int? {
    var index = startOffset
    while (index < endOffset) {
        index = indexOf(keyword, startIndex = index, ignoreCase = true)
        if (index == -1 || index >= endOffset) return null
        if (ctasIsBoundary(index - 1) && ctasIsBoundary(index + keyword.length)) return index
        index += keyword.length
    }
    return null
}

private fun String.ctasIndexOfTopLevelKeyword(
    keyword: String,
    startOffset: Int,
    endOffset: Int,
): Int? {
    var index = startOffset
    var depth = 0
    while (index < endOffset) {
        when (this[index]) {
            '(' -> depth++
            ')' -> depth--
        }
        if (depth == 0 && regionMatches(index, keyword, 0, keyword.length, ignoreCase = true)) {
            if (ctasIsBoundary(index - 1) && ctasIsBoundary(index + keyword.length)) return index
        }
        index++
    }
    return null
}

private fun String.ctasSkipWhitespace(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = startOffset
    while (index < endOffset && this[index].isWhitespace()) index++
    return index
}

private fun String.ctasSkipWhitespaceAndCommas(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = startOffset
    while (index < endOffset && (this[index].isWhitespace() || this[index] == ',')) index++
    return index
}

private fun String.ctasIdentifierEnd(
    startOffset: Int,
    endOffset: Int,
): Int {
    if (startOffset >= endOffset) return startOffset
    if (this[startOffset] == '"') {
        val end = skipSqlDoubleQuotedIdentifier(startOffset)
        return if (end <= endOffset) end else startOffset
    }
    var index = startOffset
    while (index < endOffset && (this[index].isLetterOrDigit() || this[index] == '_' || this[index] == '$' || this[index] == '#')) {
        index++
    }
    return index
}

private fun String.ctasIsBoundary(index: Int): Boolean = index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_')
