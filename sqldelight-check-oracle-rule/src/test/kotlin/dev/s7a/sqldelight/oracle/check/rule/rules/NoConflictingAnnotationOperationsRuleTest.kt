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

        test("reports conflicting quoted annotation operations exactly") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    ALTER TABLE customer
                    MODIFY customer_name ANNOTATIONS (
                      DROP "Group",
                      REPLACE "Group" 'Customer name'
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle annotation operations for Group.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 18,
                    ),
                )
        }

        test("reports conflicting string literal annotation operations exactly") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    ALTER TABLE customer
                    ANNOTATIONS (
                      DROP 'Display',
                      REPLACE 'Display' 'Customer'
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle annotation operations for Display.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 20,
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

        test("accepts matching annotation operations in different annotation clauses") {
            NoConflictingAnnotationOperationsRule()
                .diagnostics(
                    """
                    CREATE TABLE customer (
                      id NUMBER(19) ANNOTATIONS (ADD Display 'Customer ID')
                    ) ANNOTATIONS (ADD Display 'Customer table');
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
