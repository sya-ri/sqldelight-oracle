package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingCommitWriteClausesRuleTest :
    FunSpec({
        test("reports conflicting commit wait clauses exactly") {
            NoConflictingCommitWriteClausesRule()
                .diagnostics(
                    """
                    COMMIT WRITE WAIT NOWAIT;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle COMMIT WRITE clauses: WAIT.",
                        startLine = 1,
                        startColumn = 14,
                        endLine = 1,
                        endColumn = 25,
                    ),
                )
        }

        test("reports conflicting commit write modes exactly") {
            NoConflictingCommitWriteClausesRule()
                .diagnostics(
                    """
                    COMMIT WRITE IMMEDIATE BATCH;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle COMMIT WRITE clauses: MODE.",
                        startLine = 1,
                        startColumn = 14,
                        endLine = 1,
                        endColumn = 29,
                    ),
                )
        }

        test("reports repeated commit write clauses exactly") {
            NoConflictingCommitWriteClausesRule()
                .diagnostics(
                    """
                    COMMIT WRITE WAIT WRITE IMMEDIATE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle COMMIT WRITE clauses: WRITE.",
                        startLine = 1,
                        startColumn = 8,
                        endLine = 1,
                        endColumn = 24,
                    ),
                )
        }

        test("accepts one valid commit write clause") {
            NoConflictingCommitWriteClausesRule()
                .diagnostics(
                    """
                    COMMIT WRITE WAIT IMMEDIATE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
