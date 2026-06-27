package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidValuesAliasColumnCountRuleTest :
    FunSpec({
        test("reports VALUES alias column count mismatches exactly") {
            val diagnostics =
                ValidValuesAliasColumnCountRule().diagnostics(
                    """
                    invalidValuesAlias:
                    SELECT source.order_id, source.order_total
                    FROM (VALUES (1, 100), (2, 200)) source(order_id);
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle VALUES alias declares 1 column(s), but VALUES rows have 2.",
                        startLine = 3,
                        startColumn = 40,
                        endLine = 3,
                        endColumn = 50,
                    ),
                )
        }

        test("reports VALUES quoted alias column count mismatches exactly") {
            val diagnostics =
                ValidValuesAliasColumnCountRule().diagnostics(
                    """
                    invalidValuesAlias:
                    SELECT source.order_id, source.order_total
                    FROM (VALUES (1, 100), (2, 200)) "source"(order_id);
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle VALUES alias declares 1 column(s), but VALUES rows have 2.",
                        startLine = 3,
                        startColumn = 42,
                        endLine = 3,
                        endColumn = 52,
                    ),
                )
        }

        test("reports VALUES row arity mismatches exactly") {
            val diagnostics =
                ValidValuesAliasColumnCountRule().diagnostics(
                    """
                    invalidValuesRows:
                    SELECT source.order_id, source.order_total
                    FROM (VALUES (1, 100), (2, 200, 300)) source(order_id, order_total);
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle VALUES row 2 has 3 column(s), but the first row has 2.",
                        startLine = 3,
                        startColumn = 24,
                        endLine = 3,
                        endColumn = 37,
                    ),
                )
        }

        test("accepts matching VALUES aliases exactly") {
            ValidValuesAliasColumnCountRule().diagnostics(
                """
                validValuesAlias:
                SELECT source.order_id, source.order_total
                FROM (VALUES (1, 100), (2, 200)) source(order_id, order_total);
                """,
            ) shouldBe emptyList()
        }

        test("ignores VALUES text inside comments and strings exactly") {
            ValidValuesAliasColumnCountRule().diagnostics(
                """
                -- FROM (VALUES (1, 100)) source(order_id)
                SELECT 'VALUES (1, 100)' AS sql_text
                FROM (VALUES (1, 100)) source(order_id, order_total);
                """,
            ) shouldBe emptyList()
        }
    })
