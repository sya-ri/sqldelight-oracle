package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidAuditPolicyFormRuleTest :
    FunSpec({
        test("reports BY and EXCEPT in the same audit policy statement exactly") {
            ValidAuditPolicyFormRule()
                .diagnostics(
                    """
                    AUDIT POLICY app_policy BY hr EXCEPT sh;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX BY/EXCEPT.",
                        startLine = 1,
                        startColumn = 25,
                        endLine = 1,
                        endColumn = 37,
                    ),
                )
        }

        test("reports duplicate WHENEVER clauses exactly") {
            ValidAuditPolicyFormRule()
                .diagnostics(
                    """
                    AUDIT POLICY app_policy WHENEVER SUCCESSFUL WHENEVER NOT SUCCESSFUL;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX WHENEVER.",
                        startLine = 1,
                        startColumn = 25,
                        endLine = 1,
                        endColumn = 68,
                    ),
                )
        }

        test("accepts documented audit and noaudit policy forms") {
            ValidAuditPolicyFormRule()
                .diagnostics(
                    """
                    AUDIT POLICY app_policy BY hr WHENEVER SUCCESSFUL;
                    AUDIT POLICY app_policy EXCEPT sh WHENEVER NOT SUCCESSFUL;
                    AUDIT POLICY role_policy BY USERS WITH GRANTED ROLES app_role;
                    NOAUDIT POLICY app_policy BY hr;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores traditional auditing, comments, and strings") {
            ValidAuditPolicyFormRule()
                .diagnostics(
                    """
                    -- AUDIT POLICY app_policy BY hr EXCEPT sh;
                    SELECT 'AUDIT POLICY app_policy BY hr EXCEPT sh' AS sql_text FROM dual;
                    AUDIT SELECT TABLE BY hr;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX = "Use a valid Oracle unified audit policy form:"
