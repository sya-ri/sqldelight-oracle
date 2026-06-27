package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidFormatModelRuleTest :
    FunSpec({
        test("reports unknown number format model tokens") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseAmount:
                    SELECT TO_NUMBER(amount_text, '999FOO') AS amount
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 31,
                        endLine = 2,
                        endColumn = 39,
                    ),
                )
        }

        test("reports invalid number format model placement") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseAmount:
                    SELECT TO_NUMBER(amount_text, '999.99,') AS amount
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 31,
                        endLine = 2,
                        endColumn = 40,
                    ),
                )
        }

        test("reports unknown datetime format model tokens") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseDate:
                    SELECT TO_DATE(created_text, 'YYYY-FOO-DD') AS created_at
                    FROM invoices;
                    parseAltDate:
                    SELECT TO_DATE(created_text, q'[YYYY-FOO-DD]') AS created_at
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DATETIME_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 30,
                        endLine = 2,
                        endColumn = 43,
                    ),
                    DiagnosticSummary(
                        message = DATETIME_FORMAT_MODEL_MESSAGE,
                        startLine = 5,
                        startColumn = 30,
                        endLine = 5,
                        endColumn = 46,
                    ),
                )
        }

        test("reports invalid alternative quoted format models containing apostrophes and commas") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseDate:
                    SELECT TO_DATE(created_text, q'[YYYY','FOO-DD]') AS created_at
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DATETIME_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 30,
                        endLine = 2,
                        endColumn = 48,
                    ),
                )
        }

        test("reports timestamp-only elements in TO_DATE format models") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseDate:
                    SELECT TO_DATE(created_text, 'YYYY-MM-DD FF3') AS created_at
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DATETIME_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 30,
                        endLine = 2,
                        endColumn = 46,
                    ),
                )
        }

        test("reports datetime format models longer than Oracle's documented limit") {
            val diagnostics =
                ValidFormatModelRule().diagnostics(
                    """
                    parseTimestamp:
                    SELECT TO_TIMESTAMP(created_text, 'YYYY-MM-DD HH24:MI:SS.FF3') AS created_at
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DATETIME_FORMAT_MODEL_MESSAGE,
                        startLine = 2,
                        startColumn = 35,
                        endLine = 2,
                        endColumn = 62,
                    ),
                )
        }

        test("accepts documented number and datetime format models") {
            ValidFormatModelRule().diagnostics(
                """
                formatValues:
                SELECT TO_NUMBER(amount_text, '999G999D99') AS parsed_amount,
                  TO_BINARY_DOUBLE(binary_text, '9.999EEEE') AS parsed_binary,
                  TO_DATE(created_text, 'YYYY-MM-DD') AS created_date,
                  TO_TIMESTAMP(created_text, 'YYYY-MM-DD HH24:MI') AS created_at,
                  TO_CHAR(created_at, 'YYYY-MM-DD') AS created_label,
                  TO_CHAR(amount, 'FM9990.00') AS amount_label,
                  TO_DATE(created_text, q'[YYYY-MM-DD]') AS alt_created_date,
                  TO_NUMBER(amount_text, q'[999G999D99]') AS alt_parsed_amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }

        test("ignores comments strings and dynamic format models") {
            ValidFormatModelRule().diagnostics(
                """
                -- SELECT TO_NUMBER(amount_text, '999FOO')
                SELECT 'TO_DATE(created_text, ''YYYY-FOO-DD'')' AS sql_text,
                  TO_NUMBER(amount_text, :amount_format) AS amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }
    })

private const val NUMBER_FORMAT_MODEL_MESSAGE =
    "Use a valid Oracle number format model literal."

private const val DATETIME_FORMAT_MODEL_MESSAGE =
    "Use a valid Oracle datetime format model literal."
