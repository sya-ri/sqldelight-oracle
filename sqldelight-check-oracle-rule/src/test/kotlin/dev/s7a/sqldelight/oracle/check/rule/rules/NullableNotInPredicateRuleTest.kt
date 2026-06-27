package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NullableNotInPredicateRuleTest :
    FunSpec({
        test("reports NOT IN subquery without explicit null filtering") {
            val diagnostics =
                NullableNotInPredicateRule().diagnostics(
                    """
                    SELECT *
                    FROM customer
                    WHERE id NOT IN (
                        SELECT customer_id
                        FROM invoice
                    );
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NULLABLE_NOT_IN_MESSAGE,
                        startLine = 3,
                        startColumn = 10,
                        endLine = 3,
                        endColumn = 16,
                    ),
                )
        }

        test("reports NOT IN subquery when only the outer query filters nulls") {
            val diagnostics =
                NullableNotInPredicateRule().diagnostics(
                    """
                    SELECT *
                    FROM customer
                    WHERE id NOT IN (
                        SELECT customer_id
                        FROM invoice
                    )
                    AND status IS NOT NULL;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NULLABLE_NOT_IN_MESSAGE,
                        startLine = 3,
                        startColumn = 10,
                        endLine = 3,
                        endColumn = 16,
                    ),
                )
        }

        test("accepts NOT IN subquery with explicit null filtering") {
            NullableNotInPredicateRule().diagnostics(
                """
                SELECT *
                FROM customer
                WHERE id NOT IN (
                    SELECT customer_id
                    FROM invoice
                    WHERE customer_id IS NOT NULL
                );
                """,
            ) shouldBe emptyList()
        }

        test("accepts NOT IN literal lists") {
            NullableNotInPredicateRule().diagnostics(
                """
                SELECT *
                FROM customer
                WHERE id NOT IN (1, 2, 3);
                """,
            ) shouldBe emptyList()
        }

        test("ignores NOT IN in comments and string literals") {
            NullableNotInPredicateRule().diagnostics(
                """
                -- WHERE id NOT IN (SELECT customer_id FROM invoice)
                /* WHERE id NOT IN (SELECT customer_id FROM invoice) */
                SELECT /*+ FULL(invoice) */ 'NOT IN (SELECT customer_id FROM invoice)' AS label
                FROM dual;
                """,
            ) shouldBe emptyList()
        }

        test("ignores NOT IN in multiline block comments") {
            NullableNotInPredicateRule().diagnostics(
                """
                /*
                  WHERE id NOT IN (
                    SELECT customer_id
                    FROM invoice
                  )
                */
                SELECT 'NOT IN (SELECT customer_id FROM invoice)' AS label
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })

private const val NULLABLE_NOT_IN_MESSAGE =
    "Filter nullable values inside Oracle NOT IN subqueries with IS NOT NULL, or use NOT EXISTS."
