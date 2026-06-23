package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidReturningClauseRuleTest :
    FunSpec({
        test("reports repeated returning clauses exactly") {
            ValidReturningClauseRule()
                .diagnostics(
                    """
                    UPDATE customer
                    SET name = :name
                    RETURNING id INTO :id
                    RETURNING name INTO :name_out;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle RETURNING clause: RETURNING.",
                        startLine = 3,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 10,
                    ),
                )
        }

        test("reports repeated returning into clauses exactly") {
            ValidReturningClauseRule()
                .diagnostics(
                    """
                    DELETE FROM customer
                    WHERE id = :id
                    RETURNING id INTO :old_id INTO :new_id;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle RETURNING clause: INTO.",
                        startLine = 3,
                        startColumn = 14,
                        endLine = 3,
                        endColumn = 31,
                    ),
                )
        }

        test("reports mixed old and new returning expressions exactly") {
            ValidReturningClauseRule()
                .diagnostics(
                    """
                    UPDATE customer
                    SET name = :name
                    RETURNING OLD id, NEW id INTO :id;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle RETURNING clause: OLD/NEW.",
                        startLine = 3,
                        startColumn = 11,
                        endLine = 3,
                        endColumn = 22,
                    ),
                )
        }

        test("accepts one valid returning clause") {
            ValidReturningClauseRule()
                .diagnostics(
                    """
                    INSERT INTO customer (id, name)
                    VALUES (:id, :name)
                    RETURNING id INTO :generated_id;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
