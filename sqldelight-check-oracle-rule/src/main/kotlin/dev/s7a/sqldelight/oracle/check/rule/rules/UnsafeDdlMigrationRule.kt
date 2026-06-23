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
        content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .filter { statement -> statement.isUnsafeMigrationDdl() }
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

private fun List<SqlToken>.isUnsafeMigrationDdl(): Boolean =
    when {
        startsWithStatement("TRUNCATE", "TABLE") -> true
        startsWithStatement("ALTER", "TABLE") -> hasUnsafeAlterTableOperation()
        else -> false
    }

private fun List<SqlToken>.hasUnsafeAlterTableOperation(): Boolean =
    containsSequence("DROP", "COLUMN") ||
        containsSequence("DROP", "COLUMNS") ||
        containsSequence("DROP", "UNUSED", "COLUMNS") ||
        containsSequence("SET", "UNUSED", "COLUMN") ||
        containsSequence("SET", "UNUSED", "COLUMNS") ||
        hasUnsafePartitionMaintenanceOperation() ||
        containsSequence("SHRINK", "SPACE") ||
        any { token -> token.hasText("MOVE") } ||
        changesRequiredColumnWithoutDefault()

private fun List<SqlToken>.hasUnsafePartitionMaintenanceOperation(): Boolean =
    partitionMaintenanceOperations.any { operation ->
        containsSequence(operation, "PARTITION") ||
            containsSequence(operation, "PARTITIONS") ||
            containsSequence(operation, "SUBPARTITION") ||
            containsSequence(operation, "SUBPARTITIONS")
    }

private fun List<SqlToken>.changesRequiredColumnWithoutDefault(): Boolean =
    any { token -> token.hasText("ADD") || token.hasText("MODIFY") } &&
        containsSequence("NOT", "NULL") &&
        none { token -> token.hasText("DEFAULT") }

private fun List<SqlToken>.startsWithStatement(
    first: String,
    second: String,
): Boolean = getOrNull(0).hasText(first) && getOrNull(1).hasText(second)

private fun List<SqlToken>.containsSequence(vararg terms: String): Boolean =
    windowed(size = terms.size).any { tokens ->
        tokens.map { token -> token.text.lowercase() } == terms.map { term -> term.lowercase() }
    }

private fun SqlToken?.hasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private val partitionMaintenanceOperations = setOf("DROP", "TRUNCATE", "MOVE", "MERGE", "SPLIT", "EXCHANGE")
