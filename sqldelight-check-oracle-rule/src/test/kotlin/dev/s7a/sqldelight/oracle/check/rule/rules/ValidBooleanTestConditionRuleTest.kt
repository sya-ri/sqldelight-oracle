package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidBooleanTestConditionRuleTest :
    FunSpec({
        test("reports double NOT boolean test conditions exactly") {
            ValidBooleanTestConditionRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM feature_flags
                    WHERE enabled IS NOT NOT TRUE
                       OR archived IS NOT NOT UNKNOWN;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = BOOLEAN_TEST_MESSAGE,
                        startLine = 3,
                        startColumn = 15,
                        endLine = 3,
                        endColumn = 30,
                    ),
                    DiagnosticSummary(
                        message = BOOLEAN_TEST_MESSAGE,
                        startLine = 4,
                        startColumn = 16,
                        endLine = 4,
                        endColumn = 34,
                    ),
                )
        }

        test("accepts documented boolean test conditions") {
            ValidBooleanTestConditionRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM feature_flags
                    WHERE enabled IS TRUE
                       OR enabled IS NOT FALSE
                       OR archived IS UNKNOWN;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores boolean test text in comments and strings") {
            ValidBooleanTestConditionRule()
                .diagnostics(
                    """
                    -- WHERE enabled IS NOT NOT TRUE
                    SELECT 'IS NOT NOT TRUE' AS sql_text
                    FROM feature_flags
                    WHERE enabled IS TRUE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val BOOLEAN_TEST_MESSAGE =
    "Use a valid Oracle boolean test condition."
