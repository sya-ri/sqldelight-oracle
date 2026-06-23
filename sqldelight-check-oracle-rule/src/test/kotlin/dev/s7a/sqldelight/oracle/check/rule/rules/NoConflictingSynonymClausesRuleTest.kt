package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingSynonymClausesRuleTest :
    FunSpec({
        test("reports conflicting editionable synonym clauses exactly") {
            NoConflictingSynonymClausesRule()
                .diagnostics(
                    """
                    CREATE OR REPLACE EDITIONABLE NONEDITIONABLE SYNONYM app_syn
                    FOR hr.customer;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle CREATE SYNONYM clauses: EDITIONABLE.",
                        startLine = 1,
                        startColumn = 19,
                        endLine = 1,
                        endColumn = 45,
                    ),
                )
        }

        test("reports repeated sharing synonym clauses exactly") {
            NoConflictingSynonymClausesRule()
                .diagnostics(
                    """
                    CREATE SYNONYM app_syn
                    SHARING = METADATA
                    SHARING = NONE
                    FOR hr.customer;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle CREATE SYNONYM clauses: SHARING.",
                        startLine = 2,
                        startColumn = 1,
                        endLine = 3,
                        endColumn = 15,
                    ),
                )
        }

        test("accepts one synonym clause per semantic group") {
            NoConflictingSynonymClausesRule()
                .diagnostics(
                    """
                    CREATE OR REPLACE EDITIONABLE PUBLIC SYNONYM app_syn
                    SHARING = METADATA
                    FOR hr.customer;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
