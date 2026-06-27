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

        test("reports required column additions when only a sibling column has a default") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    ALTER TABLE customer ADD (
                        status VARCHAR2(20) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                    );
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

        test("reports required column modifications without default values") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    ALTER TABLE customer MODIFY status VARCHAR2(20) NOT NULL;
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
                    ALTER TABLE customer SET UNUSED COLUMN legacy_code;
                    ALTER TABLE customer DROP UNUSED COLUMNS;
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
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 4,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 5,
                        startColumn = 1,
                        endLine = 5,
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

        test("reports TRUNCATE CLUSTER") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    TRUNCATE CLUSTER customer_cluster;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 1,
                        startColumn = 1,
                        endLine = 1,
                        endColumn = 17,
                    ),
                )
        }

        test("reports destructive partition maintenance operations") {
            val diagnostics =
                UnsafeDdlMigrationRule().diagnostics(
                    """
                    ALTER TABLE orders DROP PARTITION orders_2024;
                    ALTER TABLE orders TRUNCATE SUBPARTITION orders_2024_q1;
                    ALTER TABLE orders MOVE PARTITION orders_2024;
                    ALTER TABLE orders MERGE PARTITIONS orders_2024_q1, orders_2024_q2 INTO PARTITION orders_2024_h1;
                    ALTER TABLE orders SPLIT PARTITION orders_2024 AT (DATE '2024-07-01') INTO (PARTITION orders_2024_h1, PARTITION orders_2024_h2);
                    ALTER TABLE orders EXCHANGE PARTITION orders_2024 WITH TABLE orders_stage;
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
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 4,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 5,
                        startColumn = 1,
                        endLine = 5,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = UNSAFE_DDL_MESSAGE,
                        startLine = 6,
                        startColumn = 1,
                        endLine = 6,
                        endColumn = 12,
                    ),
                )
        }

        test("accepts additive nullable and defaulted columns") {
            UnsafeDdlMigrationRule().diagnostics(
                """
                ALTER TABLE customer ADD display_name VARCHAR2(200);
                ALTER TABLE customer ADD status VARCHAR2(20) DEFAULT 'active' NOT NULL;
                ALTER TABLE customer MODIFY status VARCHAR2(20) DEFAULT 'active' NOT NULL;
                """,
            ) shouldBe emptyList()
        }

        test("ignores unsafe DDL text in comments and string literals") {
            UnsafeDdlMigrationRule().diagnostics(
                """
                -- ALTER TABLE customer DROP COLUMN legacy_code;
                /* ALTER TABLE customer DROP COLUMN legacy_code; */
                SELECT /*+ PARALLEL(customer) */ 'TRUNCATE TABLE customer_event' AS ddl_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }

        test("ignores unsafe DDL text in multiline block comments") {
            UnsafeDdlMigrationRule().diagnostics(
                """
                /*
                  ALTER TABLE customer DROP COLUMN legacy_code;
                  TRUNCATE TABLE customer_event;
                */
                SELECT 'ALTER TABLE customer MOVE' AS ddl_text
                FROM dual;
                """,
            ) shouldBe emptyList()
        }
    })

private const val UNSAFE_DDL_MESSAGE =
    "Review Oracle migration DDL that can rewrite, lock, or destructively change large tables."
