package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoUppercaseRowidColumnRuleTest :
    FunSpec({
        test("reports quoted uppercase ROWID in CREATE TABLE column definitions exactly") {
            NoUppercaseRowidColumnRule()
                .diagnostics(
                    """
                    CREATE TABLE audit_event (
                        "ROWID" NUMBER(19) NOT NULL,
                        event_name VARCHAR2(100)
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = ROWID_COLUMN_MESSAGE,
                        startLine = 2,
                        startColumn = 5,
                        endLine = 2,
                        endColumn = 12,
                    ),
                )
        }

        test("reports quoted uppercase ROWID in ALTER TABLE column operations exactly") {
            NoUppercaseRowidColumnRule()
                .diagnostics(
                    """
                    ALTER TABLE audit_event ADD "ROWID" NUMBER(19);
                    ALTER TABLE audit_event RENAME COLUMN event_id TO "ROWID";
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = ROWID_COLUMN_MESSAGE,
                        startLine = 1,
                        startColumn = 29,
                        endLine = 1,
                        endColumn = 36,
                    ),
                    DiagnosticSummary(
                        message = ROWID_COLUMN_MESSAGE,
                        startLine = 2,
                        startColumn = 51,
                        endLine = 2,
                        endColumn = 58,
                    ),
                )
        }

        test("accepts non-column quoted ROWID and mixed-case rowid identifiers exactly") {
            NoUppercaseRowidColumnRule()
                .diagnostics(
                    """
                    CREATE TABLE "ROWID" (
                        "Rowid" NUMBER(19),
                        "rowid" NUMBER(19)
                    );

                    SELECT "ROWID"
                    FROM audit_event;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores ROWID text in comments and string literals") {
            NoUppercaseRowidColumnRule()
                .diagnostics(
                    """
                    -- CREATE TABLE audit_event ("ROWID" NUMBER(19));
                    SELECT 'ALTER TABLE audit_event ADD "ROWID" NUMBER(19)' AS ddl_text
                    FROM dual;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })

private const val ROWID_COLUMN_MESSAGE =
    "Do not use quoted uppercase \"ROWID\" as an Oracle column name."
