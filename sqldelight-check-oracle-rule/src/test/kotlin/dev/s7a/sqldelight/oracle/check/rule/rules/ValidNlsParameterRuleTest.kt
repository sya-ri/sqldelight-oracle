package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidNlsParameterRuleTest :
    FunSpec({
        test("reports invalid datetime NLS parameter literals exactly") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    SELECT TO_DATE(created_text, 'Month DD, YYYY', 'NLS_NUMERIC_CHARACTERS = ''.,''') AS created_at
                    FROM invoices;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DATETIME_NLS_PARAMETER_MESSAGE,
                        startLine = 1,
                        startColumn = 48,
                        endLine = 1,
                        endColumn = 81,
                    ),
                )
        }

        test("reports invalid number NLS parameter literals exactly") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    SELECT TO_NUMBER(amount_text, '999G999D99', 'NLS_NUMERIC_CHARACTERS = ''..''') AS amount
                    FROM invoices;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_NLS_PARAMETER_MESSAGE,
                        startLine = 1,
                        startColumn = 45,
                        endLine = 1,
                        endColumn = 78,
                    ),
                )
        }

        test("reports unknown NLS parameter names exactly") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    SELECT TO_NUMBER(amount_text, '999G999D99', 'NLS_UNKNOWN = ''x''') AS amount
                    FROM invoices;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_NLS_PARAMETER_MESSAGE,
                        startLine = 1,
                        startColumn = 45,
                        endLine = 1,
                        endColumn = 66,
                    ),
                )
        }

        test("reports invalid alternative quoted NLS parameter literals") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    SELECT TO_NUMBER(amount_text, '999G999D99', q'[NLS_NUMERIC_CHARACTERS = ',,']') AS amount
                    FROM invoices;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = NUMBER_NLS_PARAMETER_MESSAGE,
                        startLine = 1,
                        startColumn = 45,
                        endLine = 1,
                        endColumn = 79,
                    ),
                )
        }

        test("accepts documented datetime and number NLS parameter literals") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    SELECT TO_DATE(created_text, 'Month DD, YYYY', 'NLS_DATE_LANGUAGE = American') AS created_date,
                      TO_TIMESTAMP(created_text, 'DD-Mon-RR HH24:MI:SS.FF', 'NLS_DATE_LANGUAGE = American') AS created_at,
                      TO_TIMESTAMP_TZ(created_text, 'YYYY-MM-DD HH24:MI:SS TZH:TZM', 'NLS_DATE_LANGUAGE = American') AS created_tz,
                      TO_NUMBER(amount_text, 'L9G999D99', 'NLS_NUMERIC_CHARACTERS = '',.'' NLS_CURRENCY = ''AusDollars''') AS amount,
                      TO_BINARY_FLOAT(amount_text, '9G999D99', 'NLS_NUMERIC_CHARACTERS = ''.,''') AS binary_amount,
                      TO_CHAR(amount, '99G999D99C', 'NLS_NUMERIC_CHARACTERS = '',.'' NLS_ISO_CURRENCY = POLAND') AS amount_label
                    FROM invoices;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores dynamic NLS parameters and NLS text in comments and strings") {
            ValidNlsParameterRule()
                .diagnostics(
                    """
                    -- SELECT TO_DATE(created_text, 'YYYY', 'NLS_UNKNOWN = ''x''')
                    SELECT 'TO_NUMBER(amount_text, ''999'', ''NLS_UNKNOWN = ''''x'''''')' AS sql_text,
                      TO_NUMBER(amount_text, '999G999D99', :nls_parameter) AS amount
                    FROM invoices;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val DATETIME_NLS_PARAMETER_MESSAGE =
    "Use a valid Oracle datetime NLS parameter literal."

private const val NUMBER_NLS_PARAMETER_MESSAGE =
    "Use a valid Oracle number NLS parameter literal."
