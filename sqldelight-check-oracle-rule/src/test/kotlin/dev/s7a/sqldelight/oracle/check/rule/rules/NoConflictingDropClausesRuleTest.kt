package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingDropClausesRuleTest :
    FunSpec({
        test("reports conflicting drop type clauses exactly") {
            NoConflictingDropClausesRule()
                .diagnostics(
                    """
                    DROP TYPE address_type FORCE VALIDATE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle DROP clauses: TYPE FINALITY.",
                        startLine = 1,
                        startColumn = 24,
                        endLine = 1,
                        endColumn = 38,
                    ),
                )
        }

        test("reports repeated drop table purge clauses exactly") {
            NoConflictingDropClausesRule()
                .diagnostics(
                    """
                    DROP TABLE audit_event PURGE PURGE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle DROP clauses: PURGE.",
                        startLine = 1,
                        startColumn = 24,
                        endLine = 1,
                        endColumn = 35,
                    ),
                )
        }

        test("accepts valid drop clauses") {
            NoConflictingDropClausesRule()
                .diagnostics(
                    """
                    DROP TYPE address_type FORCE;
                    DROP TABLE audit_event PURGE;
                    DROP TYPE BODY address_type;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
