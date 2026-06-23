package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingTableClausesRuleTest :
    FunSpec({
        test("reports conflicting CREATE TABLE clauses exactly") {
            NoConflictingTableClausesRule()
                .diagnostics(
                    """
                    CREATE TABLE customer (
                        id NUMBER(19) NOT NULL
                    )
                    LOGGING
                    NOLOGGING
                    CACHE
                    NOCACHE
                    COMPRESS
                    NOCOMPRESS;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle table clauses: LOGGING/NOLOGGING.",
                        startLine = 4,
                        startColumn = 1,
                        endLine = 5,
                        endColumn = 10,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle table clauses: CACHE/NOCACHE.",
                        startLine = 6,
                        startColumn = 1,
                        endLine = 7,
                        endColumn = 8,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle table clauses: COMPRESS/NOCOMPRESS.",
                        startLine = 8,
                        startColumn = 1,
                        endLine = 9,
                        endColumn = 11,
                    ),
                )
        }

        test("reports conflicting ALTER TABLE read clauses exactly") {
            NoConflictingTableClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE customer READ ONLY READ WRITE;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle table clauses: READ ONLY/READ WRITE.",
                        startLine = 1,
                        startColumn = 22,
                        endLine = 1,
                        endColumn = 42,
                    ),
                )
        }

        test("accepts non-conflicting table clauses exactly") {
            NoConflictingTableClausesRule()
                .diagnostics(
                    """
                    CREATE GLOBAL TEMPORARY TABLE customer_stage (
                        id NUMBER(19) NOT NULL
                    )
                    NOLOGGING
                    NOCACHE
                    NOCOMPRESS;

                    ALTER TABLE customer READ WRITE;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores non-table DDL with overlapping clause names") {
            NoConflictingTableClausesRule()
                .diagnostics(
                    """
                    CREATE SEQUENCE invoice_seq CACHE 20 NOCACHE;
                    ALTER SEQUENCE invoice_seq ORDER NOORDER;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
