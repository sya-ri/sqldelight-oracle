package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingRoleClausesRuleTest :
    FunSpec({
        test("reports conflicting create role identification clauses exactly") {
            NoConflictingRoleClausesRule()
                .diagnostics(
                    """
                    CREATE ROLE app_role NOT IDENTIFIED IDENTIFIED BY secret;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle role clauses: IDENTIFICATION.",
                        startLine = 1,
                        startColumn = 22,
                        endLine = 1,
                        endColumn = 57,
                    ),
                )
        }

        test("reports repeated role container clauses exactly") {
            NoConflictingRoleClausesRule()
                .diagnostics(
                    """
                    CREATE ROLE app_role CONTAINER = CURRENT CONTAINER = ALL;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle role clauses: CONTAINER.",
                        startLine = 1,
                        startColumn = 22,
                        endLine = 1,
                        endColumn = 57,
                    ),
                )
        }

        test("accepts one role clause per group") {
            NoConflictingRoleClausesRule()
                .diagnostics(
                    """
                    CREATE ROLE app_role IDENTIFIED BY secret CONTAINER = CURRENT;
                    ALTER ROLE app_role NOT IDENTIFIED;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
