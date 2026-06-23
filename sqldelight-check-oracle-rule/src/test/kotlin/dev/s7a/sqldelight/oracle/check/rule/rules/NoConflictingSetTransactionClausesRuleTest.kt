package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingSetTransactionClausesRuleTest :
    FunSpec({
        test("reports conflicting set transaction read modes exactly") {
            NoConflictingSetTransactionClausesRule()
                .diagnostics(
                    """
                    SET TRANSACTION READ ONLY READ WRITE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET TRANSACTION clauses: READ MODE.",
                        startLine = 1,
                        startColumn = 17,
                        endLine = 1,
                        endColumn = 37,
                    ),
                )
        }

        test("reports repeated set transaction isolation levels exactly") {
            NoConflictingSetTransactionClausesRule()
                .diagnostics(
                    """
                    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE ISOLATION LEVEL READ COMMITTED;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET TRANSACTION clauses: ISOLATION LEVEL.",
                        startLine = 1,
                        startColumn = 17,
                        endLine = 1,
                        endColumn = 76,
                    ),
                )
        }

        test("accepts one set transaction clause per group") {
            NoConflictingSetTransactionClausesRule()
                .diagnostics(
                    """
                    SET TRANSACTION READ ONLY ISOLATION LEVEL SERIALIZABLE;
                    SET TRANSACTION ROLLBACK SEGMENT rb1;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
