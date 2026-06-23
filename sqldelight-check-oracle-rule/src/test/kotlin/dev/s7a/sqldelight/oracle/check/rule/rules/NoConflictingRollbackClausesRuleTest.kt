package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingRollbackClausesRuleTest :
    FunSpec({
        test("reports conflicting rollback target clauses exactly") {
            NoConflictingRollbackClausesRule()
                .diagnostics(
                    """
                    ROLLBACK TO SAVEPOINT before_update FORCE '42.1.9';
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle ROLLBACK clauses: TARGET.",
                        startLine = 1,
                        startColumn = 10,
                        endLine = 1,
                        endColumn = 52,
                    ),
                )
        }

        test("reports repeated rollback savepoint clauses exactly") {
            NoConflictingRollbackClausesRule()
                .diagnostics(
                    """
                    ROLLBACK TO SAVEPOINT before_update TO SAVEPOINT after_update;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle ROLLBACK clauses: TARGET.",
                        startLine = 1,
                        startColumn = 10,
                        endLine = 1,
                        endColumn = 62,
                    ),
                )
        }

        test("accepts one rollback target clause") {
            NoConflictingRollbackClausesRule()
                .diagnostics(
                    """
                    ROLLBACK;
                    ROLLBACK TO SAVEPOINT before_update;
                    ROLLBACK FORCE '42.1.9';
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
