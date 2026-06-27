package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingJsonStorageClausesRuleTest :
    FunSpec({
        test("reports repeated JSON COLUMN storage clauses exactly") {
            NoConflictingJsonStorageClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE documents
                      MODIFY JSON COLUMN payload STORE AS TEXT
                      MODIFY JSON COLUMN payload STORE AS BLOB;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid multiple Oracle JSON storage clauses for column payload.",
                        startLine = 2,
                        startColumn = 10,
                        endLine = 3,
                        endColumn = 43,
                    ),
                )
        }

        test("reports JSON column list storage conflicts exactly") {
            NoConflictingJsonStorageClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE documents
                      MODIFY JSON (payload, metadata) STORE AS json_storage
                      MODIFY JSON COLUMN metadata STORE AS CLOB;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid multiple Oracle JSON storage clauses for column metadata.",
                        startLine = 2,
                        startColumn = 10,
                        endLine = 3,
                        endColumn = 44,
                    ),
                )
        }

        test("accepts distinct JSON storage columns") {
            NoConflictingJsonStorageClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE documents
                      MODIFY JSON COLUMN payload STORE AS TEXT
                      MODIFY JSON COLUMN metadata STORE AS BLOB;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("accepts case-distinct quoted JSON storage columns") {
            NoConflictingJsonStorageClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE documents
                      MODIFY JSON COLUMN "Payload" STORE AS TEXT
                      MODIFY JSON COLUMN "payload" STORE AS BLOB;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("reports repeated quoted JSON storage columns exactly") {
            NoConflictingJsonStorageClausesRule()
                .diagnostics(
                    """
                    ALTER TABLE documents
                      MODIFY JSON COLUMN "payload" STORE AS TEXT
                      MODIFY JSON COLUMN "payload" STORE AS BLOB;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid multiple Oracle JSON storage clauses for column \"payload\".",
                        startLine = 2,
                        startColumn = 10,
                        endLine = 3,
                        endColumn = 45,
                    ),
                )
        }
    })
