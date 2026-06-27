package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidCreateTableAsColumnAliasesRuleTest :
    FunSpec({
        test("reports CTAS column alias count mismatches exactly") {
            val diagnostics =
                ValidCreateTableAsColumnAliasesRule().diagnostics(
                    """
                    CREATE TABLE IF NOT EXISTS account_snapshot(snapshot_id) AS
                    SELECT account_id, external_id
                    FROM staged_accounts;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CTAS declares 1 column alias(es), but the SELECT list has 2 column(s).",
                        startLine = 1,
                        startColumn = 44,
                        endLine = 1,
                        endColumn = 57,
                    ),
                )
        }

        test("reports duplicate CTAS column aliases exactly") {
            val diagnostics =
                ValidCreateTableAsColumnAliasesRule().diagnostics(
                    """
                    CREATE TABLE account_snapshot(snapshot_id, snapshot_id) AS
                    SELECT account_id, external_id
                    FROM staged_accounts;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CTAS column alias 'snapshot_id' is declared more than once.",
                        startLine = 1,
                        startColumn = 44,
                        endLine = 1,
                        endColumn = 55,
                    ),
                )
        }

        test("reports duplicate quoted CTAS column aliases exactly") {
            val diagnostics =
                ValidCreateTableAsColumnAliasesRule().diagnostics(
                    """
                    CREATE TABLE account_snapshot("snapshot_id", "snapshot_id") AS
                    SELECT account_id, external_id
                    FROM staged_accounts;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle CTAS column alias '\"snapshot_id\"' is declared more than once.",
                        startLine = 1,
                        startColumn = 46,
                        endLine = 1,
                        endColumn = 59,
                    ),
                )
        }

        test("accepts matching CTAS column aliases exactly") {
            ValidCreateTableAsColumnAliasesRule().diagnostics(
                """
                CREATE TABLE account_snapshot(snapshot_id, snapshot_external_id) AS
                SELECT account_id, external_id
                FROM staged_accounts;
                """,
            ) shouldBe emptyList()
        }

        test("accepts case-distinct quoted CTAS column aliases exactly") {
            ValidCreateTableAsColumnAliasesRule().diagnostics(
                """
                CREATE TABLE account_snapshot("Snapshot_Id", "snapshot_id") AS
                SELECT account_id, external_id
                FROM staged_accounts;
                """,
            ) shouldBe emptyList()
        }

        test("ignores ordinary CREATE TABLE column definitions exactly") {
            ValidCreateTableAsColumnAliasesRule().diagnostics(
                """
                CREATE TABLE staged_accounts(
                  account_id NUMBER,
                  external_id VARCHAR2(64)
                );
                """,
            ) shouldBe emptyList()
        }
    })
