package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PreferUnifiedAuditingRuleTest :
    FunSpec({
        test("reports traditional AUDIT statements") {
            val diagnostics =
                PreferUnifiedAuditingRule().diagnostics(
                    """
                    enableSelectAudit:
                    AUDIT SELECT TABLE BY app_user BY ACCESS;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = PREFER_UNIFIED_AUDITING_MESSAGE,
                        startLine = 2,
                        startColumn = 1,
                        endLine = 2,
                        endColumn = 6,
                    ),
                )
        }

        test("reports traditional NOAUDIT statements") {
            val diagnostics =
                PreferUnifiedAuditingRule().diagnostics(
                    """
                    disableSelectAudit:
                    NOAUDIT SELECT TABLE BY app_user;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = PREFER_UNIFIED_AUDITING_MESSAGE,
                        startLine = 2,
                        startColumn = 1,
                        endLine = 2,
                        endColumn = 8,
                    ),
                )
        }

        test("accepts unified auditing statements") {
            PreferUnifiedAuditingRule().diagnostics(
                """
                enablePolicies:
                AUDIT POLICY app_audit_policy BY app_user WHENEVER SUCCESSFUL;

                enableContext:
                AUDIT CONTEXT NAMESPACE app_context ATTRIBUTES tenant_id;

                disablePolicies:
                NOAUDIT POLICY app_audit_policy BY app_user;

                disableContext:
                NOAUDIT CONTEXT NAMESPACE app_context ATTRIBUTES tenant_id;
                """,
            ) shouldBe emptyList()
        }

        test("ignores AUDIT and NOAUDIT text in comments and string literals") {
            PreferUnifiedAuditingRule().diagnostics(
                """
                -- AUDIT SELECT TABLE BY app_user;
                SELECT 'NOAUDIT SELECT TABLE BY app_user' AS sql_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })

private const val PREFER_UNIFIED_AUDITING_MESSAGE =
    "Prefer Oracle unified auditing policies over traditional AUDIT and NOAUDIT statements."
