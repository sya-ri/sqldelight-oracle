package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleOptions
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectSourcePatterns

internal fun Rule.diagnostics(content: String): List<RuleDiagnostic> {
    val diagnostics = mutableListOf<RuleDiagnostic>()
    run(
        context =
            object : RuleContext {
                override val database: DatabaseContext =
                    DatabaseContext(
                        name = "Database",
                        dialect =
                            SqlDialect(
                                ids = setOf(OracleDialectId),
                                sourcePatterns = OracleDialectSourcePatterns,
                            ),
                    )
                override val file: SourceFile =
                    SourceFile(
                        path = "src/main/sqldelight/com/example/Query.sq",
                        content = content.trimIndent() + "\n",
                    )
                override val options: RuleOptions = RuleOptions()
                override val facts: SqlFacts = SqlFacts()
            },
        reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic },
    )
    return diagnostics
}

internal data class DiagnosticSummary(
    val message: String,
    val startLine: Int?,
    val startColumn: Int?,
    val endLine: Int?,
    val endColumn: Int?,
)

internal fun List<RuleDiagnostic>.summaries(): List<DiagnosticSummary> =
    map { diagnostic ->
        DiagnosticSummary(
            message = diagnostic.message,
            startLine = diagnostic.range?.start?.line,
            startColumn = diagnostic.range?.start?.column,
            endLine = diagnostic.range?.end?.line,
            endColumn = diagnostic.range?.end?.column,
        )
    }
