package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidRowLimitingClauseRuleTest :
    FunSpec({
        test("reports non-positive static row counts exactly") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders
                    OFFSET 0 ROWS FETCH NEXT -5 ROWS ONLY;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = POSITIVE_ROW_COUNT_MESSAGE,
                        startLine = 3,
                        startColumn = 1,
                        endLine = 3,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = POSITIVE_ROW_COUNT_MESSAGE,
                        startLine = 3,
                        startColumn = 26,
                        endLine = 3,
                        endColumn = 28,
                    ),
                )
        }

        test("reports out of range static row limiting percentages exactly") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders
                    FETCH FIRST 125 PERCENT ROWS ONLY;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = PERCENT_RANGE_MESSAGE,
                        startLine = 3,
                        startColumn = 13,
                        endLine = 3,
                        endColumn = 16,
                    ),
                )
        }

        test("reports WITH TIES without ORDER BY exactly") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders
                    FETCH FIRST 5 ROWS WITH TIES;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = WITH_TIES_ORDER_BY_MESSAGE,
                        startLine = 3,
                        startColumn = 20,
                        endLine = 3,
                        endColumn = 29,
                    ),
                )
        }

        test("reports WITH TIES when only a nested subquery has ORDER BY") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM (
                      SELECT *
                      FROM orders
                      ORDER BY created_at
                    )
                    FETCH FIRST 5 ROWS WITH TIES;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = WITH_TIES_ORDER_BY_MESSAGE,
                        startLine = 7,
                        startColumn = 20,
                        endLine = 7,
                        endColumn = 29,
                    ),
                )
        }

        test("accepts documented row limiting clauses") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM orders
                    ORDER BY created_at
                    OFFSET 10 ROWS FETCH NEXT 25 ROWS WITH TIES;

                    SELECT *
                    FROM orders
                    FETCH FIRST 50 PERCENT ROWS ONLY;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores dynamic row limiting expressions and comments") {
            ValidRowLimitingClauseRule()
                .diagnostics(
                    """
                    -- FETCH FIRST -1 ROWS ONLY
                    SELECT *
                    FROM orders
                    FETCH FIRST :limit ROWS ONLY;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val POSITIVE_ROW_COUNT_MESSAGE =
    "Use a positive static row count in Oracle row limiting clauses."

private const val PERCENT_RANGE_MESSAGE =
    "Use a static Oracle row limiting percentage from 0 through 100."

private const val WITH_TIES_ORDER_BY_MESSAGE =
    "Use ORDER BY with Oracle FETCH ... WITH TIES row limiting clauses."
