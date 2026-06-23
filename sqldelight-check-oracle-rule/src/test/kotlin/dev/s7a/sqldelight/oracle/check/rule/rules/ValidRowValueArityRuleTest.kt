package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidRowValueArityRuleTest :
    FunSpec({
        test("reports row-value comparison arity mismatches exactly") {
            val diagnostics =
                ValidRowValueArityRule().diagnostics(
                    """
                    invalidRowComparison:
                    SELECT id
                    FROM orders
                    WHERE (customer_id, status) = (1, 'ACTIVE', 'EXTRA');
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle row value has 3 column(s), but the left row has 2.",
                        startLine = 4,
                        startColumn = 31,
                        endLine = 4,
                        endColumn = 53,
                    ),
                )
        }

        test("reports row-value IN list arity mismatches exactly") {
            val diagnostics =
                ValidRowValueArityRule().diagnostics(
                    """
                    invalidRowIn:
                    SELECT id
                    FROM orders
                    WHERE (customer_id, status) IN ((1, 'ACTIVE'), (2, 'PENDING', 'EXTRA'));
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle row value has 3 column(s), but the left row has 2.",
                        startLine = 4,
                        startColumn = 48,
                        endLine = 4,
                        endColumn = 71,
                    ),
                )
        }

        test("accepts matching row-value arity exactly") {
            ValidRowValueArityRule().diagnostics(
                """
                validRows:
                SELECT id
                FROM orders
                WHERE (customer_id, status) = (1, 'ACTIVE')
                  AND (customer_id, status) IN ((1, 'ACTIVE'), (2, 'PENDING'));
                """,
            ) shouldBe emptyList()
        }

        test("ignores row-value text inside comments and strings exactly") {
            ValidRowValueArityRule().diagnostics(
                """
                -- WHERE (a, b) = (1, 2, 3)
                SELECT '(a, b) = (1, 2, 3)' AS sql_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })
