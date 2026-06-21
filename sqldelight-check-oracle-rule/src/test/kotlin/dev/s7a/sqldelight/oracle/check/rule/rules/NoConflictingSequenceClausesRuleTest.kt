package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingSequenceClausesRuleTest :
    FunSpec({
        test("reports conflicting create sequence clauses exactly") {
            NoConflictingSequenceClausesRule()
                .diagnostics(
                    """
                    CREATE SEQUENCE invoice_seq
                        SHARING = METADATA
                        SHARING = DATA
                        CACHE 20
                        NOCACHE
                        ORDER
                        NOORDER
                        SCALE EXTEND
                        NOSCALE
                        SESSION
                        GLOBAL;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: SHARING.",
                        startLine = 2,
                        startColumn = 5,
                        endLine = 3,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: CACHE/NOCACHE.",
                        startLine = 4,
                        startColumn = 5,
                        endLine = 5,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: ORDER/NOORDER.",
                        startLine = 6,
                        startColumn = 5,
                        endLine = 7,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: SCALE/NOSCALE.",
                        startLine = 8,
                        startColumn = 5,
                        endLine = 9,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: SESSION/GLOBAL.",
                        startLine = 10,
                        startColumn = 5,
                        endLine = 11,
                        endColumn = 11,
                    ),
                )
        }

        test("reports conflicting alter sequence clauses exactly") {
            NoConflictingSequenceClausesRule()
                .diagnostics(
                    """
                    ALTER SEQUENCE invoice_seq
                        MAXVALUE 1000
                        NOMAXVALUE
                        MINVALUE 1
                        NOMINVALUE
                        CYCLE
                        NOCYCLE
                        KEEP
                        NOKEEP
                        SHARD EXTEND
                        NOSHARD;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: MAXVALUE/NOMAXVALUE.",
                        startLine = 2,
                        startColumn = 5,
                        endLine = 3,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: MINVALUE/NOMINVALUE.",
                        startLine = 4,
                        startColumn = 5,
                        endLine = 5,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: CYCLE/NOCYCLE.",
                        startLine = 6,
                        startColumn = 5,
                        endLine = 7,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: KEEP/NOKEEP.",
                        startLine = 8,
                        startColumn = 5,
                        endLine = 9,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle sequence clauses: SHARD/NOSHARD.",
                        startLine = 10,
                        startColumn = 5,
                        endLine = 11,
                        endColumn = 12,
                    ),
                )
        }

        test("accepts non-conflicting sequence clauses exactly") {
            NoConflictingSequenceClausesRule()
                .diagnostics(
                    """
                    CREATE SEQUENCE invoice_seq
                        SHARING = METADATA
                        START WITH 1
                        INCREMENT BY 1
                        MINVALUE 1
                        MAXVALUE 1000
                        CACHE 20
                        NOCYCLE
                        NOORDER
                        KEEP
                        SCALE EXTEND
                        SHARD NOEXTEND
                        GLOBAL;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
