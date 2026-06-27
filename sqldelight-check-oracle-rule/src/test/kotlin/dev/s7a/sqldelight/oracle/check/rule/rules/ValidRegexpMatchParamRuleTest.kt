package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidRegexpMatchParamRuleTest :
    FunSpec({
        test("reports invalid REGEXP_LIKE match parameters") {
            val diagnostics =
                ValidRegexpMatchParamRule().diagnostics(
                    """
                    findName:
                    SELECT *
                    FROM customers
                    WHERE REGEXP_LIKE(name, '^a', 'iq');
                    SELECT *
                    FROM customers
                    WHERE REGEXP_LIKE(alias, '^a', q'[mz]');
                    """,
                )
            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 4,
                        startColumn = 31,
                        endLine = 4,
                        endColumn = 35,
                    ),
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 7,
                        startColumn = 32,
                        endLine = 7,
                        endColumn = 39,
                    ),
                )
        }

        test("reports invalid REGEXP_COUNT match parameters") {
            val diagnostics =
                ValidRegexpMatchParamRule().diagnostics(
                    """
                    countMatches:
                    SELECT REGEXP_COUNT(description, '[[:alpha:]]+', 1, 'z') AS word_count
                    FROM products;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 2,
                        startColumn = 53,
                        endLine = 2,
                        endColumn = 56,
                    ),
                )
        }

        test("reports invalid REGEXP_INSTR match parameters") {
            val diagnostics =
                ValidRegexpMatchParamRule().diagnostics(
                    """
                    findPosition:
                    SELECT REGEXP_INSTR(description, '[[:digit:]]+', 1, 1, 0, 'mpq') AS digit_position
                    FROM products;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 2,
                        startColumn = 59,
                        endLine = 2,
                        endColumn = 64,
                    ),
                )
        }

        test("reports invalid REGEXP_REPLACE match parameters") {
            val diagnostics =
                ValidRegexpMatchParamRule().diagnostics(
                    """
                    normalizeDescription:
                    SELECT REGEXP_REPLACE(description, '[[:space:]]+', ' ', 1, 0, 'ny') AS normalized
                    FROM products;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 2,
                        startColumn = 63,
                        endLine = 2,
                        endColumn = 67,
                    ),
                )
        }

        test("reports invalid REGEXP_SUBSTR match parameters") {
            val diagnostics =
                ValidRegexpMatchParamRule().diagnostics(
                    """
                    extractCode:
                    SELECT REGEXP_SUBSTR(description, '[A-Z]+', 1, 1, 'j') AS code
                    FROM products;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = REGEXP_MATCH_PARAM_MESSAGE,
                        startLine = 2,
                        startColumn = 51,
                        endLine = 2,
                        endColumn = 54,
                    ),
                )
        }

        test("accepts documented REGEXP match parameters") {
            ValidRegexpMatchParamRule().diagnostics(
                """
                SELECT *
                FROM customers
                WHERE REGEXP_LIKE(name, '^a', 'icnmx')
                  AND REGEXP_LIKE(alias, '^a', N'ICNMX')
                  AND REGEXP_LIKE(code, '^a', q'[imx]');
                """,
            ) shouldBe emptyList()
        }

        test("accepts contradictory case sensitivity parameters because Oracle uses the last one") {
            ValidRegexpMatchParamRule().diagnostics(
                """
                SELECT *
                FROM customers
                WHERE REGEXP_LIKE(name, '^a', 'icci');
                """,
            ) shouldBe emptyList()
        }

        test("ignores dynamic REGEXP match parameters") {
            ValidRegexpMatchParamRule().diagnostics(
                """
                SELECT *
                FROM customers
                WHERE REGEXP_LIKE(name, '^a', :match_param);
                """,
            ) shouldBe emptyList()
        }
    })

private const val REGEXP_MATCH_PARAM_MESSAGE =
    "Use only Oracle REGEXP_* match_param characters i, c, n, m, and x."
