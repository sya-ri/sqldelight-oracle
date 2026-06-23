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
 * Reports conflicting Oracle JSON storage clauses for the same column.
 */
public class NoConflictingJsonStorageClausesRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-json-storage-clauses")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskSqlCommentsAndQuotedTextPreservingOffsets()
            .jsonStorageStatements()
            .flatMap { statement -> statement.conflictingJsonStorageClauses() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid multiple Oracle JSON storage clauses for column ${conflict.columnName}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class JsonStorageToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class JsonStorageOccurrence(
    val columnName: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class JsonStorageConflict(
    val columnName: String,
    val first: JsonStorageOccurrence,
    val second: JsonStorageOccurrence,
)

private fun String.jsonStorageStatements(): List<List<JsonStorageToken>> {
    val statements = mutableListOf<List<JsonStorageToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).jsonStorageTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<JsonStorageToken>.conflictingJsonStorageClauses(): List<JsonStorageConflict> {
    if (!isTableStatement()) return emptyList()
    val firstByColumn = linkedMapOf<String, JsonStorageOccurrence>()
    return jsonStorageOccurrences().mapNotNull { occurrence ->
        val first = firstByColumn.putIfAbsent(occurrence.columnName.lowercase(), occurrence)
        first?.let {
            JsonStorageConflict(
                columnName = occurrence.columnName,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<JsonStorageToken>.isTableStatement(): Boolean =
    (getOrNull(0).jsonStorageHasText("CREATE") && take(8).any { token -> token.jsonStorageHasText("TABLE") }) ||
        (getOrNull(0).jsonStorageHasText("ALTER") && getOrNull(1).jsonStorageHasText("TABLE"))

private fun List<JsonStorageToken>.jsonStorageOccurrences(): List<JsonStorageOccurrence> =
    buildList {
        this@jsonStorageOccurrences.forEachIndexed { index, token ->
            when {
                token.jsonStorageHasText("JSON") &&
                    this@jsonStorageOccurrences.getOrNull(index + 1).jsonStorageHasText("COLUMN") &&
                    this@jsonStorageOccurrences.getOrNull(index + 3).jsonStorageHasText("STORE") &&
                    this@jsonStorageOccurrences.getOrNull(index + 4).jsonStorageHasText("AS") -> {
                    add(
                        JsonStorageOccurrence(
                            columnName = this@jsonStorageOccurrences[index + 2].text,
                            startOffset = token.startOffset,
                            endOffset =
                                this@jsonStorageOccurrences.getOrNull(index + 5)?.endOffset
                                    ?: this@jsonStorageOccurrences[index + 4].endOffset,
                        ),
                    )
                }

                token.jsonStorageHasText("JSON") &&
                    this@jsonStorageOccurrences.getOrNull(index + 1).jsonStorageHasText("(") -> {
                    addAll(jsonStorageColumnListOccurrence(index))
                }
            }
        }
    }

private fun List<JsonStorageToken>.jsonStorageColumnListOccurrence(index: Int): List<JsonStorageOccurrence> {
    val closeIndex = (index + 2 until size).firstOrNull { candidate -> get(candidate).jsonStorageHasText(")") } ?: return emptyList()
    if (!getOrNull(closeIndex + 1).jsonStorageHasText("STORE") || !getOrNull(closeIndex + 2).jsonStorageHasText("AS")) {
        return emptyList()
    }
    val endOffset = getOrNull(closeIndex + 3)?.endOffset ?: get(closeIndex + 2).endOffset
    return (index + 2 until closeIndex)
        .filter { candidate -> !get(candidate).jsonStorageHasText(",") }
        .map { candidate ->
            JsonStorageOccurrence(
                columnName = get(candidate).text,
                startOffset = get(index).startOffset,
                endOffset = endOffset,
            )
        }
}

private val jsonStorageTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\(|\)|,|;""")

private fun String.jsonStorageTokens(offset: Int): List<JsonStorageToken> =
    jsonStorageTokenPattern
        .findAll(this)
        .map { match ->
            JsonStorageToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun JsonStorageToken?.jsonStorageHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
