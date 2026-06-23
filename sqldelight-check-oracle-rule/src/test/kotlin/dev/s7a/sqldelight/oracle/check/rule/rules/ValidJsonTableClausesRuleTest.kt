package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidJsonTableClausesRuleTest :
    FunSpec({
        test("reports duplicate JSON_TABLE row clauses exactly") {
            ValidJsonTableClausesRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM JSON_TABLE(
                      payload,
                      '${'$'}'
                      NULL ON ERROR ERROR ON ERROR
                      NULL ON EMPTY DEFAULT 'missing' ON EMPTY
                      COLUMNS (id NUMBER PATH '${'$'}.id')
                    ) jt;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON ERROR clause in this scope.",
                        startLine = 5,
                        startColumn = 8,
                        endLine = 5,
                        endColumn = 31,
                    ),
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON EMPTY clause in this scope.",
                        startLine = 6,
                        startColumn = 8,
                        endLine = 6,
                        endColumn = 43,
                    ),
                )
        }

        test("reports duplicate JSON_TABLE column option groups exactly") {
            ValidJsonTableClausesRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM JSON_TABLE(
                      payload,
                      '${'$'}'
                      COLUMNS (
                        details JSON FORMAT JSON PATH '${'$'}.details' WITH WRAPPER WITHOUT WRAPPER NULL ON EMPTY DEFAULT '[]' ON EMPTY,
                        amount NUMBER PATH '${'$'}.amount' NULL ON MISMATCH ERROR ON MISMATCH
                      )
                    ) jt;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX wrapper clause in this scope.",
                        startLine = 6,
                        startColumn = 47,
                        endLine = 6,
                        endColumn = 75,
                    ),
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON EMPTY clause in this scope.",
                        startLine = 6,
                        startColumn = 81,
                        endLine = 6,
                        endColumn = 111,
                    ),
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON MISMATCH clause in this scope.",
                        startLine = 7,
                        startColumn = 40,
                        endLine = 7,
                        endColumn = 69,
                    ),
                )
        }

        test("reports duplicate nested JSON_TABLE column option groups exactly") {
            ValidJsonTableClausesRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM JSON_TABLE(
                      payload,
                      '${'$'}'
                      COLUMNS (
                        NESTED PATH '${'$'}.items[*]' COLUMNS (
                          item_name VARCHAR2(100) PATH '${'$'}.name' NULL ON ERROR ERROR ON ERROR
                        )
                      )
                    ) jt;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON ERROR clause in this scope.",
                        startLine = 7,
                        startColumn = 50,
                        endLine = 7,
                        endColumn = 73,
                    ),
                )
        }

        test("accepts documented JSON_TABLE row and column clauses") {
            ValidJsonTableClausesRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM JSON_TABLE(
                      payload,
                      '${'$'}'
                      ERROR ON ERROR
                      DEFAULT 'missing' ON EMPTY
                      COLUMNS (
                        id NUMBER PATH '${'$'}.id' NULL ON EMPTY DEFAULT 0 ON ERROR,
                        details JSON FORMAT JSON PATH '${'$'}.details' WITH CONDITIONAL WRAPPER NULL ON EMPTY,
                        has_items NUMBER EXISTS PATH '${'$'}.items' TRUE ON ERROR FALSE ON EMPTY,
                        NESTED PATH '${'$'}.items[*]' COLUMNS (
                          item_name VARCHAR2(100) PATH '${'$'}.name' NULL ON ERROR
                        )
                      )
                    ) jt;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX = "Use one Oracle JSON_TABLE"
