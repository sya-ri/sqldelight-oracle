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

        test("ignores commented comparisons") {
            NoEmptyStringComparisonRule().diagnostics(
                """
                -- WHERE name = '';
                SELECT *
                FROM customers
                WHERE id = :id;
                """,
            ) shouldBe emptyList()
        }
    })

private const val EMPTY_STRING_MESSAGE =
    "Avoid comparing with Oracle empty string literals; Oracle treats '' as NULL, so use IS NULL or IS NOT NULL."
