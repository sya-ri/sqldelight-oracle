package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidFunctionArityRuleTest :
    FunSpec({
        test("reports too many arguments for no-argument functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    currentUser:
                    SELECT SYSDATE(1), USER(tenant_id)
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle expression SYSDATE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression USER does not accept parentheses.",
                        startLine = 2,
                        startColumn = 20,
                        endLine = 2,
                        endColumn = 24,
                    ),
                )
        }

        test("reports parentheses on Oracle datetime and user environment expressions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidNoParentheses:
                    SELECT SYSDATE(), USER(), DBTIMEZONE(), SESSIONTIMEZONE(), SYSTIMESTAMP()
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle expression SYSDATE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression USER does not accept parentheses.",
                        startLine = 2,
                        startColumn = 19,
                        endLine = 2,
                        endColumn = 23,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression DBTIMEZONE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 27,
                        endLine = 2,
                        endColumn = 37,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression SESSIONTIMEZONE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 41,
                        endLine = 2,
                        endColumn = 56,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression SYSTIMESTAMP does not accept parentheses.",
                        startLine = 2,
                        startColumn = 60,
                        endLine = 2,
                        endColumn = 72,
                    ),
                )
        }

        test("reports missing precision in parenthesized current timestamp functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidCurrentTimestamp:
                    SELECT CURRENT_TIMESTAMP(), LOCALTIMESTAMP()
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function CURRENT_TIMESTAMP expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 25,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LOCALTIMESTAMP expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 29,
                        endLine = 2,
                        endColumn = 43,
                    ),
                )
        }

        test("reports too few arguments for exact arity functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidMath:
                    SELECT POWER(amount), NVL2(status, 'Y'), REGR_SLOPE(amount), CONCAT(label)
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function POWER expects 2 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NVL2 expects 3 argument(s), but got 2.",
                        startLine = 2,
                        startColumn = 23,
                        endLine = 2,
                        endColumn = 27,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REGR_SLOPE expects 2 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 42,
                        endLine = 2,
                        endColumn = 52,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CONCAT expects 2..2147483647 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 62,
                        endLine = 2,
                        endColumn = 68,
                    ),
                )
        }

        test("reports too many arguments for ranged arity functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidRegexp:
                    SELECT REGEXP_LIKE(name, '^a', 'i', 'extra'),
                      TO_DATE(created_text, 'YYYY-MM-DD', 'NLS_DATE_LANGUAGE = American', 'extra')
                    FROM customers;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function REGEXP_LIKE expects 2..3 argument(s), but got 4.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TO_DATE expects 1..3 argument(s), but got 4.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 10,
                    ),
                )
        }

        test("reports wrong arity for common Oracle scalar functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidScalars:
                    SELECT ROUND(), TRUNC(amount, 2, 3), SUBSTR(name), INSTR(name),
                      LPAD(name), RPAD(name, 10, ' ', 'x'), LOG(10), WIDTH_BUCKET(amount, 0, 100),
                      DECODE(status, 'A'), USERENV()
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function ROUND expects 1..2 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TRUNC expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 17,
                        endLine = 2,
                        endColumn = 22,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SUBSTR expects 2..3 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 38,
                        endLine = 2,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function INSTR expects 2..4 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 52,
                        endLine = 2,
                        endColumn = 57,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LPAD expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 7,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function RPAD expects 2..3 argument(s), but got 4.",
                        startLine = 3,
                        startColumn = 15,
                        endLine = 3,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LOG expects 2 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 41,
                        endLine = 3,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function WIDTH_BUCKET expects 4 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 50,
                        endLine = 3,
                        endColumn = 62,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DECODE expects 3..255 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function USERENV expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 24,
                        endLine = 4,
                        endColumn = 31,
                    ),
                )
        }

        test("reports too many arguments for Oracle decode") {
            val decodeArguments = (1..256).joinToString(", ") { index -> index.toString() }
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidDecode:
                    SELECT DECODE($decodeArguments)
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function DECODE expects 3..255 argument(s), but got 256.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 14,
                    ),
                )
        }

        test("reports wrong arity for Oracle utility scalar functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidUtilities:
                    SELECT CHR(), DUMP(label, 10, 20, 30, 40), LTRIM(label, 'x', 'y'),
                      RTRIM(label, 'x', 'y'), REPLACE(label), SYS_CONTEXT('USERENV'),
                      ORA_HASH(label, 100, 1, 2), STANDARD_HASH(label, 'SHA256', 'extra'),
                      NLSSORT(label, 'NLS_SORT = BINARY', 'extra')
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function CHR expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DUMP expects 1..4 argument(s), but got 5.",
                        startLine = 2,
                        startColumn = 15,
                        endLine = 2,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LTRIM expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 44,
                        endLine = 2,
                        endColumn = 49,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function RTRIM expects 1..2 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 8,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REPLACE expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 27,
                        endLine = 3,
                        endColumn = 34,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SYS_CONTEXT expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 43,
                        endLine = 3,
                        endColumn = 54,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ORA_HASH expects 1..3 argument(s), but got 4.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STANDARD_HASH expects 1..2 argument(s), but got 3.",
                        startLine = 4,
                        startColumn = 31,
                        endLine = 4,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLSSORT expects 1..2 argument(s), but got 3.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 10,
                    ),
                )
        }

        test("reports wrong arity for Oracle aggregates") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidAggregates:
                    SELECT COUNT(),
                      SUM(amount, fallback_amount),
                      MAX(status, fallback_status),
                      ANY_VALUE(),
                      STATS_MODE(status, fallback),
                      REGR_COUNT(amount),
                      MEDIAN(),
                      APPROX_MEDIAN(),
                      APPROX_SUM(amount, extra),
                      APPROX_COUNT(),
                      APPROX_COUNT_DISTINCT()
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function COUNT expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SUM expects 1 argument(s), but got 2.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 6,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function MAX expects 1 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 6,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ANY_VALUE expects 1 argument(s), but got 0.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STATS_MODE expects 1 argument(s), but got 2.",
                        startLine = 6,
                        startColumn = 3,
                        endLine = 6,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REGR_COUNT expects 2 argument(s), but got 1.",
                        startLine = 7,
                        startColumn = 3,
                        endLine = 7,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function MEDIAN expects 1 argument(s), but got 0.",
                        startLine = 8,
                        startColumn = 3,
                        endLine = 8,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_MEDIAN expects 1 argument(s), but got 0.",
                        startLine = 9,
                        startColumn = 3,
                        endLine = 9,
                        endColumn = 16,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_SUM expects 1 argument(s), but got 2.",
                        startLine = 10,
                        startColumn = 3,
                        endLine = 10,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_COUNT expects 1 argument(s), but got 0.",
                        startLine = 11,
                        startColumn = 3,
                        endLine = 11,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_COUNT_DISTINCT expects 1 argument(s), but got 0.",
                        startLine = 12,
                        startColumn = 3,
                        endLine = 12,
                        endColumn = 24,
                    ),
                )
        }

        test("accepts valid fixed and ranged arity calls") {
            ValidFunctionArityRule().diagnostics(
                """
                validFunctions:
                SELECT SYSDATE,
                  USER,
                  POWER(amount, 2),
                  NVL2(status, 'Y', 'N'),
                  CONCAT(first_name, ' ', last_name),
                  COUNT(*),
                  SUM(amount),
                  MAX(status),
                  ANY_VALUE(status),
                  STATS_MODE(status),
                  MEDIAN(amount),
                  APPROX_MEDIAN(amount),
                  APPROX_SUM(amount),
                  APPROX_COUNT(*),
                  APPROX_COUNT_DISTINCT(customer_id),
                  CURRENT_TIMESTAMP(3),
                  LOCALTIMESTAMP(6),
                  REGR_SLOPE(amount, quantity),
                  COALESCE(nickname, name, 'unknown'),
                  ROUND(amount),
                  ROUND(amount, 2),
                  TRUNC(created_at, 'DD'),
                  SUBSTR(name, 1, 3),
                  INSTR(name, 'x', 1, 1),
                  LPAD(name, 10, ' '),
                  RPAD(name, 10),
                  LOG(10, amount),
                  WIDTH_BUCKET(amount, 0, 100, 10),
                  DECODE(status, 'A', 'active', 'unknown'),
                  USERENV('LANGUAGE'),
                  CHR(196),
                  DUMP(label, 1016),
                  LTRIM(label, 'x'),
                  RTRIM(label),
                  REPLACE(label, 'x', 'y'),
                  SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA', 64),
                  ORA_HASH(label, 100, 1),
                  STANDARD_HASH(label, 'SHA256'),
                  NLSSORT(label, 'NLS_SORT = BINARY'),
                  REGEXP_REPLACE(description, q'{literal ' marker, comma}', 'x'),
                  REGEXP_SUBSTR(description, '[A-Z]+', 1, 1, 'i', 0),
                  TO_TIMESTAMP(created_text, 'YYYY-MM-DD HH24:MI:SS')
                FROM customers;
                """,
            ) shouldBe emptyList()
        }

        test("ignores function-like text inside comments and strings") {
            ValidFunctionArityRule().diagnostics(
                """
                -- SELECT POWER(amount)
                SELECT 'REGEXP_LIKE(name, ''^a'', ''i'', ''x'')' AS sql_text,
                  POWER(amount, 2) AS squared_amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }

        test("ignores function-like text inside Oracle alternative quoted literals") {
            ValidFunctionArityRule().diagnostics(
                """
                SELECT q'{literal ' marker REGEXP_LIKE(name, '^a', 'i', 'x')}' AS sql_text,
                  nq'[literal ' marker POWER(amount)]' AS national_sql_text,
                  POWER(amount, 2) AS squared_amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }
    })
