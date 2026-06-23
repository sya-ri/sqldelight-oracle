package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingIndexClausesRuleTest :
    FunSpec({
        test("reports conflicting CREATE INDEX clauses exactly") {
            NoConflictingIndexClausesRule()
                .diagnostics(
                    """
                    CREATE UNIQUE BITMAP INDEX customer_ix
                    ON customer (email)
                    LOGGING
                    NOLOGGING
                    VISIBLE
                    INVISIBLE
                    COMPRESS
                    NOCOMPRESS
                    PARALLEL 4
                    NOPARALLEL;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: UNIQUE/BITMAP.",
                        startLine = 1,
                        startColumn = 8,
                        endLine = 1,
                        endColumn = 21,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: LOGGING/NOLOGGING.",
                        startLine = 3,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 10,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: VISIBLE/INVISIBLE.",
                        startLine = 5,
                        startColumn = 1,
                        endLine = 6,
                        endColumn = 10,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: COMPRESS/NOCOMPRESS.",
                        startLine = 7,
                        startColumn = 1,
                        endLine = 8,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: PARALLEL/NOPARALLEL.",
                        startLine = 9,
                        startColumn = 1,
                        endLine = 10,
                        endColumn = 11,
                    ),
                )
        }

        test("reports conflicting ALTER INDEX clauses exactly") {
            NoConflictingIndexClausesRule()
                .diagnostics(
                    """
                    ALTER INDEX customer_ix
                    USABLE
                    UNUSABLE
                    ONLINE
                    OFFLINE
                    INDEXING FULL
                    INDEXING PARTIAL;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: USABLE/UNUSABLE.",
                        startLine = 2,
                        startColumn = 1,
                        endLine = 3,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: ONLINE/OFFLINE.",
                        startLine = 4,
                        startColumn = 1,
                        endLine = 5,
                        endColumn = 8,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle index clauses: INDEXING FULL/INDEXING PARTIAL.",
                        startLine = 6,
                        startColumn = 1,
                        endLine = 7,
                        endColumn = 17,
                    ),
                )
        }

        test("accepts non-conflicting index clauses exactly") {
            NoConflictingIndexClausesRule()
                .diagnostics(
                    """
                    CREATE INDEX customer_ix
                    ON customer (email)
                    NOLOGGING
                    INVISIBLE
                    NOCOMPRESS
                    NOPARALLEL;

                    ALTER INDEX customer_ix USABLE ONLINE INDEXING FULL;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores non-index statements with overlapping clause names") {
            NoConflictingIndexClausesRule()
                .diagnostics(
                    """
                    CREATE TABLE customer (
                        id NUMBER(19)
                    ) LOGGING NOLOGGING;
                    ALTER TABLE customer READ ONLY READ WRITE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
