package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidForUpdateWaitClauseRuleTest :
    FunSpec({
        test("reports negative for update wait values") {
            ValidForUpdateWaitClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customer
                    FOR UPDATE WAIT -5;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = FOR_UPDATE_WAIT_MESSAGE,
                        startLine = 3,
                        startColumn = 12,
                        endLine = 3,
                        endColumn = 19,
                    ),
                )
        }

        test("reports negative for update wait values after column lists") {
            ValidForUpdateWaitClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customer c
                    FOR UPDATE OF c.name WAIT -1;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = FOR_UPDATE_WAIT_MESSAGE,
                        startLine = 3,
                        startColumn = 22,
                        endLine = 3,
                        endColumn = 29,
                    ),
                )
        }

        test("reports invalid static for update wait values") {
            ValidForUpdateWaitClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customer
                    FOR UPDATE WAIT seconds;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = FOR_UPDATE_WAIT_MESSAGE,
                        startLine = 3,
                        startColumn = 12,
                        endLine = 3,
                        endColumn = 24,
                    ),
                )
        }

        test("accepts documented for update wait clauses") {
            ValidForUpdateWaitClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customer
                    FOR UPDATE WAIT 5;

                    SELECT *
                    FROM customer
                    FOR UPDATE WAIT FOREVER;

                    SELECT *
                    FROM customer
                    FOR UPDATE NOWAIT;

                    SELECT *
                    FROM customer
                    FOR UPDATE SKIP LOCKED;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores for update wait text in comments and strings") {
            ValidForUpdateWaitClauseRule()
                .diagnostics(
                    """
                    -- FOR UPDATE WAIT -5
                    SELECT 'FOR UPDATE WAIT -5' AS sql_text
                    FROM customer
                    FOR UPDATE WAIT 0;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val FOR_UPDATE_WAIT_MESSAGE =
    "Use a non-negative static value in Oracle FOR UPDATE WAIT clauses."
