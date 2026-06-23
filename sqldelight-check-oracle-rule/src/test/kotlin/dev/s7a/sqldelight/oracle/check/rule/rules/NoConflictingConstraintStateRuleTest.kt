package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingConstraintStateRuleTest :
    FunSpec({
        test("reports conflicting CREATE TABLE constraint state clauses exactly") {
            NoConflictingConstraintStateRule()
                .diagnostics(
                    """
                    CREATE TABLE customer (
                        id NUMBER(19)
                            CONSTRAINT customer_pk PRIMARY KEY
                            ENABLE
                            DISABLE
                            VALIDATE
                            NOVALIDATE
                            DEFERRABLE
                            NOT DEFERRABLE
                            INITIALLY IMMEDIATE
                            INITIALLY DEFERRED
                            RELY
                            NORELY
                    );
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle constraint state clauses: ENABLE/DISABLE.",
                        startLine = 4,
                        startColumn = 9,
                        endLine = 5,
                        endColumn = 16,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle constraint state clauses: VALIDATE/NOVALIDATE.",
                        startLine = 6,
                        startColumn = 9,
                        endLine = 7,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle constraint state clauses: DEFERRABLE/NOT DEFERRABLE.",
                        startLine = 8,
                        startColumn = 9,
                        endLine = 9,
                        endColumn = 23,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle constraint state clauses: INITIALLY IMMEDIATE/INITIALLY DEFERRED.",
                        startLine = 10,
                        startColumn = 9,
                        endLine = 11,
                        endColumn = 27,
                    ),
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle constraint state clauses: RELY/NORELY.",
                        startLine = 12,
                        startColumn = 9,
                        endLine = 13,
                        endColumn = 15,
                    ),
                )
        }

        test("accepts non-conflicting constraint state clauses exactly") {
            NoConflictingConstraintStateRule()
                .diagnostics(
                    """
                    CREATE TABLE customer (
                        id NUMBER(19)
                            CONSTRAINT customer_pk PRIMARY KEY
                            ENABLE
                            VALIDATE
                            DEFERRABLE
                            INITIALLY DEFERRED
                            RELY
                    );

                    ALTER TABLE customer ADD CONSTRAINT customer_name_check CHECK (name IS NOT NULL) DISABLE NOVALIDATE;
                    """,
                ).summaries() shouldBe emptyList()
        }

        test("ignores non-table statements with overlapping clause names") {
            NoConflictingConstraintStateRule()
                .diagnostics(
                    """
                    CREATE INDEX customer_ix ON customer (id) VISIBLE INVISIBLE;
                    ALTER INDEX customer_ix USABLE UNUSABLE;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
