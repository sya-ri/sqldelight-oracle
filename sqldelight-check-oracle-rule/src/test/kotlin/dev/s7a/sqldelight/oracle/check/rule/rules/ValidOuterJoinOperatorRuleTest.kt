package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidOuterJoinOperatorRuleTest :
    FunSpec({
        test("reports legacy outer join operator with OR exactly") {
            ValidOuterJoinOperatorRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders o, customers c
                    WHERE o.customer_id = c.id(+) OR c.status = 'ACTIVE';
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = OUTER_JOIN_MESSAGE,
                        startLine = 3,
                        startColumn = 7,
                        endLine = 3,
                        endColumn = 42,
                    ),
                )
        }

        test("reports legacy outer join operator with IN exactly") {
            ValidOuterJoinOperatorRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders o, customers c
                    WHERE c.id(+) IN (o.customer_id, o.previous_customer_id);
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = OUTER_JOIN_MESSAGE,
                        startLine = 3,
                        startColumn = 7,
                        endLine = 3,
                        endColumn = 57,
                    ),
                )
        }

        test("reports legacy outer join operator across AND before OR") {
            ValidOuterJoinOperatorRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders o, customers c
                    WHERE o.customer_id = c.id(+)
                      AND c.region(+) = o.region
                       OR c.status = 'ACTIVE';
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = OUTER_JOIN_MESSAGE,
                        startLine = 3,
                        startColumn = 7,
                        endLine = 5,
                        endColumn = 15,
                    ),
                )
        }

        test("accepts legacy outer join operator in simple comparisons") {
            ValidOuterJoinOperatorRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders o, customers c
                    WHERE o.customer_id = c.id(+)
                      AND c.region(+) = o.region;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores outer join text in comments and strings") {
            ValidOuterJoinOperatorRule()
                .diagnostics(
                    """
                    -- WHERE o.customer_id = c.id(+) OR c.status = 'ACTIVE'
                    SELECT '(+) OR IN' AS sql_text
                    FROM orders o, customers c
                    WHERE o.customer_id = c.id(+);
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val OUTER_JOIN_MESSAGE =
    "Avoid Oracle legacy outer join operator (+) with OR or IN conditions."
