package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidSubqueryRestrictionClauseRuleTest :
    FunSpec({
        test("reports conflicting subquery restriction clauses exactly") {
            ValidSubqueryRestrictionClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM (SELECT * FROM orders WITH READ ONLY WITH CHECK OPTION) o;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX WITH READ ONLY/WITH CHECK OPTION.",
                        startLine = 2,
                        startColumn = 28,
                        endLine = 2,
                        endColumn = 60,
                    ),
                )
        }

        test("reports duplicate subquery restriction clauses exactly") {
            ValidSubqueryRestrictionClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM (SELECT * FROM orders WITH READ ONLY WITH READ ONLY) o;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX WITH READ ONLY/WITH READ ONLY.",
                        startLine = 2,
                        startColumn = 28,
                        endLine = 2,
                        endColumn = 57,
                    ),
                )
        }

        test("accepts one subquery restriction clause") {
            ValidSubqueryRestrictionClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM (SELECT * FROM orders WITH CHECK OPTION) o;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("accepts one subquery restriction clause per inline view") {
            ValidSubqueryRestrictionClauseRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM (SELECT * FROM orders WITH READ ONLY) o
                    JOIN (SELECT * FROM customers WITH CHECK OPTION) c
                    ON c.customer_id = o.customer_id;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX = "Use one Oracle subquery restriction clause:"
