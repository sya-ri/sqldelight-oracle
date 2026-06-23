package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidDmlHintPlacementRuleTest :
    FunSpec({
        test("reports APPEND hints outside INSERT statements") {
            val diagnostics =
                ValidDmlHintPlacementRule().diagnostics(
                    """
                    updateCustomer:
                    UPDATE /*+ APPEND */ customers
                    SET name = :name
                    WHERE id = :id;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_HINT_MESSAGE,
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 21,
                    ),
                )
        }

        test("reports APPEND_VALUES hints on INSERT without VALUES") {
            val diagnostics =
                ValidDmlHintPlacementRule().diagnostics(
                    """
                    archiveCustomers:
                    INSERT /*+ APPEND_VALUES */ INTO archived_customers
                    SELECT *
                    FROM customers;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_HINT_MESSAGE,
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 28,
                    ),
                )
        }

        test("reports line APPEND_VALUES hints outside INSERT VALUES") {
            val diagnostics =
                ValidDmlHintPlacementRule().diagnostics(
                    """
                    mergeCustomers:
                    MERGE --+ APPEND_VALUES
                    INTO customers c
                    USING incoming_customers i
                    ON (c.id = i.id)
                    WHEN MATCHED THEN UPDATE SET c.name = i.name;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = DML_HINT_MESSAGE,
                        startLine = 2,
                        startColumn = 7,
                        endLine = 2,
                        endColumn = 24,
                    ),
                )
        }

        test("accepts APPEND on INSERT statements") {
            ValidDmlHintPlacementRule().diagnostics(
                """
                insertCustomer:
                INSERT /*+ APPEND */ INTO customers(id, name)
                SELECT id, name
                FROM incoming_customers;
                """,
            ) shouldBe emptyList()
        }

        test("accepts APPEND_VALUES on INSERT VALUES statements") {
            ValidDmlHintPlacementRule().diagnostics(
                """
                insertCustomer:
                INSERT /*+ APPEND_VALUES */ INTO customers(id, name)
                VALUES (:id, :name);
                """,
            ) shouldBe emptyList()
        }
    })

private const val DML_HINT_MESSAGE =
    "Use Oracle APPEND only on INSERT statements and APPEND_VALUES only on INSERT statements with VALUES."
