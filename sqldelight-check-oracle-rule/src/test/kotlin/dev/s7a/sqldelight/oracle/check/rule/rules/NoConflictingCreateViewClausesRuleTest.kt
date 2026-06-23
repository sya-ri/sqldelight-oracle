package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingCreateViewClausesRuleTest :
    FunSpec({
        test("reports conflicting FORCE clauses exactly") {
            NoConflictingCreateViewClausesRule()
                .diagnostics(
                    """
                    CREATE OR REPLACE FORCE NO FORCE VIEW customer_view AS
                    SELECT id FROM customers;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX FORCE/NO FORCE.",
                        startLine = 1,
                        startColumn = 19,
                        endLine = 1,
                        endColumn = 33,
                    ),
                )
        }

        test("reports conflicting editioning clauses exactly") {
            NoConflictingCreateViewClausesRule()
                .diagnostics(
                    """
                    CREATE EDITIONING NONEDITIONING VIEW customer_view AS
                    SELECT id FROM customers;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX EDITIONING/NONEDITIONING.",
                        startLine = 1,
                        startColumn = 8,
                        endLine = 1,
                        endColumn = 32,
                    ),
                )
        }

        test("accepts documented create view clauses") {
            NoConflictingCreateViewClausesRule()
                .diagnostics(
                    """
                    CREATE OR REPLACE FORCE EDITIONABLE VIEW customer_view AS
                    SELECT id FROM customers;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX =
    "Avoid conflicting Oracle CREATE VIEW clauses:"
