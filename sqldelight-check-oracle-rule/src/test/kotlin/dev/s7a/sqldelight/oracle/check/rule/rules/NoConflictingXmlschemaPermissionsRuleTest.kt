package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NoConflictingXmlschemaPermissionsRuleTest :
    FunSpec({
        test("reports conflicting XMLSchema permission clauses exactly") {
            NoConflictingXmlschemaPermissionsRule()
                .diagnostics(
                    """
                    CREATE TABLE xml_documents OF XMLTYPE
                    XMLSCHEMA 'http://example.com/doc.xsd'
                    ALLOW NONSCHEMA
                    DISALLOW NONSCHEMA;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle XMLSchema permission clauses.",
                        startLine = 3,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 19,
                    ),
                )
        }

        test("reports multiple XMLSchema allow forms exactly") {
            NoConflictingXmlschemaPermissionsRule()
                .diagnostics(
                    """
                    CREATE TABLE xml_documents OF XMLTYPE
                    XMLSCHEMA 'http://example.com/doc.xsd'
                    ALLOW NONSCHEMA
                    ALLOW ANYSCHEMA;
                    """,
                ).summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Avoid conflicting Oracle XMLSchema permission clauses.",
                        startLine = 3,
                        startColumn = 1,
                        endLine = 4,
                        endColumn = 16,
                    ),
                )
        }

        test("accepts one XMLSchema permission clause") {
            NoConflictingXmlschemaPermissionsRule()
                .diagnostics(
                    """
                    CREATE TABLE xml_documents OF XMLTYPE
                    XMLSCHEMA 'http://example.com/doc.xsd'
                    ALLOW NONSCHEMA;
                    """,
                ).summaries() shouldBe emptyList()
        }
    })
