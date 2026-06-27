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
 * Reports migration DDL that can rewrite, lock, or destructively change large Oracle tables.
 */
public class UnsafeDdlMigrationRule : Rule {
    override val id: RuleId = RuleId("unsafe-ddl-migration")
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
            .filter { statement -> statement.isUnsafeMigrationDdl(maskedContent) }
            .forEach { statement ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Review Oracle migration DDL that can rewrite, lock, or destructively change large tables.",
                        file = context.file,
                        range = content.rangeAtOffsets(statement[0].startOffset, statement[1].endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.isUnsafeMigrationDdl(maskedContent: String): Boolean =
    when {
        startsWithStatement("DROP", "TABLE") -> true
        startsWithStatement("DROP", "MATERIALIZED", "VIEW") && !getOrNull(3).hasText("LOG") -> true
        startsWithStatement("TRUNCATE", "TABLE") -> true
        startsWithStatement("TRUNCATE", "CLUSTER") -> true
        startsWithStatement("ALTER", "TABLE") -> hasUnsafeAlterTableOperation(maskedContent)
        else -> false
    }

private fun List<SqlToken>.hasUnsafeAlterTableOperation(maskedContent: String): Boolean =
    containsSequence("DROP", "COLUMN") ||
        containsSequence("DROP", "COLUMNS") ||
        containsSequence("DROP", "UNUSED", "COLUMNS") ||
        containsSequence("SET", "UNUSED", "COLUMN") ||
        containsSequence("SET", "UNUSED", "COLUMNS") ||
        hasUnsafePartitionMaintenanceOperation() ||
        containsSequence("SHRINK", "SPACE") ||
        any { token -> token.hasText("MOVE") } ||
        changesRequiredColumnWithoutDefault(maskedContent)

private fun List<SqlToken>.hasUnsafePartitionMaintenanceOperation(): Boolean =
    partitionMaintenanceOperations.any { operation ->
        containsSequence(operation, "PARTITION") ||
            containsSequence(operation, "PARTITIONS") ||
            containsSequence(operation, "SUBPARTITION") ||
            containsSequence(operation, "SUBPARTITIONS")
    }

private fun List<SqlToken>.changesRequiredColumnWithoutDefault(maskedContent: String): Boolean {
    val statementEndOffset = last().endOffset
    return filter { token -> token.hasText("ADD") || token.hasText("MODIFY") }
        .any { operation ->
            maskedContent
                .columnDefinitionRangesAfter(operation.endOffset, statementEndOffset)
                .any { range -> maskedContent.isRequiredColumnWithoutDefault(range) }
        }
}

private fun List<SqlToken>.startsWithStatement(vararg terms: String): Boolean =
    take(terms.size).map { token -> token.text.lowercase() } == terms.map { term -> term.lowercase() }

private fun List<SqlToken>.containsSequence(vararg terms: String): Boolean =
    windowed(size = terms.size).any { tokens ->
        tokens.map { token -> token.text.lowercase() } == terms.map { term -> term.lowercase() }
    }

private fun String.columnDefinitionRangesAfter(
    operationEndOffset: Int,
    statementEndOffset: Int,
): List<IntRange> {
    val start = skipUnsafeDdlWhitespace(operationEndOffset, statementEndOffset)
    if (start >= statementEndOffset) return emptyList()
    if (this[start] == '(') {
        val end = matchingSqlParenthesis(start) ?: statementEndOffset
        return topLevelUnsafeDdlItemRanges(start + 1, end)
    }
    return listOf(start until statementEndOffset)
}

private fun String.topLevelUnsafeDdlItemRanges(
    startOffset: Int,
    endOffset: Int,
): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var itemStart = startOffset
    var depth = 0
    for (index in startOffset until endOffset) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                if (depth > 0) depth--
            }

            ',' -> {
                if (depth == 0) {
                    ranges += itemStart until index
                    itemStart = index + 1
                }
            }
        }
    }
    ranges += itemStart until endOffset
    return ranges
}

private fun String.isRequiredColumnWithoutDefault(range: IntRange): Boolean {
    val tokens = unsafeDdlTokens(range)
    val firstToken = tokens.firstOrNull()?.uppercase() ?: return false
    val columnStartIndex =
        when {
            firstToken == "COLUMN" -> 1
            firstToken in nonColumnAlterTableItems -> return false
            else -> 0
        }
    val columnTokens = tokens.drop(columnStartIndex)
    return columnTokens.containsAdjacent("NOT", "NULL") &&
        columnTokens.none { token -> token.equals("DEFAULT", ignoreCase = true) }
}

private fun String.unsafeDdlTokens(range: IntRange): List<String> =
    Regex("""[A-Za-z_][A-Za-z0-9_$#]*""")
        .findAll(this, range.first)
        .takeWhile { match -> match.range.first <= range.last }
        .map { match -> match.value }
        .toList()

private fun List<String>.containsAdjacent(
    first: String,
    second: String,
): Boolean =
    windowed(size = 2).any { tokens ->
        tokens[0].equals(first, ignoreCase = true) && tokens[1].equals(second, ignoreCase = true)
    }

private fun String.skipUnsafeDdlWhitespace(
    startOffset: Int,
    endOffset: Int,
): Int {
    var index = startOffset
    while (index < endOffset && this[index].isWhitespace()) index++
    return index
}

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private val partitionMaintenanceOperations = setOf("DROP", "TRUNCATE", "MOVE", "MERGE", "SPLIT", "EXCHANGE")

private val nonColumnAlterTableItems =
    setOf(
        "CONSTRAINT",
        "PRIMARY",
        "UNIQUE",
        "FOREIGN",
        "CHECK",
        "PERIOD",
        "SUPPLEMENTAL",
        "OVERFLOW",
    )
