package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidLikeEscapeRuleTest :
    FunSpec({
        test("reports empty and multi-character LIKE ESCAPE literals exactly") {
            ValidLikeEscapeRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customers
                    WHERE name LIKE 'A%' ESCAPE ''
                       OR alias LIKE 'B%' ESCAPE 'xy';
                    SELECT *
                    FROM customers
                    WHERE code LIKE 'C%' ESCAPE q'[xy]';
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = LIKE_ESCAPE_MESSAGE,
                        startLine = 3,
                        startColumn = 29,
                        endLine = 3,
                        endColumn = 31,
                    ),
                    DiagnosticSummary(
                        message = LIKE_ESCAPE_MESSAGE,
                        startLine = 4,
                        startColumn = 30,
                        endLine = 4,
                        endColumn = 34,
                    ),
                    DiagnosticSummary(
                        message = LIKE_ESCAPE_MESSAGE,
                        startLine = 7,
                        startColumn = 29,
                        endLine = 7,
                        endColumn = 36,
                    ),
                )
        }

        test("accepts one-character LIKE ESCAPE literals") {
            ValidLikeEscapeRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM customers
                    WHERE name LIKE 'A\_%' ESCAPE '\'
                       OR quoted_name LIKE 'A''_%' ESCAPE ''''
                       OR national_name LIKE N'A~_%' ESCAPE N'~'
                       OR code LIKE 'C!_%' ESCAPE q'[!]';
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores dynamic escapes and ESCAPE text in comments and strings") {
            ValidLikeEscapeRule()
                .diagnostics(
                    """
                    -- WHERE name LIKE 'A%' ESCAPE 'xy'
                    SELECT 'ESCAPE ''xy''' AS sql_text
                    FROM customers
                    WHERE name LIKE 'A%' ESCAPE :escape_char;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val LIKE_ESCAPE_MESSAGE =
    "Use exactly one character in static Oracle LIKE ESCAPE literals."
