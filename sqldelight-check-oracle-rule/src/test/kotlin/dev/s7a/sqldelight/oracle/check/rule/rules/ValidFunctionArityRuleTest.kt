package dev.s7a.sqldelight.oracle.check.rule.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidFunctionArityRuleTest :
    FunSpec({
        test("reports too many arguments for no-argument functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    currentUser:
                    SELECT SYSDATE(1), USER(tenant_id)
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle expression SYSDATE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression USER does not accept parentheses.",
                        startLine = 2,
                        startColumn = 20,
                        endLine = 2,
                        endColumn = 24,
                    ),
                )
        }

        test("reports parentheses on Oracle datetime and user environment expressions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidNoParentheses:
                    SELECT SYSDATE(), USER(), DBTIMEZONE(), SESSIONTIMEZONE(), SYSTIMESTAMP()
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle expression SYSDATE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression USER does not accept parentheses.",
                        startLine = 2,
                        startColumn = 19,
                        endLine = 2,
                        endColumn = 23,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression DBTIMEZONE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 27,
                        endLine = 2,
                        endColumn = 37,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression SESSIONTIMEZONE does not accept parentheses.",
                        startLine = 2,
                        startColumn = 41,
                        endLine = 2,
                        endColumn = 56,
                    ),
                    DiagnosticSummary(
                        message = "Oracle expression SYSTIMESTAMP does not accept parentheses.",
                        startLine = 2,
                        startColumn = 60,
                        endLine = 2,
                        endColumn = 72,
                    ),
                )
        }

        test("reports missing precision in parenthesized current timestamp functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidCurrentTimestamp:
                    SELECT CURRENT_TIMESTAMP(), LOCALTIMESTAMP()
                    FROM dual;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function CURRENT_TIMESTAMP expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 25,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LOCALTIMESTAMP expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 29,
                        endLine = 2,
                        endColumn = 43,
                    ),
                )
        }

        test("reports too few arguments for exact arity functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidMath:
                    SELECT POWER(amount), NVL2(status, 'Y'), REGR_SLOPE(amount), CONCAT(label)
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function POWER expects 2 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NVL2 expects 3 argument(s), but got 2.",
                        startLine = 2,
                        startColumn = 23,
                        endLine = 2,
                        endColumn = 27,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REGR_SLOPE expects 2 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 42,
                        endLine = 2,
                        endColumn = 52,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CONCAT expects 2..2147483647 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 62,
                        endLine = 2,
                        endColumn = 68,
                    ),
                )
        }

        test("reports too many arguments for ranged arity functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidRegexp:
                    SELECT REGEXP_LIKE(name, '^a', 'i', 'extra'),
                      TO_DATE(created_text, 'YYYY-MM-DD', 'NLS_DATE_LANGUAGE = American', 'extra')
                    FROM customers;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function REGEXP_LIKE expects 2..3 argument(s), but got 4.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TO_DATE expects 1..3 argument(s), but got 4.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 10,
                    ),
                )
        }

        test("reports wrong arity for common Oracle scalar functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidScalars:
                    SELECT ROUND(), TRUNC(amount, 2, 3), SUBSTR(name), INSTR(name),
                      LPAD(name), RPAD(name, 10, ' ', 'x'), LOG(10), WIDTH_BUCKET(amount, 0, 100),
                      DECODE(status, 'A'), USERENV()
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function ROUND expects 1..2 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TRUNC expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 17,
                        endLine = 2,
                        endColumn = 22,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SUBSTR expects 2..3 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 38,
                        endLine = 2,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function INSTR expects 2..4 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 52,
                        endLine = 2,
                        endColumn = 57,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LPAD expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 7,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function RPAD expects 2..3 argument(s), but got 4.",
                        startLine = 3,
                        startColumn = 15,
                        endLine = 3,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LOG expects 2 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 41,
                        endLine = 3,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function WIDTH_BUCKET expects 4 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 50,
                        endLine = 3,
                        endColumn = 62,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DECODE expects 3..255 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function USERENV expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 24,
                        endLine = 4,
                        endColumn = 31,
                    ),
                )
        }

        test("reports too many arguments for Oracle decode") {
            val decodeArguments = (1..256).joinToString(", ") { index -> index.toString() }
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidDecode:
                    SELECT DECODE($decodeArguments)
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function DECODE expects 3..255 argument(s), but got 256.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 14,
                    ),
                )
        }

        test("reports wrong arity for Oracle utility scalar functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidUtilities:
                    SELECT CHR(), DUMP(label, 10, 20, 30, 40), LTRIM(label, 'x', 'y'),
                      RTRIM(label, 'x', 'y'), REPLACE(label), SYS_CONTEXT('USERENV'),
                      ORA_HASH(label, 100, 1, 2), STANDARD_HASH(label, 'SHA256', 'extra'),
                      NLSSORT(label, 'NLS_SORT = BINARY', 'extra')
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function CHR expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DUMP expects 1..4 argument(s), but got 5.",
                        startLine = 2,
                        startColumn = 15,
                        endLine = 2,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LTRIM expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 44,
                        endLine = 2,
                        endColumn = 49,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function RTRIM expects 1..2 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 8,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REPLACE expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 27,
                        endLine = 3,
                        endColumn = 34,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SYS_CONTEXT expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 43,
                        endLine = 3,
                        endColumn = 54,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ORA_HASH expects 1..3 argument(s), but got 4.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STANDARD_HASH expects 1..2 argument(s), but got 3.",
                        startLine = 4,
                        startColumn = 31,
                        endLine = 4,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLSSORT expects 1..2 argument(s), but got 3.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 10,
                    ),
                )
        }

        test("reports wrong arity for Oracle NLS and XML utility functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidNlsXmlUtilities:
                    SELECT BFILENAME('DATA_DIR'), NLS_UPPER(), NLS_LOWER(label, 'x', 'y'),
                      NLS_INITCAP(label, 'x', 'y'), NLS_CHARSET_ID(), NLS_CHARSET_NAME(),
                      NLS_CHARSET_DECL_LEN(10), EXTRACTVALUE(payload_xml),
                      EXISTSNODE(payload_xml, '/a', 'xmlns:x="urn:x"', 'extra'),
                      XMLISVALID(payload_xml, 'schema.xsd', 'extra')
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function BFILENAME expects 2 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 17,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_UPPER expects 1..2 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 31,
                        endLine = 2,
                        endColumn = 40,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_LOWER expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 44,
                        endLine = 2,
                        endColumn = 53,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_INITCAP expects 1..2 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_CHARSET_ID expects 1 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 33,
                        endLine = 3,
                        endColumn = 47,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_CHARSET_NAME expects 1 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 51,
                        endLine = 3,
                        endColumn = 67,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_CHARSET_DECL_LEN expects 2 argument(s), but got 1.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 23,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function EXTRACTVALUE expects 2..3 argument(s), but got 1.",
                        startLine = 4,
                        startColumn = 29,
                        endLine = 4,
                        endColumn = 41,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function EXISTSNODE expects 2..3 argument(s), but got 4.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function XMLISVALID expects 1..2 argument(s), but got 3.",
                        startLine = 6,
                        startColumn = 3,
                        endLine = 6,
                        endColumn = 13,
                    ),
                )
        }

        test("reports wrong arity for Oracle system conversion functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidSystemConversions:
                    SELECT SCN_TO_TIMESTAMP(), TIMESTAMP_TO_SCN(created_at, 'extra'),
                      ORA_DST_AFFECTED(created_at, 'extra'), ORA_DST_ERROR(),
                      ORA_DST_CONVERT(created_at, 'extra'), TZ_OFFSET()
                    FROM events;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function SCN_TO_TIMESTAMP expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 24,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TIMESTAMP_TO_SCN expects 1 argument(s), but got 2.",
                        startLine = 2,
                        startColumn = 28,
                        endLine = 2,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ORA_DST_AFFECTED expects 1 argument(s), but got 2.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ORA_DST_ERROR expects 1 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 42,
                        endLine = 3,
                        endColumn = 55,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ORA_DST_CONVERT expects 1 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TZ_OFFSET expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 41,
                        endLine = 4,
                        endColumn = 50,
                    ),
                )
        }

        test("reports wrong arity for additional typed Oracle utility functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidTypedUtilities:
                    SELECT COMPOSE(), CONVERT(label), DECOMPOSE(label, 'CANONICAL', 'extra'),
                      GROUPING(), GROUPING_ID(), GROUP_ID(status),
                      SYS_CONNECT_BY_PATH(label), BIN_TO_NUM()
                    FROM invoices
                    GROUP BY ROLLUP(status);
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function COMPOSE expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CONVERT expects 2..3 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 19,
                        endLine = 2,
                        endColumn = 26,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DECOMPOSE expects 1..2 argument(s), but got 3.",
                        startLine = 2,
                        startColumn = 35,
                        endLine = 2,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function GROUPING expects 1 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function GROUPING_ID expects 1..2147483647 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 15,
                        endLine = 3,
                        endColumn = 26,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function GROUP_ID expects 0 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 30,
                        endLine = 3,
                        endColumn = 38,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SYS_CONNECT_BY_PATH expects 2 argument(s), but got 1.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 22,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BIN_TO_NUM expects 1..2147483647 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 31,
                        endLine = 4,
                        endColumn = 41,
                    ),
                )
        }

        test("reports wrong arity for Oracle metadata utility functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidMetadataUtilities:
                    SELECT CARDINALITY(), COLLATION(label, 'extra'), NLS_COLLATION_ID(),
                      NLS_COLLATION_NAME(collation_id, 'extra'), CON_DBID_TO_ID(),
                      CON_GUID_TO_ID(guid, extra), CON_NAME_TO_ID(), CON_UID_TO_ID(uid, extra),
                      SYS_TYPEID()
                    FROM metadata_sources;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function CARDINALITY expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function COLLATION expects 1 argument(s), but got 2.",
                        startLine = 2,
                        startColumn = 23,
                        endLine = 2,
                        endColumn = 32,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_COLLATION_ID expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 50,
                        endLine = 2,
                        endColumn = 66,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NLS_COLLATION_NAME expects 1 argument(s), but got 2.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 21,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CON_DBID_TO_ID expects 1 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 46,
                        endLine = 3,
                        endColumn = 60,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CON_GUID_TO_ID expects 1 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 17,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CON_NAME_TO_ID expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 32,
                        endLine = 4,
                        endColumn = 46,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CON_UID_TO_ID expects 1 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 50,
                        endLine = 4,
                        endColumn = 63,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SYS_TYPEID expects 1 argument(s), but got 0.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 13,
                    ),
                )
        }

        test("reports wrong arity for Oracle specialized utility functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidSpecializedUtilities:
                    SELECT PHONIC_ENCODE(DOUBLE_METAPHONE), PHONIC_ENCODE(DOUBLE_METAPHONE, name, 10, 'extra'),
                      FUZZY_MATCH(LEVENSHTEIN, name), DOMAIN_CHECK(domain_name), DOMAIN_CHECK_TYPE(domain_name, value, extra),
                      DOMAIN_NAME()
                    FROM contacts;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function PHONIC_ENCODE expects 2..3 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 21,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function PHONIC_ENCODE expects 2..3 argument(s), but got 4.",
                        startLine = 2,
                        startColumn = 41,
                        endLine = 2,
                        endColumn = 54,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function FUZZY_MATCH expects 3..2147483647 argument(s), but got 2.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DOMAIN_CHECK expects 2 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 35,
                        endLine = 3,
                        endColumn = 47,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DOMAIN_CHECK_TYPE expects 2 argument(s), but got 3.",
                        startLine = 3,
                        startColumn = 62,
                        endLine = 3,
                        endColumn = 79,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function DOMAIN_NAME expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 14,
                    ),
                )
        }

        test("reports wrong arity for Oracle vector functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidVectors:
                    SELECT TO_VECTOR(), TO_VECTOR(payload, 3, FLOAT32, 'extra'),
                      VECTOR_DISTANCE(embedding), VECTOR_DISTANCE(embedding, target_embedding, COSINE, 'extra'),
                      FROM_VECTOR(), L1_DISTANCE(embedding), L2_DISTANCE(embedding, target_embedding, extra),
                      COSINE_DISTANCE(embedding), INNER_PRODUCT(embedding), HAMMING_DISTANCE(embedding),
                      JACCARD_DISTANCE(embedding), VECTOR_NORM(), VECTOR_DIMS(),
                      VECTOR_DIMENSION_COUNT(embedding, extra), VECTOR_DIMENSION_FORMAT()
                    FROM vector_items;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function TO_VECTOR expects 1..3 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 17,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function TO_VECTOR expects 1..3 argument(s), but got 4.",
                        startLine = 2,
                        startColumn = 21,
                        endLine = 2,
                        endColumn = 30,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_DISTANCE expects 2..3 argument(s), but got 1.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_DISTANCE expects 2..3 argument(s), but got 4.",
                        startLine = 3,
                        startColumn = 31,
                        endLine = 3,
                        endColumn = 46,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function FROM_VECTOR expects 1 argument(s), but got 0.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function L1_DISTANCE expects 2 argument(s), but got 1.",
                        startLine = 4,
                        startColumn = 18,
                        endLine = 4,
                        endColumn = 29,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function L2_DISTANCE expects 2 argument(s), but got 3.",
                        startLine = 4,
                        startColumn = 42,
                        endLine = 4,
                        endColumn = 53,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function COSINE_DISTANCE expects 2 argument(s), but got 1.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function INNER_PRODUCT expects 2 argument(s), but got 1.",
                        startLine = 5,
                        startColumn = 31,
                        endLine = 5,
                        endColumn = 44,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function HAMMING_DISTANCE expects 2 argument(s), but got 1.",
                        startLine = 5,
                        startColumn = 57,
                        endLine = 5,
                        endColumn = 73,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function JACCARD_DISTANCE expects 2 argument(s), but got 1.",
                        startLine = 6,
                        startColumn = 3,
                        endLine = 6,
                        endColumn = 19,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_NORM expects 1 argument(s), but got 0.",
                        startLine = 6,
                        startColumn = 32,
                        endLine = 6,
                        endColumn = 43,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_DIMS expects 1 argument(s), but got 0.",
                        startLine = 6,
                        startColumn = 47,
                        endLine = 6,
                        endColumn = 58,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_DIMENSION_COUNT expects 1 argument(s), but got 2.",
                        startLine = 7,
                        startColumn = 3,
                        endLine = 7,
                        endColumn = 25,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VECTOR_DIMENSION_FORMAT expects 1 argument(s), but got 0.",
                        startLine = 7,
                        startColumn = 45,
                        endLine = 7,
                        endColumn = 68,
                    ),
                )
        }

        test("reports wrong arity for Oracle analytic value functions") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidAnalytics:
                    SELECT ROW_NUMBER(1) OVER (ORDER BY id),
                      LAG() OVER (ORDER BY id),
                      LEAD(amount, 1, 0, 2) OVER (ORDER BY id),
                      FIRST_VALUE() OVER (ORDER BY id),
                      LAST_VALUE(amount, fallback_amount) OVER (ORDER BY id),
                      NTH_VALUE(amount) OVER (ORDER BY id),
                      NTILE() OVER (ORDER BY id),
                      RATIO_TO_REPORT(amount, fallback_amount) OVER ()
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function ROW_NUMBER expects 0 argument(s), but got 1.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LAG expects 1..3 argument(s), but got 0.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 6,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LEAD expects 1..3 argument(s), but got 4.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 7,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function FIRST_VALUE expects 1 argument(s), but got 0.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function LAST_VALUE expects 1 argument(s), but got 2.",
                        startLine = 6,
                        startColumn = 3,
                        endLine = 6,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NTH_VALUE expects 2 argument(s), but got 1.",
                        startLine = 7,
                        startColumn = 3,
                        endLine = 7,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function NTILE expects 1 argument(s), but got 0.",
                        startLine = 8,
                        startColumn = 3,
                        endLine = 8,
                        endColumn = 8,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function RATIO_TO_REPORT expects 1 argument(s), but got 2.",
                        startLine = 9,
                        startColumn = 3,
                        endLine = 9,
                        endColumn = 18,
                    ),
                )
        }

        test("reports wrong arity for Oracle aggregates") {
            val diagnostics =
                ValidFunctionArityRule().diagnostics(
                    """
                    invalidAggregates:
                    SELECT COUNT(),
                      SUM(amount, fallback_amount),
                      MAX(status, fallback_status),
                      ANY_VALUE(),
                      STATS_MODE(status, fallback),
                      REGR_COUNT(amount),
                      MEDIAN(),
                      APPROX_MEDIAN(),
                      APPROX_SUM(amount, extra),
                      APPROX_COUNT(),
                      APPROX_COUNT_DISTINCT(),
                      STDDEV(),
                      STDDEV_POP(amount, extra),
                      VARIANCE(),
                      VAR_POP(amount, extra),
                      CORR(amount),
                      COVAR_POP(amount),
                      SKEWNESS_POP(amount, extra),
                      KURTOSIS_SAMP(amount, extra),
                      BIT_AND_AGG(),
                      BIT_OR_AGG(flag_bits, extra),
                      BIT_XOR_AGG(),
                      BOOLEAN_AND_AGG(enabled, extra),
                      BOOLEAN_OR_AGG(),
                      CHECKSUM(flag_bits, extra)
                    FROM invoices;
                    """,
                )

            diagnostics.summaries() shouldBe
                listOf(
                    DiagnosticSummary(
                        message = "Oracle function COUNT expects 1 argument(s), but got 0.",
                        startLine = 2,
                        startColumn = 8,
                        endLine = 2,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SUM expects 1 argument(s), but got 2.",
                        startLine = 3,
                        startColumn = 3,
                        endLine = 3,
                        endColumn = 6,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function MAX expects 1 argument(s), but got 2.",
                        startLine = 4,
                        startColumn = 3,
                        endLine = 4,
                        endColumn = 6,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function ANY_VALUE expects 1 argument(s), but got 0.",
                        startLine = 5,
                        startColumn = 3,
                        endLine = 5,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STATS_MODE expects 1 argument(s), but got 2.",
                        startLine = 6,
                        startColumn = 3,
                        endLine = 6,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function REGR_COUNT expects 2 argument(s), but got 1.",
                        startLine = 7,
                        startColumn = 3,
                        endLine = 7,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function MEDIAN expects 1 argument(s), but got 0.",
                        startLine = 8,
                        startColumn = 3,
                        endLine = 8,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_MEDIAN expects 1 argument(s), but got 0.",
                        startLine = 9,
                        startColumn = 3,
                        endLine = 9,
                        endColumn = 16,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_SUM expects 1 argument(s), but got 2.",
                        startLine = 10,
                        startColumn = 3,
                        endLine = 10,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_COUNT expects 1 argument(s), but got 0.",
                        startLine = 11,
                        startColumn = 3,
                        endLine = 11,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function APPROX_COUNT_DISTINCT expects 1 argument(s), but got 0.",
                        startLine = 12,
                        startColumn = 3,
                        endLine = 12,
                        endColumn = 24,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STDDEV expects 1 argument(s), but got 0.",
                        startLine = 13,
                        startColumn = 3,
                        endLine = 13,
                        endColumn = 9,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function STDDEV_POP expects 1 argument(s), but got 2.",
                        startLine = 14,
                        startColumn = 3,
                        endLine = 14,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VARIANCE expects 1 argument(s), but got 0.",
                        startLine = 15,
                        startColumn = 3,
                        endLine = 15,
                        endColumn = 11,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function VAR_POP expects 1 argument(s), but got 2.",
                        startLine = 16,
                        startColumn = 3,
                        endLine = 16,
                        endColumn = 10,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CORR expects 2 argument(s), but got 1.",
                        startLine = 17,
                        startColumn = 3,
                        endLine = 17,
                        endColumn = 7,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function COVAR_POP expects 2 argument(s), but got 1.",
                        startLine = 18,
                        startColumn = 3,
                        endLine = 18,
                        endColumn = 12,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function SKEWNESS_POP expects 1 argument(s), but got 2.",
                        startLine = 19,
                        startColumn = 3,
                        endLine = 19,
                        endColumn = 15,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function KURTOSIS_SAMP expects 1 argument(s), but got 2.",
                        startLine = 20,
                        startColumn = 3,
                        endLine = 20,
                        endColumn = 16,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BIT_AND_AGG expects 1 argument(s), but got 0.",
                        startLine = 21,
                        startColumn = 3,
                        endLine = 21,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BIT_OR_AGG expects 1 argument(s), but got 2.",
                        startLine = 22,
                        startColumn = 3,
                        endLine = 22,
                        endColumn = 13,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BIT_XOR_AGG expects 1 argument(s), but got 0.",
                        startLine = 23,
                        startColumn = 3,
                        endLine = 23,
                        endColumn = 14,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BOOLEAN_AND_AGG expects 1 argument(s), but got 2.",
                        startLine = 24,
                        startColumn = 3,
                        endLine = 24,
                        endColumn = 18,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function BOOLEAN_OR_AGG expects 1 argument(s), but got 0.",
                        startLine = 25,
                        startColumn = 3,
                        endLine = 25,
                        endColumn = 17,
                    ),
                    DiagnosticSummary(
                        message = "Oracle function CHECKSUM expects 1 argument(s), but got 2.",
                        startLine = 26,
                        startColumn = 3,
                        endLine = 26,
                        endColumn = 11,
                    ),
                )
        }

        test("accepts valid fixed and ranged arity calls") {
            ValidFunctionArityRule().diagnostics(
                """
                validFunctions:
                SELECT SYSDATE,
                  USER,
                  POWER(amount, 2),
                  NVL2(status, 'Y', 'N'),
                  CONCAT(first_name, ' ', last_name),
                  COUNT(*),
                  SUM(amount),
                  MAX(status),
                  ANY_VALUE(status),
                  STATS_MODE(status),
                  MEDIAN(amount),
                  APPROX_MEDIAN(amount),
                  APPROX_SUM(amount),
                  APPROX_COUNT(*),
                  APPROX_COUNT_DISTINCT(customer_id),
                  STDDEV(amount),
                  STDDEV_POP(amount),
                  STDDEV_SAMP(amount),
                  VARIANCE(amount),
                  VAR_POP(amount),
                  VAR_SAMP(amount),
                  CORR(amount, quantity),
                  COVAR_POP(amount, quantity),
                  COVAR_SAMP(amount, quantity),
                  SKEWNESS_POP(amount),
                  SKEWNESS_SAMP(amount),
                  KURTOSIS_POP(amount),
                  KURTOSIS_SAMP(amount),
                  BIT_AND_AGG(flag_bits),
                  BIT_OR_AGG(flag_bits),
                  BIT_XOR_AGG(DISTINCT flag_bits),
                  BOOLEAN_AND_AGG(enabled),
                  BOOLEAN_OR_AGG(ALL enabled),
                  CHECKSUM(flag_bits),
                  CURRENT_TIMESTAMP(3),
                  LOCALTIMESTAMP(6),
                  REGR_SLOPE(amount, quantity),
                  COALESCE(nickname, name, 'unknown'),
                  ROUND(amount),
                  ROUND(amount, 2),
                  TRUNC(created_at, 'DD'),
                  SUBSTR(name, 1, 3),
                  INSTR(name, 'x', 1, 1),
                  LPAD(name, 10, ' '),
                  RPAD(name, 10),
                  LOG(10, amount),
                  WIDTH_BUCKET(amount, 0, 100, 10),
                  DECODE(status, 'A', 'active', 'unknown'),
                  USERENV('LANGUAGE'),
                  CHR(196),
                  DUMP(label, 1016),
                  LTRIM(label, 'x'),
                  RTRIM(label),
                  REPLACE(label, 'x', 'y'),
                  SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA', 64),
                  ORA_HASH(label, 100, 1),
                  STANDARD_HASH(label, 'SHA256'),
                  NLSSORT(label, 'NLS_SORT = BINARY'),
                  BFILENAME('DATA_DIR', 'payload.bin'),
                  NLS_UPPER(label, 'NLS_SORT = XGERMAN'),
                  NLS_LOWER(label),
                  NLS_INITCAP(label, 'NLS_SORT = BINARY_CI'),
                  NLS_CHARSET_ID('AL32UTF8'),
                  NLS_CHARSET_NAME(873),
                  NLS_CHARSET_DECL_LEN(10, 873),
                  CARDINALITY(items),
                  COLLATION(label),
                  NLS_COLLATION_ID('BINARY_CI'),
                  NLS_COLLATION_NAME(16382),
                  CON_DBID_TO_ID(dbid),
                  CON_GUID_TO_ID(guid),
                  CON_NAME_TO_ID(pdb_name),
                  CON_UID_TO_ID(con_uid),
                  SYS_TYPEID(object_value),
                  PHONIC_ENCODE(DOUBLE_METAPHONE, last_name),
                  PHONIC_ENCODE(DOUBLE_METAPHONE_ALT, last_name, 10),
                  FUZZY_MATCH(LEVENSHTEIN, first_name, alternate_name),
                  FUZZY_MATCH(BIGRAM, first_name, alternate_name, UNSCALED),
                  DOMAIN_CHECK(sample_domain, domain_value),
                  DOMAIN_CHECK_TYPE(sample_domain, domain_value),
                  DOMAIN_NAME(domain_value),
                  COMPOSE(label),
                  CONVERT(label, 'AL32UTF8', 'WE8ISO8859P1'),
                  DECOMPOSE(label, 'CANONICAL'),
                  SYS_CONNECT_BY_PATH(label, '/'),
                  BIN_TO_NUM(1, 0, 1),
                  EXTRACTVALUE(payload_xml, '/a'),
                  EXISTSNODE(payload_xml, '/a', 'xmlns:x="urn:x"'),
                  XMLISVALID(payload_xml, 'schema.xsd'),
                  SCN_TO_TIMESTAMP(scn_value),
                  TIMESTAMP_TO_SCN(created_at),
                  ORA_DST_AFFECTED(created_at),
                  ORA_DST_ERROR(created_at),
                  ORA_DST_CONVERT(created_at),
                  TZ_OFFSET('US/Eastern'),
                  ROW_NUMBER() OVER (ORDER BY id),
                  LAG(amount, 1, 0) OVER (ORDER BY id),
                  LEAD(amount) OVER (ORDER BY id),
                  FIRST_VALUE(amount) OVER (ORDER BY id),
                  LAST_VALUE(amount) OVER (ORDER BY id),
                  NTH_VALUE(amount, 2) OVER (ORDER BY id),
                  NTILE(4) OVER (ORDER BY id),
                  RATIO_TO_REPORT(amount) OVER (),
                  REGEXP_REPLACE(description, q'{literal ' marker, comma}', 'x'),
                  REGEXP_SUBSTR(description, '[A-Z]+', 1, 1, 'i', 0),
                  TO_TIMESTAMP(created_text, 'YYYY-MM-DD HH24:MI:SS'),
                  TO_VECTOR('[1,2,3]', 3, FLOAT32),
                  VECTOR_DISTANCE(embedding, target_embedding, COSINE),
                  FROM_VECTOR(embedding),
                  L1_DISTANCE(embedding, target_embedding),
                  L2_DISTANCE(embedding, target_embedding),
                  COSINE_DISTANCE(embedding, target_embedding),
                  INNER_PRODUCT(embedding, target_embedding),
                  HAMMING_DISTANCE(embedding, target_embedding),
                  JACCARD_DISTANCE(embedding, target_embedding),
                  VECTOR_NORM(embedding),
                  VECTOR_DIMS(embedding),
                  VECTOR_DIMENSION_COUNT(embedding),
                  VECTOR_DIMENSION_FORMAT(embedding),
                  GROUPING(status),
                  GROUPING_ID(status, region),
                  GROUP_ID()
                FROM customers;
                """,
            ) shouldBe emptyList()
        }

        test("ignores function-like text inside comments and strings") {
            ValidFunctionArityRule().diagnostics(
                """
                -- SELECT POWER(amount)
                SELECT 'REGEXP_LIKE(name, ''^a'', ''i'', ''x'')' AS sql_text,
                  POWER(amount, 2) AS squared_amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }

        test("ignores function-like text inside Oracle alternative quoted literals") {
            ValidFunctionArityRule().diagnostics(
                """
                SELECT q'{literal ' marker REGEXP_LIKE(name, '^a', 'i', 'x')}' AS sql_text,
                  nq'[literal ' marker POWER(amount)]' AS national_sql_text,
                  POWER(amount, 2) AS squared_amount
                FROM invoices;
                """,
            ) shouldBe emptyList()
        }
    })
