package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidCreateViewColumnAliasesRuleTest :
    FunSpec({
        test("reports CREATE VIEW column alias count mismatches exactly") {
            val diagnostics =
                ValidCreateViewColumnAliasesRule().diagnostics(
                    """
                    CREATE VIEW order_summary(order_id, order_total) AS
                    SELECT id, total, status
                    FROM orders;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CREATE VIEW declares 2 column alias(es), but the SELECT list has 3 column(s).",
                        startLine = 1,
                        startColumn = 26,
                        endLine = 1,
                        endColumn = 49,
                    ),
                )
        }

        test("reports multiline CREATE VIEW column alias count mismatches") {
            val diagnostics =
                ValidCreateViewColumnAliasesRule().diagnostics(
                    """
                    CREATE OR REPLACE
                    VIEW order_summary(order_id, order_total) AS
                    SELECT id, total, status
                    FROM orders;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CREATE VIEW declares 2 column alias(es), but the SELECT list has 3 column(s).",
                        startLine = 2,
                        startColumn = 19,
                        endLine = 2,
                        endColumn = 42,
                    ),
                )
        }

        test("reports duplicate CREATE VIEW column aliases exactly") {
            val diagnostics =
                ValidCreateViewColumnAliasesRule().diagnostics(
                    """
                    CREATE VIEW order_summary(order_id, order_id) AS
                    SELECT id, total
                    FROM orders;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CREATE VIEW column alias 'order_id' is declared more than once.",
                        startLine = 1,
                        startColumn = 37,
                        endLine = 1,
                        endColumn = 45,
                    ),
                )
        }

        test("accepts matching CREATE VIEW column aliases exactly") {
            ValidCreateViewColumnAliasesRule().diagnostics(
                """
                CREATE VIEW order_summary(order_id, order_total) AS
                SELECT id, total
                FROM orders;
                """,
            ) shouldBe emptyList()
        }

        test("ignores CREATE VIEW text inside comments and strings exactly") {
            ValidCreateViewColumnAliasesRule().diagnostics(
                """
                -- CREATE VIEW broken(a) AS SELECT a, b FROM t;
                CREATE VIEW sql_text_view(sql_text) AS
                SELECT 'CREATE VIEW broken(a) AS SELECT a, b FROM t'
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })
