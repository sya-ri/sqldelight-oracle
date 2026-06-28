package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoEmptyStringComparisonRuleTest :
    FunSpec({
        test("reports equality with an empty string literal") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlank:
                    SELECT *
                    FROM customers
                    WHERE name = '';
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 16,
                    ),
                )
        }

        test("reports inequality with an empty string literal") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findPresent:
                    SELECT *
                    FROM customers
                    WHERE name <> '';
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 17,
                    ),
                )
        }

        test("reports equality with an empty national string literal") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlank:
                    SELECT *
                    FROM customers
                    WHERE name = N'';
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 17,
                    ),
                )
        }

        test("reports comparisons with empty alternative quoted literals") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlank:
                    SELECT *
                    FROM customers
                    WHERE name = q'[]'
                      OR q'{}' != nickname;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 5,
                        startColumn = 6,
                        endLine = 5,
                        endColumn = 14,
                    ),
                )
        }

        test("reports LIKE predicates with empty string literals") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlankPattern:
                    SELECT *
                    FROM customers
                    WHERE name LIKE ''
                      OR nickname NOT LIKE q'[]'
                      OR alias LIKEC N''
                      OR '' LIKE display_name;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 5,
                        startColumn = 15,
                        endLine = 5,
                        endColumn = 29,
                    ),
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 6,
                        startColumn = 12,
                        endLine = 6,
                        endColumn = 21,
                    ),
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 7,
                        startColumn = 6,
                        endLine = 7,
                        endColumn = 13,
                    ),
                )
        }

        test("reports IN predicates with empty string literals") {
            val diagnostics =
                NoEmptyStringComparisonRule().diagnostics(
                    """
                    findBlankMembership:
                    SELECT *
                    FROM customers
                    WHERE name IN ('')
                      OR nickname NOT IN (N'');
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 4,
                        startColumn = 12,
                        endLine = 4,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = EMPTY_STRING_MESSAGE,
                        startLine = 5,
                        startColumn = 15,
                        endLine = 5,
                        endColumn = 26,
                    ),
                )
        }

        test("accepts null predicates") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                findBlank:
                SELECT *
                FROM customers
                WHERE name IS NULL;
                """,
            ) shouldBe emptyList()
        }

        test("ignores comparison text inside string literals") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                SELECT 'WHERE name = ''' AS sql_text,
                  'WHERE nickname <> ''' AS other_sql_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }

        test("ignores commented comparisons") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                -- WHERE name = '';
                /* WHERE name = ''; */
                SELECT /*+ INDEX(customers) */ *
                FROM customers
                WHERE id = :id;
                """,
            ) shouldBe emptyList()
        }

        test("ignores comparisons in multiline block comments") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                /*
                  WHERE name = ''
                  OR nickname <> ''
                */
                SELECT *
                FROM customers
                WHERE id = :id;
                """,
            ) shouldBe emptyList()
        }
    })

private const val EMPTY_STRING_MESSAGE =
    "Avoid comparing with Oracle empty string literals; Oracle treats '' as NULL, so use IS NULL or IS NOT NULL."
