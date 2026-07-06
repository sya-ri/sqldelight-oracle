package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidDmlWaitClauseRuleTest :
    FunSpec({
        test("reports invalid dml wait values") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    UPDATE customer
                    SET name = :name
                    WHERE id = :id
                    WAIT seconds;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_WAIT_MESSAGE,
                        startLine = 4,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 13,
                    ),
                )
        }

        test("reports negative dml wait values") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    DELETE FROM customer
                    WHERE id = :id
                    WAIT -1;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_WAIT_MESSAGE,
                        startLine = 3,
                        startColumn = 1,
                        endLine = 3,
                        endColumn = 8,
                    ),
                )
        }

        test("accepts documented dml wait clauses") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    UPDATE customer SET name = :name WAIT 5;
                    DELETE FROM customer WHERE id = :id WAIT FOREVER;
                    MERGE INTO customer target
                    USING incoming source
                    ON (target.id = source.id)
                    WHEN MATCHED THEN UPDATE SET name = source.name
                    WAIT 10 SECONDS;
                    INSERT INTO customer (id, name) VALUES (:id, :name) NOWAIT;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("does not report wait column assignments") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    UPDATE customer
                    SET wait = :wait,
                        retry_after = wait + 1
                    WHERE id = :id;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("does not report wait table or column names") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    DELETE FROM wait;
                    INSERT INTO wait (id, wait) VALUES (:id, :wait);
                    MERGE INTO wait target
                    USING source wait
                    ON (target.id = wait.id)
                    WHEN MATCHED THEN UPDATE SET target.wait = wait.wait;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("reports invalid dml wait values after a SQLDelight query label") {
            ValidDmlWaitClauseRule()
                .diagnostics(
                    """
                    updateCustomer:
                    UPDATE customer
                    SET name = :name
                    WHERE id = :id
                    WAIT seconds;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_WAIT_MESSAGE,
                        startLine = 5,
                        startColumn = 1,
                        endLine = 5,
                        endColumn = 13,
                    ),
                )
        }
    })

private const val DML_WAIT_MESSAGE = "Use a non-negative static value in Oracle DML WAIT clauses."
