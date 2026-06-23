package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidJsonConditionOptionsRuleTest :
    FunSpec({
        test("reports conflicting IS JSON condition options exactly") {
            ValidJsonConditionOptionsRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM documents
                    WHERE payload IS JSON STRICT LAX WITH UNIQUE KEYS WITHOUT UNIQUE KEYS;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX STRICT/LAX.",
                        startLine = 3,
                        startColumn = 23,
                        endLine = 3,
                        endColumn = 33,
                    ),
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX WITH UNIQUE KEYS/WITHOUT UNIQUE KEYS.",
                        startLine = 3,
                        startColumn = 34,
                        endLine = 3,
                        endColumn = 70,
                    ),
                )
        }

        test("reports conflicting JSON_EXISTS ON clauses exactly") {
            ValidJsonConditionOptionsRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM documents
                    WHERE JSON_EXISTS(payload, '${'$'}.items[*]' TRUE ON ERROR FALSE ON ERROR ERROR ON EMPTY TRUE ON EMPTY);
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON ERROR.",
                        startLine = 3,
                        startColumn = 46,
                        endLine = 3,
                        endColumn = 69,
                    ),
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON EMPTY.",
                        startLine = 3,
                        startColumn = 76,
                        endLine = 3,
                        endColumn = 98,
                    ),
                )
        }

        test("reports ON EMPTY for JSON_EQUAL exactly") {
            ValidJsonConditionOptionsRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM documents
                    WHERE JSON_EQUAL(payload, expected_payload TRUE ON ERROR TRUE ON EMPTY);
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "$MESSAGE_PREFIX ON EMPTY is not valid for this SQL/JSON condition.",
                        startLine = 3,
                        startColumn = 63,
                        endLine = 3,
                        endColumn = 71,
                    ),
                )
        }

        test("accepts documented SQL JSON condition options") {
            ValidJsonConditionOptionsRule()
                .diagnostics(
                    """
                    SELECT *
                    FROM documents
                    WHERE payload IS JSON STRICT WITH UNIQUE KEYS
                      AND JSON_EXISTS(payload, '${'$'}.items[*]' TRUE ON ERROR FALSE ON EMPTY)
                      AND JSON_EQUAL(payload, expected_payload TRUE ON ERROR)
                      AND JSON_TEXTCONTAINS(payload, '${'$'}.description', search_text);
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val MESSAGE_PREFIX = "Use valid Oracle SQL/JSON condition options:"
