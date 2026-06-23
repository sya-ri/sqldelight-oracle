package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RequireNumberPrecisionRuleTest :
    FunSpec({
        test("reports bare NUMBER column type") {
            val diagnostics =
                RequireNumberPrecisionRule().diagnostics(
                    """
                    CREATE TABLE invoice (
                        amount NUMBER NOT NULL
                    );
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_PRECISION_MESSAGE,
                        startLine = 2,
                        startColumn = 12,
                        endLine = 2,
                        endColumn = 18,
                    ),
                )
        }

        test("reports bare NUMBER regardless of keyword case") {
            val diagnostics =
                RequireNumberPrecisionRule().diagnostics(
                    """
                    CREATE TABLE invoice (
                        amount number NOT NULL
                    );
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_PRECISION_MESSAGE,
                        startLine = 2,
                        startColumn = 12,
                        endLine = 2,
                        endColumn = 18,
                    ),
                )
        }

        test("reports wildcard NUMBER precision") {
            val diagnostics =
                RequireNumberPrecisionRule().diagnostics(
                    """
                    CREATE TABLE invoice (
                        amount NUMBER(*) NOT NULL,
                        tax_amount NUMBER(*, 2)
                    );
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_PRECISION_MESSAGE,
                        startLine = 2,
                        startColumn = 12,
                        endLine = 2,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = NUMBER_PRECISION_MESSAGE,
                        startLine = 3,
                        startColumn = 16,
                        endLine = 3,
                        endColumn = 22,
                    ),
                )
        }

        test("accepts NUMBER with precision") {
            RequireNumberPrecisionRule().diagnostics(
                """
                CREATE TABLE invoice (
                    id NUMBER(19) NOT NULL
                );
                """,
            ) shouldBe emptyList()
        }

        test("accepts NUMBER with precision and scale") {
            RequireNumberPrecisionRule().diagnostics(
                """
                CREATE TABLE invoice (
                    amount NUMBER(10, 2) NOT NULL,
                    tax_amount NUMBER ( 8, 2 )
                );
                """,
            ) shouldBe emptyList()
        }

        test("ignores NUMBER in comments and string literals") {
            RequireNumberPrecisionRule().diagnostics(
                """
                -- amount NUMBER NOT NULL
                /* amount NUMBER NOT NULL */
                SELECT /*+ NO_INDEX(invoice) */ 'NUMBER' AS label
                FROM dual;
                """,
            ) shouldBe emptyList()
        }

        test("ignores NUMBER in multiline block comments") {
            RequireNumberPrecisionRule().diagnostics(
                """
                /*
                  CREATE TABLE invoice (
                    amount NUMBER NOT NULL
                  );
                */
                SELECT 'NUMBER' AS label
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })

private const val NUMBER_PRECISION_MESSAGE =
    "Declare Oracle NUMBER with explicit precision and scale, such as NUMBER(19) or NUMBER(10, 2)."
