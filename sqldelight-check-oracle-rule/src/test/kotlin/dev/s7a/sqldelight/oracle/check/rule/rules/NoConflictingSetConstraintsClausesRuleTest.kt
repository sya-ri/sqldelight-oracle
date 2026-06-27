package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingSetConstraintsClausesRuleTest :
    FunSpec({
        test("reports conflicting set constraints timing clauses exactly") {
            NoConflictingSetConstraintsClausesRule()
                .diagnostics(
                    """
                    SET CONSTRAINTS ALL IMMEDIATE DEFERRED;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET CONSTRAINTS clauses: TIMING.",
                        startLine = 1,
                        startColumn = 21,
                        endLine = 1,
                        endColumn = 39,
                    ),
                )
        }

        test("reports conflicting singular set constraint timing clauses exactly") {
            NoConflictingSetConstraintsClausesRule()
                .diagnostics(
                    """
                    SET CONSTRAINT constraint_name IMMEDIATE DEFERRED;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET CONSTRAINTS clauses: TIMING.",
                        startLine = 1,
                        startColumn = 32,
                        endLine = 1,
                        endColumn = 50,
                    ),
                )
        }

        test("accepts one set constraints timing clause") {
            NoConflictingSetConstraintsClausesRule()
                .diagnostics(
                    """
                    SET CONSTRAINTS ALL IMMEDIATE;
                    SET CONSTRAINT constraint_name DEFERRED;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
