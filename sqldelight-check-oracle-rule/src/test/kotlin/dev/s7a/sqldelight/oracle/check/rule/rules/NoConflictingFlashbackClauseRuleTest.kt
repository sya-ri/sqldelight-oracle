package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingFlashbackClauseRuleTest :
    FunSpec({
        test("reports duplicate AS OF flashback clauses exactly") {
            NoConflictingFlashbackClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders AS OF SCN 100 AS OF TIMESTAMP created_at
                    WHERE id = ?;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX AS OF.",
                        startLine = 2,
                        startColumn = 13,
                        endLine = 2,
                        endColumn = 32,
                    ),
                )
        }

        test("reports duplicate VERSIONS flashback clauses exactly") {
            NoConflictingFlashbackClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE VERSIONS PERIOD FOR valid_time BETWEEN TIMESTAMP '2024-01-01 00:00:00' AND TIMESTAMP '2024-12-31 23:59:59'
                    WHERE id = ?;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX VERSIONS.",
                        startLine = 2,
                        startColumn = 13,
                        endLine = 2,
                        endColumn = 71,
                    ),
                )
        }

        test("accepts documented combined VERSIONS and AS OF flashback clauses") {
            NoConflictingFlashbackClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE AS OF SCN 123456
                    WHERE id = ?;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("accepts flashback clauses on separate table references") {
            NoConflictingFlashbackClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders AS OF SCN 100 o
                    JOIN customers AS OF SCN 100 c ON c.id = o.customer_id
                    WHERE o.id = ?;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores flashback text in comments and strings") {
            NoConflictingFlashbackClauseRule()
                .diagnostics(
                    """
                    -- FROM orders AS OF SCN 100 AS OF SCN 200
                    SELECT 'AS OF SCN 100 AS OF SCN 200' AS flashback_text
                    FROM orders AS OF SCN 100;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX = "Avoid duplicate Oracle flashback query clauses:"
