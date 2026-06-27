package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidLockTableWaitClauseRuleTest :
    FunSpec({
        test("reports conflicting lock table wait clauses exactly") {
            ValidLockTableWaitClauseRule()
                .diagnostics(
                    """
                    LOCK TABLE customer IN EXCLUSIVE MODE NOWAIT WAIT 5;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle LOCK TABLE wait clause: WAIT.",
                        startLine = 1,
                        startColumn = 39,
                        endLine = 1,
                        endColumn = 52,
                    ),
                )
        }

        test("reports invalid lock table wait value exactly") {
            ValidLockTableWaitClauseRule()
                .diagnostics(
                    """
                    LOCK TABLE customer IN SHARE MODE WAIT seconds;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle LOCK TABLE wait clause: WAIT VALUE.",
                        startLine = 1,
                        startColumn = 35,
                        endLine = 1,
                        endColumn = 47,
                    ),
                )
        }

        test("reports negative lock table wait values") {
            ValidLockTableWaitClauseRule()
                .diagnostics(
                    """
                    LOCK TABLE customer IN SHARE MODE WAIT -5;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Use a valid Oracle LOCK TABLE wait clause: WAIT VALUE.",
                        startLine = 1,
                        startColumn = 35,
                        endLine = 1,
                        endColumn = 42,
                    ),
                )
        }

        test("accepts one static lock table wait clause") {
            ValidLockTableWaitClauseRule()
                .diagnostics(
                    """
                    LOCK TABLE customer IN EXCLUSIVE MODE WAIT 5;
                    LOCK TABLE invoice IN SHARE MODE NOWAIT;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
