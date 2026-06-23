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
 * Reports mutually exclusive Oracle CREATE VIEW clauses.
 */
public class NoConflictingCreateViewClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-create-view-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskCreateViewCommentsAndQuotedTextPreservingOffsets()
            .createViewStatements()
            .flatMap { statement -> statement.conflictingCreateViewClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle CREATE VIEW clauses: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class CreateViewToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class CreateViewOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class CreateViewConflict(
    val group: String,
    val first: CreateViewOccurrence,
    val second: CreateViewOccurrence,
)

private fun String.createViewStatements(): List<List<CreateViewToken>> {
    val statements = mutableListOf<List<CreateViewToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).createViewTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<CreateViewToken>.conflictingCreateViewClauses(): List<CreateViewConflict> {
    if (!isCreateViewStatement()) return emptyList()
    val firstByGroup = linkedMapOf<String, CreateViewOccurrence>()
    return createViewOccurrences().mapNotNull { occurrence ->
        val first = firstByGroup.putIfAbsent(occurrence.group, occurrence)
        first?.let {
            CreateViewConflict(
                group = occurrence.group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<CreateViewToken>.isCreateViewStatement(): Boolean =
    getOrNull(0).createViewHasText("CREATE") && take(10).any { token -> token.createViewHasText("VIEW") }

private fun List<CreateViewToken>.createViewOccurrences(): List<CreateViewOccurrence> =
    takeWhile { token -> !token.createViewHasText("AS") }
        .mapIndexedNotNull { index, token ->
            when {
                token.createViewHasText("FORCE") && !getOrNull(index - 1).createViewHasText("NO") -> {
                    token.createViewOccurrence("FORCE/NO FORCE")
                }

                token.createViewHasText("NO") && getOrNull(index + 1).createViewHasText("FORCE") -> {
                    createViewOccurrence("FORCE/NO FORCE", index, index + 1)
                }

                token.createViewHasText("EDITIONING") || token.createViewHasText("NONEDITIONING") -> {
                    token.createViewOccurrence("EDITIONING/NONEDITIONING")
                }

                token.createViewHasText("EDITIONABLE") || token.createViewHasText("NONEDITIONABLE") -> {
                    token.createViewOccurrence("EDITIONABLE/NONEDITIONABLE")
                }

                else -> {
                    null
                }
            }
        }

private fun CreateViewToken.createViewOccurrence(group: String): CreateViewOccurrence =
    CreateViewOccurrence(
        group = group,
        startOffset = startOffset,
        endOffset = endOffset,
    )

private fun List<CreateViewToken>.createViewOccurrence(
    group: String,
    startIndex: Int,
    endIndex: Int,
): CreateViewOccurrence =
    CreateViewOccurrence(
        group = group,
        startOffset = this[startIndex].startOffset,
        endOffset = this[endIndex].endOffset,
    )

private val createViewTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.createViewTokens(offset: Int): List<CreateViewToken> =
    createViewTokenPattern
        .findAll(this)
        .map { match ->
            CreateViewToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskCreateViewCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> createViewMaskRange(chars, index, skipCreateViewLineComment(index))
                startsWith("/*", index) -> createViewMaskRange(chars, index, skipCreateViewBlockComment(index))
                chars[index] == '\'' -> createViewMaskRange(chars, index, skipCreateViewQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipCreateViewLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipCreateViewBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipCreateViewQuotedString(start: Int): Int {
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

private fun createViewMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun CreateViewToken?.createViewHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
