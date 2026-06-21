package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleOptions
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectSourcePatterns
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class NoEmptyStringComparisonRuleTest :
    FunSpec({
        test("reports equality with an empty string literal") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlank:
                    SELECT *
                    FROM customers
                    WHERE name = '';
                    """,
                )

            diagnostics shouldHaveSize 1
            diagnostics
                .single()
                .range
                ?.start
                ?.line shouldBe 4
        }

        test("reports inequality with an empty string literal") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                findPresent:
                SELECT *
                FROM customers
                WHERE name <> '';
                """,
            ) shouldHaveSize 1
        }

        test("accepts null predicates") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                findBlank:
                SELECT *
                FROM customers
                WHERE name IS NULL;
                """,
            ) shouldBe emptyList()
        }

        test("ignores commented comparisons") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                -- WHERE name = '';
                SELECT *
                FROM customers
                WHERE id = :id;
                """,
            ) shouldBe emptyList()
        }
    })

private fun NoEmptyStringComparisonRule.diagnostics(content: String): List<RuleDiagnostic> {
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
