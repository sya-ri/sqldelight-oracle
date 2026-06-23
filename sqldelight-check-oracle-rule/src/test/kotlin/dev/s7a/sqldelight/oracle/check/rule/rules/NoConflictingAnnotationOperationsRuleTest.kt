package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingAnnotationOperationsRuleTest :
    FunSpec({
        test("reports conflicting annotation operations exactly") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    ALTER TABLE customer
                    ANNOTATIONS (
                      ADD sensitive,
                      DROP sensitive
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle annotation operations for sensitive.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 17,
                    ),
                )
        }

        test("reports conflicting annotation operations with existence checks exactly") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    ALTER TABLE customer
                    ANNOTATIONS (
                      ADD IF NOT EXISTS pii,
                      REPLACE pii
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle annotation operations for pii.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 14,
                    ),
                )
        }

        test("accepts distinct annotation operations") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    ALTER TABLE customer
                    ANNOTATIONS (
                      ADD sensitive,
                      ADD owner
                    );
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
