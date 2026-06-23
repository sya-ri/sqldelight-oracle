package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidSegmentCreationClauseRuleTest :
    FunSpec({
        test("reports duplicate segment creation clauses exactly") {
            ValidSegmentCreationClauseRule()
                .diagnostics(
                    """
                    CREATE TABLE customers (
                        id NUMBER(19) NOT NULL
                    )
                    SEGMENT CREATION IMMEDIATE
                    SEGMENT CREATION DEFERRED;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = SEGMENT_CREATION_MESSAGE,
                        startLine = 4,
                        startColumn = 1,
                        endLine = 5,
                        endColumn = 26,
                    ),
                )
        }

        test("accepts one segment creation clause") {
            ValidSegmentCreationClauseRule()
                .diagnostics(
                    """
                    CREATE TABLE customers (
                        id NUMBER(19) NOT NULL
                    )
                    SEGMENT CREATION DEFERRED;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores segment creation text in comments and strings") {
            ValidSegmentCreationClauseRule()
                .diagnostics(
                    """
                    -- SEGMENT CREATION IMMEDIATE SEGMENT CREATION DEFERRED
                    CREATE TABLE customers (
                        note VARCHAR2(100) DEFAULT 'SEGMENT CREATION IMMEDIATE SEGMENT CREATION DEFERRED'
                    )
                    SEGMENT CREATION IMMEDIATE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val SEGMENT_CREATION_MESSAGE =
    "Use one Oracle SEGMENT CREATION clause per table statement."
