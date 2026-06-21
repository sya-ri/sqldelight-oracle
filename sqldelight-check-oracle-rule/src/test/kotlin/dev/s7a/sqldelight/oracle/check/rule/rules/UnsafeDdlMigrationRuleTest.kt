package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UnsafeDdlMigrationRuleTest :
    FunSpec({
        test("reports required column additions without default values") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    ALTER TABLE customer ADD status VARCHAR2(20) NOT NULL;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 1,
                        startColumn = 1,
                        endLine = 1,
                        endColumn = 12,
                    ),
                )
        }

        test("reports destructive ALTER TABLE operations") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    ALTER TABLE customer DROP COLUMN legacy_code;
                    ALTER TABLE customer MOVE;
                    ALTER TABLE customer SHRINK SPACE;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 1,
                        startColumn = 1,
                        endLine = 1,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 2,
                        startColumn = 1,
                        endLine = 2,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 3,
                        startColumn = 1,
                        endLine = 3,
                        endColumn = 12,
                    ),
                )
        }

        test("reports TRUNCATE TABLE") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    TRUNCATE TABLE customer_event;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 1,
                        startColumn = 1,
                        endLine = 1,
                        endColumn = 15,
                    ),
                )
        }

        test("accepts additive nullable and defaulted columns") {
            UnsafeDdlMigrationRule().diagnostics(
                """
                ALTER TABLE customer ADD display_name VARCHAR2(200);
                ALTER TABLE customer ADD status VARCHAR2(20) DEFAULT 'active' NOT NULL;
                """,
            ) shouldBe emptyList()
        }

        test("ignores unsafe DDL text in comments and string literals") {
            UnsafeDdlMigrationRule().diagnostics(
                """
                -- ALTER TABLE customer DROP COLUMN legacy_code;
                SELECT 'TRUNCATE TABLE customer_event' AS ddl_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })

private const val UNSAFE_DDL_MESSAGE =
    "Review Oracle migration DDL that can rewrite, lock, or destructively change large tables."
