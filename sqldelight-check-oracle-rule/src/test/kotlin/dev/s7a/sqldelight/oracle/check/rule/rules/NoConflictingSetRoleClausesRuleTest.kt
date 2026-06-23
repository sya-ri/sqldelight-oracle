package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingSetRoleClausesRuleTest :
    FunSpec({
        test("reports conflicting set role all and none exactly") {
            NoConflictingSetRoleClausesRule()
                .diagnostics(
                    """
                    SET ROLE ALL NONE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET ROLE clauses: MODE.",
                        startLine = 1,
                        startColumn = 10,
                        endLine = 1,
                        endColumn = 18,
                    ),
                )
        }

        test("reports conflicting set role all except and none exactly") {
            NoConflictingSetRoleClausesRule()
                .diagnostics(
                    """
                    SET ROLE ALL EXCEPT read_role NONE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle SET ROLE clauses: MODE.",
                        startLine = 1,
                        startColumn = 10,
                        endLine = 1,
                        endColumn = 35,
                    ),
                )
        }

        test("accepts one set role form") {
            NoConflictingSetRoleClausesRule()
                .diagnostics(
                    """
                    SET ROLE ALL EXCEPT read_role;
                    SET ROLE NONE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
