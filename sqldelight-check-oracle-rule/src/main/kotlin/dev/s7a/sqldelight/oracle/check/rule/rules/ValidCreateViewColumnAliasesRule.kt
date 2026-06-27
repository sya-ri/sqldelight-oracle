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
 * Reports static Oracle CREATE VIEW column alias list count and duplicate problems.
 */
public class ValidCreateViewColumnAliasesRule : Rule {
    override val id: RuleId = RuleId("valid-create-view-column-aliases")
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
        maskedContent.createViewAliasChecks().forEach { check ->
            check.duplicateAlias()?.let { duplicate ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Oracle CREATE VIEW column alias '${duplicate.text}' is declared more than once.",
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
                            "Oracle CREATE VIEW declares ${check.aliases.size} column alias(es), " +
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

private data class ViewAlias(
    val text: String,
    val range: IntRange,
)

private data class CreateViewAliasCheck(
    val aliases: List<ViewAlias>,
    val aliasListRange: IntRange,
    val selectColumnCount: Int,
) {
    fun duplicateAlias(): ViewAlias? {
        val seen = mutableSetOf<String>()
        return aliases.firstOrNull { alias -> !seen.add(alias.text.createViewAliasKey()) }
    }
}

private fun String.createViewAliasKey(): String =
    if (startsWith("\"") && endsWith("\"")) {
        removeSurrounding("\"").replace("\"\"", "\"")
    } else {
        uppercase()
    }

private fun String.createViewAliasChecks(): List<CreateViewAliasCheck> {
    val checks = mutableListOf<CreateViewAliasCheck>()
    createViewStatementRanges().forEach { statementRange ->
        parseCreateViewAliasCheck(statementRange)?.let { checks += it }
    }
    return checks
}

private fun String.createViewStatementRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        val createOffset = indexOfKeyword("CREATE", start, end)
        val viewOffset = createOffset?.let { indexOfKeyword("VIEW", it + "CREATE".length, end) }
        if (viewOffset != null) {
            ranges += start until end
        }
        start = end
    }
    return ranges
}

private fun String.parseCreateViewAliasCheck(statementRange: IntRange): CreateViewAliasCheck? {
    val asOffset = indexOfKeyword("AS", statementRange.first, statementRange.last + 1) ?: return null
    val aliasListEnd = lastIndexOf(')', startIndex = asOffset).takeIf { it in statementRange } ?: return null
    val aliasListStart = matchingOpenParenthesis(aliasListEnd) ?: return null
    val aliases = aliasesIn(aliasListStart + 1, aliasListEnd)
    if (aliases.isEmpty()) return null

    val selectOffset = indexOfKeyword("SELECT", asOffset, statementRange.last + 1) ?: return null
    val fromOffset = indexOfTopLevelKeyword("FROM", selectOffset + "SELECT".length, statementRange.last + 1) ?: return null
    val selectColumnCount = countTopLevelSqlItems(selectOffset + "SELECT".length, fromOffset)
    return CreateViewAliasCheck(
        aliases = aliases,
        aliasListRange = aliasListStart..aliasListEnd,
        selectColumnCount = selectColumnCount,
    )
}

private fun String.aliasesIn(
    startOffset: Int,
    endOffset: Int,
): List<ViewAlias> {
    val aliases = mutableListOf<ViewAlias>()
    viewColumnItems(startOffset, endOffset).forEach { item ->
        val index = skipCreateViewWhitespace(item.first, item.last + 1)
        val startsWithConstraint =
            regionMatches(
                index,
                "CONSTRAINT",
                0,
                "CONSTRAINT".length,
                ignoreCase = true,
            ) && isCreateViewBoundary(index + "CONSTRAINT".length)
        if (startsWithConstraint) {
            return@forEach
        }
        val aliasEnd = createViewIdentifierEnd(index, item.last + 1)
        if (aliasEnd > index) {
            aliases += ViewAlias(substring(index, aliasEnd), index until aliasEnd)
        }
    }
    return aliases
}

private fun String.viewColumnItems(
    startOffset: Int,
    endOffset: Int,
): List<IntRange> {
    val items = mutableListOf<IntRange>()
    var itemStart = startOffset
    var index = startOffset
    var depth = 0
    while (index < endOffset) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
            }

            ',' -> {
                if (depth == 0) {
                    items += itemStart until index
                    itemStart = index + 1
                }
            }
        }
        index++
    }
    items += itemStart until endOffset
    return items
}

private fun String.indexOfKeyword(
    keyword: String,
    startOffset: Int,
    endOffset: Int,
): Int? {
    var index = startOffset
    while (index < endOffset) {
        index = indexOf(keyword, startIndex = index, ignoreCase = true)
        if (index == -1 || index >= endOffset) return null
        if (isCreateViewBoundary(index - 1) && isCreateViewBoundary(index + keyword.length)) return index
        index += keyword.length
    }
    return null
}

private fun String.indexOfTopLevelKeyword(
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
            if (isCreateViewBoundary(index - 1) && isCreateViewBoundary(index + keyword.length)) return index
        }
        index++
    }
    return null
}

private fun String.matchingOpenParenthesis(closeOffset: Int): Int? {
    var depth = 0
    for (index in closeOffset downTo 0) {
        when (this[index]) {
            ')' -> {
                depth++
            }

            '(' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private fun String.skipCreateViewWhitespaceAndCommas(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = startOffset
    while (index < endOffset && (this[index].isWhitespace() || this[index] == ',')) index++
    return index
}

private fun String.skipCreateViewWhitespace(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = startOffset
    while (index < endOffset && this[index].isWhitespace()) index++
    return index
}

private fun String.createViewIdentifierEnd(
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

private fun String.isCreateViewBoundary(index: Int): Boolean = index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_')
