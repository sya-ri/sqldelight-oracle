package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.core.annotators.OptimisticLockCompilerAnnotator
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.psi.PsiDocumentManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

class OracleParserBackedTest :
    FunSpec({
        test("parses Oracle create table type names through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE sample (
                  id NUMBER(10) NOT NULL,
                  amount NUMBER(10, 2),
                  numeric_alias NUMERIC(8, 0),
                  decimal_alias DECIMAL(20, 0),
                  integer_alias INTEGER,
                  double_alias DOUBLE PRECISION,
                  name NATIONAL CHARACTER VARYING(100),
                  character_alias CHARACTER VARYING(100),
                  extended_text VARCHAR2(32767),
                  extended_national_text NVARCHAR2(32767),
                  payload JSON,
                  active BOOLEAN,
                  raw_uuid RAW(16),
                  extended_raw RAW(32767),
                  embedding VECTOR(3, FLOAT32),
                  created_at TIMESTAMP(6) WITH LOCAL TIME ZONE,
                  elapsed INTERVAL DAY TO SECOND
                );

                selectAll:
                SELECT id, amount, numeric_alias, decimal_alias, integer_alias, double_alias, name, character_alias, extended_text, extended_national_text, payload, active, raw_uuid, extended_raw, embedding, created_at, elapsed
                FROM sample;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL line comments through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE sample (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(64)
                );

                selectWithComments:
                SELECT id, name -- Result-column line comment.
                FROM sample
                WHERE id = 1; -- Trailing statement comment.
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle datetime and interval literals exactly") {
            val sql =
                """
                CREATE TABLE dual (
                  dummy VARCHAR2(1)
                );

                SELECT DATE '1998-12-25'
                FROM dual;

                SELECT TIMESTAMP '1997-01-31 09:26:50.124'
                FROM dual;

                SELECT TIMESTAMP '1999-04-15 8:00:00 US/Pacific'
                FROM dual;

                SELECT TIMESTAMP '2009-10-29 01:30:00' AT TIME ZONE 'US/Pacific'
                FROM dual;

                SELECT INTERVAL '123-2' YEAR(3) TO MONTH
                FROM dual;

                SELECT INTERVAL '300' MONTH(3)
                FROM dual;

                SELECT INTERVAL '4 5:12:10.222' DAY TO SECOND(3)
                FROM dual;

                SELECT INTERVAL '11:12:10.2222222' HOUR TO SECOND(7)
                FROM dual;

                SELECT INTERVAL '10:22' MINUTE TO SECOND
                FROM dual;

                SELECT INTERVAL '30.12345' SECOND(2, 4)
                FROM dual;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle pseudocolumns exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100)
                );

                CREATE TABLE employeeSeq (
                  NEXTVAL NUMBER,
                  CURRVAL NUMBER
                );

                SELECT employeeSeq.NEXTVAL
                FROM employeeSeq;

                SELECT employeeSeq.CURRVAL
                FROM employeeSeq;

                SELECT ROWNUM, ROWID, ORA_ROWSCN, name
                FROM employees
                WHERE ROWNUM < 11;

                SELECT LEVEL, CONNECT_BY_ISLEAF, CONNECT_BY_ISCYCLE, name
                FROM employees
                WHERE LEVEL <= 3;

                SELECT COLUMN_VALUE, OBJECT_ID, OBJECT_VALUE, XMLDATA, ORA_SHARDSPACE_NAME
                FROM employees;

                SELECT DBTIMEZONE,
                  ORA_INVOKING_USER,
                  ORA_INVOKING_USERID,
                  SESSIONTIMEZONE,
                  UID,
                  USER
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle version query pseudocolumns exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100)
                );

                SELECT VERSIONS_STARTSCN,
                  VERSIONS_STARTTIME,
                  VERSIONS_ENDSCN,
                  VERSIONS_ENDTIME,
                  VERSIONS_XID,
                  VERSIONS_OPERATION
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle hierarchical and shard operators exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  manager_id NUMBER,
                  name VARCHAR2(100),
                  shard_class VARCHAR2(8)
                );

                SELECT CONNECT_BY_ROOT name, id
                FROM employees
                WHERE PRIOR id = manager_id;

                SELECT SHARD_CHUNK_ID(NULL, shard_class, id)
                FROM employees;

                SELECT JSON_ID('OID'), JSON_ID('UUID')
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle data quality operators exactly") {
            val sql =
                """
                CREATE TABLE contact_samples (
                  id NUMBER PRIMARY KEY,
                  first_name VARCHAR2(100),
                  last_name VARCHAR2(100),
                  alternate_name VARCHAR2(100)
                );

                SELECT FUZZY_MATCH(LEVENSHTEIN, first_name, alternate_name),
                  FUZZY_MATCH(DAMERAU_LEVENSHTEIN, first_name, alternate_name, RELATE_TO_SHORTER),
                  FUZZY_MATCH(WHOLE_WORD_MATCH, last_name, alternate_name, EDIT_TOLERANCE 60),
                  FUZZY_MATCH(BIGRAM, first_name, alternate_name, UNSCALED),
                  PHONIC_ENCODE(DOUBLE_METAPHONE, last_name),
                  PHONIC_ENCODE(DOUBLE_METAPHONE_ALT, last_name, 10)
                FROM contact_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle domain and UUID functions exactly") {
            val sql =
                """
                CREATE TABLE domain_uuid_samples (
                  id NUMBER PRIMARY KEY,
                  sample_domain VARCHAR2(100),
                  domain_value VARCHAR2(100),
                  uuid_text VARCHAR2(36),
                  uuid_raw RAW(16)
                );

                SELECT DOMAIN_CHECK(sample_domain, domain_value),
                  DOMAIN_CHECK_TYPE(sample_domain, domain_value),
                  DOMAIN_NAME(domain_value),
                  UUID(),
                  UUID(4),
                  IS_UUID(uuid_text),
                  UUID_TO_RAW(uuid_text),
                  RAW_TO_UUID(uuid_raw)
                FROM domain_uuid_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle single-row utility functions exactly") {
            val sql =
                """
                CREATE TABLE single_row_function_samples (
                  id NUMBER PRIMARY KEY,
                  label VARCHAR2(100),
                  raw_value RAW(16),
                  amount NUMBER,
                  enabled BOOLEAN
                );

                SELECT CON_DBID_TO_ID(amount) AS container_id_from_dbid,
                  CON_GUID_TO_ID(raw_value) AS container_id_from_guid,
                  CON_NAME_TO_ID(label) AS container_id_from_name,
                  CON_UID_TO_ID(amount) AS container_id_from_uid,
                  COSH(amount) AS hyperbolic_cosine,
                  DUMP(label) AS dumped_label,
                  LNNVL(enabled) AS not_false_enabled,
                  NULLIF(label, 'unknown') AS nullable_label,
                  ORA_HASH(label) AS hashed_label,
                  SINH(amount) AS hyperbolic_sine,
                  STANDARD_HASH(label) AS standard_hash,
                  SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
                  SYS_GUID() AS generated_guid,
                  SYS_TYPEID(label) AS type_identifier,
                  TANH(amount) AS hyperbolic_tangent,
                  USERENV('LANGUAGE') AS legacy_language,
                  VSIZE(label) AS label_size
                FROM single_row_function_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle COLLATE operator exactly") {
            val sql =
                """
                CREATE TABLE customers (
                  id NUMBER PRIMARY KEY,
                  last_name VARCHAR2(100)
                );

                SELECT id,
                  last_name COLLATE BINARY_CI AS normalized_last_name,
                  ('prefix' COLLATE USING_NLS_COMP) AS normalized_literal
                FROM customers
                WHERE last_name COLLATE BINARY_CI = 'smith'
                ORDER BY last_name COLLATE GENERIC_M;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle multiset operators and conditions exactly") {
            val sql =
                """
                CREATE TABLE collection_samples (
                  id NUMBER PRIMARY KEY,
                  tags VARCHAR2(100),
                  other_tags VARCHAR2(100),
                  tag_value VARCHAR2(40)
                );

                SELECT id,
                  tags MULTISET UNION other_tags AS all_tags,
                  tags MULTISET EXCEPT DISTINCT other_tags AS removed_tags,
                  tags MULTISET INTERSECT ALL other_tags AS common_tags
                FROM collection_samples
                WHERE tags IS A SET
                  AND other_tags IS NOT EMPTY
                  AND tag_value MEMBER OF tags
                  AND tags NOT SUBMULTISET OF other_tags;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle arithmetic and concatenation operators exactly") {
            val sql =
                """
                CREATE TABLE operator_samples (
                  id NUMBER PRIMARY KEY,
                  amount NUMBER,
                  tax NUMBER,
                  first_name VARCHAR2(100),
                  last_name VARCHAR2(100)
                );

                SELECT amount + tax AS gross_amount,
                  amount - tax AS net_amount,
                  amount * 2 AS doubled_amount,
                  amount / 2 AS half_amount,
                  -amount AS negative_amount,
                  first_name || ' ' || last_name AS full_name
                FROM operator_samples
                WHERE amount + tax > 100
                  AND first_name || last_name IS NOT NULL;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle floating-point and IS OF type conditions exactly") {
            val sql =
                """
                CREATE TABLE measurement_samples (
                  id NUMBER PRIMARY KEY,
                  payload OBJECT,
                  salary BINARY_DOUBLE,
                  commission_pct BINARY_FLOAT
                );

                SELECT id
                FROM measurement_samples
                WHERE salary IS NAN
                  AND commission_pct IS NOT INFINITE
                  AND payload IS OF TYPE (employee_t)
                  AND payload IS NOT OF (ONLY part_time_emp_t, hr.contractor_t);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle IS JSON conditions exactly") {
            val sql =
                """
                CREATE TABLE json_samples (
                  id NUMBER PRIMARY KEY,
                  doc CLOB,
                  expected_doc CLOB,
                  search_text VARCHAR2(100)
                );

                SELECT id
                FROM json_samples
                WHERE doc IS JSON STRICT WITH UNIQUE KEYS
                  AND expected_doc IS NOT JSON VALIDATE USING '{"type":"object"}';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle XML path conditions exactly") {
            val sql =
                """
                CREATE TABLE xml_path_samples (
                  id NUMBER PRIMARY KEY,
                  res XMLTYPE
                );

                SELECT id
                FROM xml_path_samples
                WHERE EQUALS_PATH(res, '/sys/schemas/OE/www.example.com') = 1
                  AND UNDER_PATH(res, '/sys/schemas/OE/www.example.com', 1, 2) = 1;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle Text conditions and score operators exactly") {
            val sql =
                """
                CREATE TABLE oracle_text_samples (
                  id NUMBER PRIMARY KEY,
                  content CLOB,
                  category VARCHAR2(100),
                  query_text VARCHAR2(4000)
                );

                SELECT id, SCORE(1) AS relevance
                FROM oracle_text_samples
                WHERE CONTAINS(content, 'oracle database', 1) > 0
                  AND CATSEARCH(category, 'database', 'order by category') > 0
                  AND MATCHES(query_text, 'oracle') > 0;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle Unicode LIKE conditions exactly") {
            val sql =
                """
                CREATE TABLE name_samples (
                  id NUMBER PRIMARY KEY,
                  display_name VARCHAR2(100),
                  normalized_name NVARCHAR2(100)
                );

                SELECT id
                FROM name_samples
                WHERE display_name LIKEC 'A\_%' ESCAPE '\'
                  AND normalized_name NOT LIKE2 'B%'
                  AND (display_name) LIKE4 'C_';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle REGEXP_LIKE conditions exactly") {
            val sql =
                """
                CREATE TABLE regex_samples (
                  id NUMBER PRIMARY KEY,
                  first_name VARCHAR2(100),
                  last_name VARCHAR2(100)
                );

                SELECT id
                FROM regex_samples
                WHERE REGEXP_LIKE(first_name, '^Ste(v|ph)en${'$'}')
                  AND REGEXP_LIKE(last_name, '([aeiou])\1', 'i');
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle caret inequality condition exactly") {
            val sql =
                """
                CREATE TABLE salary_samples (
                  id NUMBER PRIMARY KEY,
                  salary NUMBER,
                  department_id NUMBER,
                  status VARCHAR2(20)
                );

                SELECT id
                FROM salary_samples
                WHERE status ^= 'inactive'
                  AND department_id ^= 30;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle BETWEEN, EXISTS, and IN conditions exactly") {
            val sql =
                """
                CREATE TABLE departments (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100)
                );

                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  status VARCHAR2(20),
                  salary NUMBER
                );

                SELECT id
                FROM employees
                WHERE salary BETWEEN 50000 AND 100000
                  AND department_id NOT BETWEEN 90 AND 99
                  AND status IN ('ACTIVE', 'PENDING')
                  AND department_id IN (
                    SELECT id
                    FROM departments
                    WHERE name IN ('SALES', 'HR')
                  )
                  AND (department_id, status) IN ((10, 'ACTIVE'), (20, 'PENDING'))
                  AND (department_id, status) NOT IN (
                    SELECT id, name
                    FROM departments
                  )
                  AND EXISTS (
                    SELECT id
                    FROM departments
                    WHERE departments.id = employees.department_id
                  )
                  AND salary >= ALL (1400, 3000)
                  AND salary = ANY (
                    SELECT salary
                    FROM employees e
                    WHERE e.department_id = employees.department_id
                  )
                  AND department_id != SOME (10, 20)
                  AND (department_id, salary) = (10, 75000)
                  AND (department_id, salary) <> (
                    SELECT id, 0
                    FROM departments
                    WHERE name = 'HR'
                  )
                  AND (department_id, salary) >= ALL (
                    SELECT department_id, salary
                    FROM employees e
                    WHERE e.status = employees.status
                  )
                  AND (department_id, status) = ANY ((10, 'ACTIVE'), (20, 'PENDING'))
                  AND (department_id, status) != SOME ((30, 'INACTIVE'), (40, 'SUSPENDED'))
                  AND ((department_id, status) (
                    VALUES (10, 'ACTIVE'),
                           (20, 'PENDING')
                  ) AS allowed_departments (department_id, status));
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle type constructor expressions exactly") {
            val sql =
                """
                CREATE TABLE constructor_samples (
                  id NUMBER PRIMARY KEY,
                  street VARCHAR2(100),
                  postal_code NUMBER,
                  city VARCHAR2(80),
                  state_code VARCHAR2(2),
                  country_code VARCHAR2(2)
                );

                SELECT NEW cust_address_typ(street, postal_code, city, state_code, country_code) AS address_value,
                  NEW hr.cust_address_typ(street, postal_code, city, state_code, country_code) AS schema_address_value,
                  cust_address_typ(street, postal_code, city, state_code, country_code) AS optional_new_address_value,
                  address_book_t() AS empty_address_book,
                  address_book_t(cust_address_typ(street, postal_code, city, state_code, country_code)) AS address_book
                FROM constructor_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle cursor expressions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100)
                );

                SELECT CURSOR(
                  SELECT name
                  FROM employees
                )
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle case expressions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  status VARCHAR2(20),
                  salary NUMBER
                );

                SELECT CASE status
                  WHEN 'ACTIVE' THEN 'current'
                  WHEN 'INACTIVE' THEN 'former'
                  ELSE 'unknown'
                END,
                CASE
                  WHEN salary > 100000 THEN 'high'
                  WHEN salary > 50000 THEN 'mid'
                  ELSE 'standard'
                END
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle scalar subquery expressions exactly") {
            val sql =
                """
                CREATE TABLE departments (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100)
                );

                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  salary NUMBER
                );

                SELECT id,
                  (SELECT name FROM departments WHERE departments.id = employees.department_id),
                  salary + (SELECT 100 FROM departments WHERE departments.id = employees.department_id)
                FROM employees
                WHERE salary > (SELECT 50000 FROM departments WHERE departments.id = employees.department_id);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle placeholder expressions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  name VARCHAR2(100),
                  salary NUMBER
                );

                SELECT id, name
                FROM employees
                WHERE department_id = :department_id
                  AND salary > ?;

                INSERT INTO employees (id, department_id, name, salary)
                VALUES (:id, :department_id, :name, ?);

                UPDATE employees
                SET salary = :salary
                WHERE id = :1
                  OR id = ?;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle type conversion expressions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100),
                  active BOOLEAN,
                  payload JSON
                );

                SELECT CAST(name AS VARCHAR2(200)),
                  CAST(id AS BINARY_DOUBLE),
                  CAST(active AS NUMBER),
                  TREAT(payload AS JSON)
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL JSON generation functions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100),
                  details JSON
                );

                SELECT JSON_OBJECT(
                  KEY 'id' VALUE id,
                  'name' : name,
                  'details' VALUE details FORMAT JSON,
                  *
                  ABSENT ON NULL
                  RETURNING JSON
                  STRICT
                  WITH UNIQUE KEYS
                ),
                  JSON_ARRAY(
                    id,
                    details FORMAT JSON,
                    NULL NULL ON NULL
                    RETURNING VARCHAR2(4000)
                    STRICT
                  )
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL JSON query functions exactly") {
            val sql =
                """
                CREATE TABLE documents (
                  id NUMBER PRIMARY KEY,
                  payload JSON
                );

                SELECT JSON_VALUE(
                  payload,
                  '$.employee.id'
                  RETURNING NUMBER
                  ERROR ON ERROR
                  DEFAULT 0 ON EMPTY
                ),
                  JSON_QUERY(
                    payload FORMAT JSON,
                    '$.items[*]'
                    RETURNING JSON
                    WITH CONDITIONAL WRAPPER
                    DISALLOW SCALARS
                    EMPTY ARRAY ON ERROR
                    NULL ON EMPTY
                  ),
                  JSON_SERIALIZE(
                    payload
                    RETURNING VARCHAR2(4000)
                    PRETTY ASCII ORDERED
                    TRUNCATE
                    ERROR ON ERROR
                  )
                FROM documents;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL JSON transform function exactly") {
            val sql =
                """
                CREATE TABLE documents (
                  id NUMBER PRIMARY KEY,
                  payload JSON,
                  threshold NUMBER
                );

                SELECT JSON_TRANSFORM(
                  payload,
                  SET '$.lastUpdated' = '2026-01-01T00:00:00',
                  INSERT '$.status' = 'active' ERROR ON EXISTING,
                  REPLACE '$.score' = threshold IGNORE ON MISSING,
                  APPEND '$.items' = JSON_ARRAY('new'),
                  PREPEND '$.items' = '$.defaults',
                  REMOVE '$.ssn',
                  RENAME '$.oldName' = 'newName',
                  KEEP '$.public', '$.items',
                  SORT '$.items',
                  NESTED PATH '$.address' (
                    SET '$.verified' = 1
                  ),
                  CASE
                    WHEN '$.archived' THEN (
                      REMOVE '$.active'
                    )
                    ELSE (
                      SET '$.active' = 1
                    )
                  END
                  RETURNING JSON
                  PASSING threshold AS ${'$'}threshold
                )
                FROM documents;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL JSON scalar function exactly") {
            val sql =
                """
                CREATE TABLE events (
                  id NUMBER PRIMARY KEY,
                  occurred_at TIMESTAMP,
                  active BOOLEAN,
                  amount NUMBER(10, 2)
                );

                SELECT JSON_SCALAR(id),
                  JSON_SCALAR(occurred_at),
                  JSON_SCALAR(active),
                  JSON_SCALAR(amount)
                FROM events;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL XML functions exactly") {
            val sql =
                """
                CREATE TABLE departments (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(100),
                  warehouse_spec XMLTYPE
                );

                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  name VARCHAR2(100)
                );

                SELECT XMLELEMENT(
                  "Department",
                  XMLATTRIBUTES(d.id AS "ID", d.name),
                  XMLAGG(XMLELEMENT("Employee", e.name) ORDER BY e.name)
                ),
                  XMLCAST(XMLQUERY('/Warehouse/Area' PASSING d.warehouse_spec RETURNING CONTENT) AS NUMBER),
                  XMLFOREST(d.name AS "Name", d.id AS "Identifier"),
                  XMLPARSE(CONTENT '<Warehouse/>' WELLFORMED),
                  XMLPI(NAME "Department", d.name),
                  XMLSERIALIZE(CONTENT d.warehouse_spec AS CLOB INDENT SIZE = 2 HIDE DEFAULTS),
                  DEPTH(1),
                  EXISTSNODE(d.warehouse_spec, '/Warehouse'),
                  EXTRACT(d.warehouse_spec, '/Warehouse'),
                  EXTRACTVALUE(d.warehouse_spec, '/Warehouse/Area'),
                  PATH(1),
                  SYS_DBURIGEN(d.id),
                  SYS_XMLAGG(XMLELEMENT("Employee", e.name)),
                  SYS_XMLGEN(d.name),
                  XMLCDATA(d.name),
                  XMLCOLATTVAL(d.name),
                  XMLCOMMENT(d.name),
                  XMLCONCAT(XMLELEMENT("Name", d.name), XMLELEMENT("Id", d.id)),
                  XMLDIFF(d.warehouse_spec, XMLTYPE('<Warehouse/>')),
                  XMLISVALID(d.warehouse_spec),
                  XMLPATCH(d.warehouse_spec, XMLTYPE('<Warehouse/>')),
                  XMLSEQUENCE(EXTRACT(d.warehouse_spec, '/Warehouse')),
                  XMLTRANSFORM(d.warehouse_spec, XMLTYPE('<Warehouse/>'))
                FROM departments d,
                  employees e
                WHERE e.department_id = d.id
                  AND XMLEXISTS('/Warehouse[Area > 50000]' PASSING d.warehouse_spec)
                GROUP BY d.id, d.name, d.warehouse_spec;

                SELECT d.id
                FROM departments d,
                  XMLTABLE(
                    XMLNAMESPACES(DEFAULT 'http://example.com/warehouse'),
                    '/Warehouse'
                    PASSING BY VALUE XMLTYPE('<Warehouse/>')
                    RETURNING SEQUENCE BY REF
                    COLUMNS
                      line_number FOR ORDINALITY,
                      "Water" VARCHAR2(6) PATH 'WaterAccess' DEFAULT 'N',
                      "Rail" VARCHAR2(6) PATH 'RailAccess'
                  ) warehouse;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle SQL vector functions exactly") {
            val sql =
                """
                CREATE TABLE documents (
                  id NUMBER PRIMARY KEY,
                  body CLOB,
                  embedding VECTOR(3, FLOAT32)
                );

                SELECT VECTOR('[1,2,3]'),
                  TO_VECTOR('[1,2,3]', 3, FLOAT32),
                  VECTOR_DISTANCE(embedding, TO_VECTOR('[1,2,3]', 3, FLOAT32), COSINE),
                  VECTOR_EMBEDDING(all_minilm_l12 USING body AS DATA),
                  VECTOR_SERIALIZE(embedding RETURNING CLOB)
                FROM documents
                ORDER BY VECTOR_DISTANCE(embedding, VECTOR('[1,2,3]'));
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses representative Oracle single-row functions exactly") {
            val sql =
                """
                CREATE TABLE function_samples (
                  id NUMBER PRIMARY KEY,
                  amount NUMBER,
                  fallback_amount NUMBER,
                  first_name VARCHAR2(100),
                  last_name VARCHAR2(100),
                  raw_value RAW(16),
                  created_at TIMESTAMP
                );

                SELECT TO_CHAR(created_at, 'YYYY-MM-DD'),
                  TO_NUMBER('123.45'),
                  TO_DATE('2026-01-01', 'YYYY-MM-DD'),
                  TO_TIMESTAMP('2026-01-01 10:30:00', 'YYYY-MM-DD HH24:MI:SS'),
                  RAWTOHEX(raw_value),
                  HEXTORAW('00112233445566778899AABBCCDDEEFF'),
                  LENGTH(first_name),
                  INSTR(last_name, 'a'),
                  NVL(amount, fallback_amount),
                  NVL2(first_name, amount, fallback_amount),
                  COALESCE(amount, fallback_amount, 0),
                  DECODE(first_name, 'A', amount, fallback_amount),
                  GREATEST(amount, fallback_amount),
                  LEAST(amount, fallback_amount),
                  NANVL(amount, fallback_amount)
                FROM function_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle datetime single-row functions exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  hire_date DATE,
                  shift_time TIMESTAMP,
                  shift_time_tz TIMESTAMP WITH TIME ZONE
                );

                SELECT id,
                  ADD_MONTHS(hire_date, 1) AS next_month,
                  EXTRACT(YEAR FROM hire_date) AS hire_year,
                  EXTRACT(TIMEZONE_REGION FROM shift_time_tz) AS shift_timezone_region,
                  FROM_TZ(shift_time, '3:00') AS shift_timestamp_tz,
                  LAST_DAY(hire_date) AS month_end,
                  NEW_TIME(hire_date, 'PST', 'EST') AS eastern_hire_time,
                  NEXT_DAY(hire_date, 'TUESDAY') AS next_tuesday,
                  NUMTODSINTERVAL(100, 'DAY') AS one_hundred_days,
                  NUMTOYMINTERVAL(1, 'YEAR') AS one_year,
                  ORA_DST_AFFECTED(shift_time_tz) AS dst_affected,
                  ORA_DST_CONVERT(shift_time_tz) AS dst_converted,
                  ORA_DST_ERROR(shift_time_tz) AS dst_error,
                  SYS_EXTRACT_UTC(shift_time_tz) AS utc_shift_time,
                  TO_DSINTERVAL('100 10:00:00') AS day_second_interval,
                  TO_TIMESTAMP_TZ('1999-12-01 10:00:00 -8:00', 'YYYY-MM-DD HH:MI:SS TZH:TZM') AS parsed_timestamp_tz,
                  TO_YMINTERVAL('01-02') AS year_month_interval,
                  TZ_OFFSET('US/Eastern') AS eastern_offset,
                  MONTHS_BETWEEN(CURRENT_DATE, hire_date) AS tenure_months
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle character single-row functions exactly") {
            val sql =
                """
                CREATE TABLE names (
                  id NUMBER PRIMARY KEY,
                  first_name VARCHAR2(100),
                  last_name VARCHAR2(100),
                  label VARCHAR2(100)
                );

                SELECT CHR(65) AS chr_value,
                  CONCAT(first_name, last_name) AS full_name,
                  INITCAP(label) AS title_label,
                  LOWER(label) AS lower_label,
                  LPAD(label, 10, '*') AS left_padded,
                  LTRIM(label, '*') AS left_trimmed,
                  NCHR(12354) AS nchr_value,
                  NLS_INITCAP(label, 'NLS_SORT = XGERMAN') AS nls_title,
                  NLS_LOWER(label, 'NLS_SORT = XGERMAN') AS nls_lower,
                  NLS_UPPER(label, 'NLS_SORT = XGERMAN') AS nls_upper,
                  NLSSORT(label, 'NLS_SORT = XGERMAN') AS sort_key,
                  REGEXP_REPLACE(label, '[aeiou]', '*') AS replaced_regex,
                  REGEXP_SUBSTR(label, '[[:alpha:]]+') AS regex_part,
                  RPAD(label, 10, '*') AS right_padded,
                  RTRIM(label, '*') AS right_trimmed,
                  SOUNDEX(label) AS soundex_value,
                  SUBSTR(label, 1, 3) AS label_prefix,
                  TRANSLATE(label, 'abc', 'xyz') AS translated_label,
                  TRIM(label) AS trimmed_label,
                  UPPER(label) AS upper_label,
                  ASCII(label) AS ascii_value
                FROM names;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle calendar functions exactly") {
            val sql =
                """
                CREATE TABLE calendar_dates (
                  id NUMBER PRIMARY KEY,
                  d DATE,
                  fiscal_start DATE,
                  periods NUMBER,
                  fmt VARCHAR2(100),
                  nls VARCHAR2(100),
                  restated VARCHAR2(20)
                );

                SELECT CALENDAR_YEAR(d) AS calendar_year,
                  CALENDAR_QUARTER(d, fmt, nls) AS calendar_quarter,
                  CALENDAR_MONTH(d, fmt, nls) AS calendar_month,
                  CALENDAR_WEEK(d, fmt, nls) AS calendar_week,
                  CALENDAR_DAY(d, fmt, nls) AS calendar_day,
                  FISCAL_YEAR(d, fiscal_start, fmt, nls) AS fiscal_year,
                  FISCAL_QUARTER(d, fiscal_start, fmt, nls) AS fiscal_quarter,
                  FISCAL_MONTH(d, fiscal_start, fmt, nls) AS fiscal_month,
                  FISCAL_WEEK(d, fiscal_start, fmt, nls) AS fiscal_week,
                  FISCAL_DAY(d, fiscal_start, fmt, nls) AS fiscal_day,
                  RETAIL_YEAR(d, fmt, restated, nls) AS retail_year,
                  RETAIL_QUARTER(d, fmt, restated, nls) AS retail_quarter,
                  RETAIL_MONTH(d, fmt, restated, nls) AS retail_month,
                  RETAIL_WEEK(d, fmt, restated, nls) AS retail_week,
                  RETAIL_DAY(d, fmt, restated, nls) AS retail_day,
                  CALENDAR_YEAR_START_DATE(d, nls) AS calendar_year_start,
                  CALENDAR_YEAR_END_DATE(d, nls) AS calendar_year_end,
                  CALENDAR_QUARTER_START_DATE(d, nls) AS calendar_quarter_start,
                  CALENDAR_QUARTER_END_DATE(d, nls) AS calendar_quarter_end,
                  CALENDAR_MONTH_START_DATE(d, nls) AS calendar_month_start,
                  CALENDAR_MONTH_END_DATE(d, nls) AS calendar_month_end,
                  CALENDAR_WEEK_START_DATE(d, nls) AS calendar_week_start,
                  CALENDAR_WEEK_END_DATE(d, nls) AS calendar_week_end,
                  FISCAL_YEAR_START_DATE(d, fiscal_start, nls) AS fiscal_year_start_date,
                  FISCAL_YEAR_END_DATE(d, fiscal_start, nls) AS fiscal_year_end_date,
                  FISCAL_QUARTER_START_DATE(d, fiscal_start, nls) AS fiscal_quarter_start,
                  FISCAL_QUARTER_END_DATE(d, fiscal_start, nls) AS fiscal_quarter_end,
                  FISCAL_MONTH_START_DATE(d, fiscal_start, nls) AS fiscal_month_start,
                  FISCAL_MONTH_END_DATE(d, fiscal_start, nls) AS fiscal_month_end,
                  FISCAL_WEEK_START_DATE(d, fiscal_start, nls) AS fiscal_week_start,
                  FISCAL_WEEK_END_DATE(d, fiscal_start, nls) AS fiscal_week_end,
                  RETAIL_YEAR_START_DATE(d, restated) AS retail_year_start,
                  RETAIL_YEAR_END_DATE(d, restated) AS retail_year_end,
                  RETAIL_QUARTER_START_DATE(d, restated) AS retail_quarter_start,
                  RETAIL_QUARTER_END_DATE(d, restated) AS retail_quarter_end,
                  RETAIL_MONTH_START_DATE(d, restated) AS retail_month_start,
                  RETAIL_MONTH_END_DATE(d, restated) AS retail_month_end,
                  RETAIL_WEEK_START_DATE(d, restated) AS retail_week_start,
                  RETAIL_WEEK_END_DATE(d, restated) AS retail_week_end,
                  CALENDAR_YEAR_NUMBER(d, nls) AS calendar_year_number,
                  CALENDAR_QUARTER_OF_YEAR(d, nls) AS calendar_quarter_of_year,
                  CALENDAR_MONTH_OF_YEAR(d, nls) AS calendar_month_of_year,
                  CALENDAR_MONTH_OF_QUARTER(d, nls) AS calendar_month_of_quarter,
                  CALENDAR_WEEK_OF_YEAR(d, nls) AS calendar_week_of_year,
                  CALENDAR_DAY_OF_YEAR(d, nls) AS calendar_day_of_year,
                  CALENDAR_DAY_OF_QUARTER(d, nls) AS calendar_day_of_quarter,
                  CALENDAR_DAY_OF_MONTH(d, nls) AS calendar_day_of_month,
                  CALENDAR_DAY_OF_WEEK(d, 'DATE', nls) AS calendar_day_of_week,
                  FISCAL_YEAR_NUMBER(d, fiscal_start, nls) AS fiscal_year_number,
                  FISCAL_QUARTER_OF_YEAR(d, fiscal_start, nls) AS fiscal_quarter_of_year,
                  FISCAL_MONTH_OF_YEAR(d, fiscal_start, 'DATE', nls) AS fiscal_month_of_year,
                  FISCAL_MONTH_OF_QUARTER(d, fiscal_start, nls) AS fiscal_month_of_quarter,
                  FISCAL_WEEK_OF_YEAR(d, fiscal_start, nls) AS fiscal_week_of_year,
                  FISCAL_DAY_OF_YEAR(d, fiscal_start, nls) AS fiscal_day_of_year,
                  FISCAL_DAY_OF_QUARTER(d, fiscal_start, nls) AS fiscal_day_of_quarter,
                  FISCAL_DAY_OF_MONTH(d, fiscal_start, nls) AS fiscal_day_of_month,
                  FISCAL_DAY_OF_WEEK(d, fiscal_start, 'POSITION', nls) AS fiscal_day_of_week,
                  RETAIL_YEAR_NUMBER(d, restated) AS retail_year_number,
                  RETAIL_QUARTER_OF_YEAR(d, restated) AS retail_quarter_of_year,
                  RETAIL_MONTH_OF_YEAR(d, restated, 'DATE') AS retail_month_of_year,
                  RETAIL_MONTH_OF_QUARTER(d, restated) AS retail_month_of_quarter,
                  RETAIL_WEEK_OF_YEAR(d, restated) AS retail_week_of_year,
                  RETAIL_WEEK_OF_QUARTER(d, restated) AS retail_week_of_quarter,
                  RETAIL_WEEK_OF_MONTH(d, restated) AS retail_week_of_month,
                  RETAIL_DAY_OF_YEAR(d, restated) AS retail_day_of_year,
                  RETAIL_DAY_OF_QUARTER(d, restated) AS retail_day_of_quarter,
                  RETAIL_DAY_OF_MONTH(d, restated) AS retail_day_of_month,
                  RETAIL_DAY_OF_WEEK(d, restated, 'POSITION') AS retail_day_of_week,
                  CALENDAR_ADD_YEARS(d, periods, nls) AS calendar_add_years,
                  CALENDAR_ADD_QUARTERS(d, periods, nls) AS calendar_add_quarters,
                  CALENDAR_ADD_MONTHS(d, periods, nls) AS calendar_add_months,
                  CALENDAR_ADD_WEEKS(d, periods, nls) AS calendar_add_weeks,
                  CALENDAR_ADD_DAYS(d, periods, nls) AS calendar_add_days,
                  FISCAL_ADD_YEARS(d, periods, fiscal_start, nls) AS fiscal_add_years,
                  FISCAL_ADD_QUARTERS(d, periods, fiscal_start, nls) AS fiscal_add_quarters,
                  FISCAL_ADD_MONTHS(d, periods, fiscal_start, nls) AS fiscal_add_months,
                  FISCAL_ADD_WEEKS(d, periods, fiscal_start, nls) AS fiscal_add_weeks,
                  FISCAL_ADD_DAYS(d, periods, fiscal_start, nls) AS fiscal_add_days,
                  RETAIL_ADD_YEARS(d, periods, restated) AS retail_add_years,
                  RETAIL_ADD_QUARTERS(d, periods, restated) AS retail_add_quarters,
                  RETAIL_ADD_MONTHS(d, periods, restated) AS retail_add_months,
                  RETAIL_ADD_WEEKS(d, periods, restated) AS retail_add_weeks,
                  RETAIL_ADD_DAYS(d, periods, restated) AS retail_add_days,
                  CALENDAR_SINCE(d, fmt, nls) AS calendar_since,
                  RETAIL_DAY_EXISTS(d, restated) AS retail_day_exists
                FROM calendar_dates;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle numeric single-row functions exactly") {
            val sql =
                """
                CREATE TABLE measurements (
                  id NUMBER PRIMARY KEY,
                  amount NUMBER,
                  ratio BINARY_DOUBLE,
                  bucket_count NUMBER
                );

                SELECT ABS(amount) AS absolute_amount,
                  BITAND(bucket_count, 7) AS masked_bucket,
                  CEIL(amount) AS ceiling_amount,
                  FLOOR(amount) AS floor_amount,
                  MOD(bucket_count, 3) AS modulo_bucket,
                  POWER(amount, 2) AS powered_amount,
                  REMAINDER(bucket_count, 3) AS remainder_bucket,
                  ROUND(amount, 2) AS rounded_amount,
                  SIGN(amount) AS amount_sign,
                  TRUNC(amount, 2) AS truncated_amount,
                  WIDTH_BUCKET(ratio, 0, 1, 10) AS ratio_bucket
                FROM measurements;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle character set and collation functions exactly") {
            val sql =
                """
                CREATE TABLE localized_names (
                  id NUMBER PRIMARY KEY,
                  name VARCHAR2(64) COLLATE BINARY_CI
                );

                SELECT NLS_CHARSET_DECL_LEN(10, NLS_CHARSET_ID('AL32UTF8')) AS declared_length,
                  NLS_CHARSET_ID('AL32UTF8') AS charset_id,
                  NLS_CHARSET_NAME(873) AS charset_name,
                  COLLATION(name) AS derived_collation,
                  NLS_COLLATION_ID('BINARY_CI') AS collation_id,
                  NLS_COLLATION_NAME(147455) AS collation_name
                FROM localized_names;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle large object functions exactly") {
            val sql =
                """
                CREATE TABLE media_assets (
                  id NUMBER PRIMARY KEY,
                  binary_file BFILE,
                  image_data BLOB,
                  text_data CLOB
                );

                SELECT BFILENAME('MEDIA_DIR', 'asset.bin') AS file_locator,
                  EMPTY_BLOB() AS empty_blob_value,
                  EMPTY_CLOB() AS empty_clob_value
                FROM media_assets;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle collection and hierarchical functions exactly") {
            val sql =
                """
                CREATE TABLE collection_samples (
                  id NUMBER PRIMARY KEY,
                  parent_id NUMBER,
                  tag VARCHAR2(100),
                  tags VARRAY(10)
                );

                SELECT CARDINALITY(tags) AS tag_count,
                  COLLECT(tag ORDER BY tag) AS collected_tags,
                  POWERMULTISET(tags) AS tag_power_set,
                  POWERMULTISET_BY_CARDINALITY(tags, 2) AS tag_pairs,
                  SET(tags) AS distinct_tags,
                  SYS_CONNECT_BY_PATH(tag, '/') AS tag_path
                FROM collection_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle object reference functions exactly") {
            val sql =
                """
                CREATE TABLE object_reference_samples (
                  id NUMBER PRIMARY KEY,
                  address_ref REF
                );

                SELECT REF(address_ref) AS sample_ref,
                  DEREF(address_ref) AS address_object,
                  MAKE_REF(id, id) AS made_ref
                FROM object_reference_samples sample;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle conversion functions exactly") {
            val sql =
                """
                CREATE TABLE conversion_samples (
                  id NUMBER PRIMARY KEY,
                  raw_value RAW(16),
                  text_value VARCHAR2(4000),
                  row_identifier ROWID,
                  created_at TIMESTAMP
                );

                SELECT ASCIISTR(text_value) AS ascii_text,
                  BIN_TO_NUM(1, 0, 1, 1) AS binary_number,
                  CHARTOROWID(ROWIDTOCHAR(row_identifier)) AS chartorowid_value,
                  COMPOSE(text_value) AS composed_text,
                  CONVERT(text_value, 'AL32UTF8', 'WE8ISO8859P1') AS converted_text,
                  DECOMPOSE(text_value) AS decomposed_text,
                  RAWTONHEX(raw_value) AS national_hex,
                  ROWIDTOCHAR(row_identifier) AS rowid_text,
                  ROWIDTONCHAR(row_identifier) AS rowid_national_text,
                  SCN_TO_TIMESTAMP(123456) AS timestamp_from_scn,
                  TIMESTAMP_TO_SCN(created_at) AS scn_from_timestamp,
                  TO_BINARY_DOUBLE(text_value) AS binary_double_value,
                  TO_BINARY_FLOAT(text_value) AS binary_float_value,
                  TO_BLOB(raw_value) AS blob_value,
                  TO_CLOB(text_value) AS clob_value,
                  TO_LOB(text_value) AS lob_text_value,
                  TO_LOB(raw_value) AS lob_binary_value,
                  TO_MULTI_BYTE(text_value) AS multibyte_text,
                  TO_NCHAR(text_value) AS national_text,
                  TO_NCLOB(text_value) AS national_clob_value,
                  TO_SINGLE_BYTE(text_value) AS singlebyte_text,
                  UNISTR('\3042') AS unicode_text,
                  VALIDATE_CONVERSION(text_value AS NUMBER) AS valid_number
                FROM conversion_samples;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle text and national literals exactly") {
            val sql =
                """
                CREATE TABLE messages (
                  id NUMBER PRIMARY KEY,
                  body VARCHAR2(4000),
                  national_body NVARCHAR2(4000)
                );

                SELECT 'Jackie''s raincoat',
                  N'nchar literal',
                  n'national lowercase literal'
                FROM messages
                WHERE body = 'plain literal';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle numeric literals exactly") {
            val sql =
                """
                CREATE TABLE numeric_values (
                  id NUMBER PRIMARY KEY,
                  amount NUMBER,
                  single_value BINARY_FLOAT,
                  double_value BINARY_DOUBLE
                );

                SELECT 25,
                  +6.34,
                  0.5,
                  -1,
                  25f,
                  +6.34F,
                  0.5d,
                  -1D,
                  BINARY_FLOAT_NAN,
                  BINARY_FLOAT_INFINITY,
                  BINARY_DOUBLE_NAN,
                  BINARY_DOUBLE_INFINITY
                FROM numeric_values
                WHERE amount < 125.0;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle analytic and aggregate function clauses exactly") {
            val sql =
                """
                CREATE TABLE sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  department_id NUMBER,
                  employee_id NUMBER,
                  amount NUMBER,
                  sold_at DATE
                );

                SELECT region,
                  department_id,
                  employee_id,
                  SUM(amount) OVER (
                    PARTITION BY region
                    ORDER BY sold_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                  ) AS running_amount,
                  ROW_NUMBER() OVER (
                    PARTITION BY department_id
                    ORDER BY amount DESC NULLS LAST
                  ) AS amount_rank,
                  RANK() OVER (
                    PARTITION BY department_id
                    ORDER BY amount DESC
                  ) AS amount_rank_with_gaps,
                  DENSE_RANK() OVER (
                    PARTITION BY department_id
                    ORDER BY amount DESC
                  ) AS dense_amount_rank,
                  NTILE(4) OVER (
                    PARTITION BY region
                    ORDER BY amount DESC
                  ) AS amount_quartile,
                  CUME_DIST() OVER (
                    PARTITION BY department_id
                    ORDER BY amount
                  ) AS cumulative_distribution,
                  PERCENT_RANK() OVER (
                    PARTITION BY department_id
                    ORDER BY amount
                  ) AS percent_rank,
                  RATIO_TO_REPORT(amount) OVER (
                    PARTITION BY region
                  ) AS regional_ratio,
                  LAG(amount, 1, 0) OVER (
                    PARTITION BY department_id
                    ORDER BY sold_at
                  ) AS previous_amount,
                  LEAD(sold_at, 1) OVER (
                    PARTITION BY department_id
                    ORDER BY sold_at
                  ) AS next_sold_at,
                  FIRST_VALUE(region) OVER (
                    PARTITION BY department_id
                    ORDER BY amount ASC ROWS UNBOUNDED PRECEDING
                  ) AS first_region,
                  LAST_VALUE(sold_at) OVER (
                    PARTITION BY department_id
                    ORDER BY amount DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                  ) AS last_sold_at,
                  NTH_VALUE(amount, 2) OVER (
                    PARTITION BY department_id
                    ORDER BY sold_at ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                  ) AS second_amount,
                  SUM(amount) OVER (
                    PARTITION BY region
                    ORDER BY sold_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING EXCLUDE CURRENT ROW
                  ) AS other_region_amount,
                  AVG(amount) OVER (
                    PARTITION BY department_id
                    ORDER BY amount
                    GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES
                  ) AS peer_adjusted_average,
                  MAX(employee_id) KEEP (
                    DENSE_RANK LAST ORDER BY amount NULLS LAST
                  ) AS top_employee_id
                FROM sales;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses representative Oracle aggregate functions exactly") {
            val sql =
                """
                CREATE TABLE sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  employee_id NUMBER,
                  amount NUMBER,
                  discount NUMBER
                );

                SELECT region,
                  COUNT(*) AS total_rows,
                  COUNT(employee_id) AS employee_count,
                  SUM(amount) AS total_amount,
                  AVG(discount) AS average_discount,
                  MIN(amount) AS minimum_amount,
                  MAX(amount) AS maximum_amount,
                  MEDIAN(amount) AS median_amount,
                  STDDEV(amount) AS amount_stddev,
                  STDDEV_POP(amount) AS population_stddev,
                  STDDEV_SAMP(amount) AS sample_stddev,
                  VARIANCE(amount) AS amount_variance,
                  VAR_POP(amount) AS population_variance,
                  VAR_SAMP(amount) AS sample_variance,
                  CORR(amount, discount) AS amount_discount_correlation,
                  COVAR_POP(amount, discount) AS population_covariance,
                  COVAR_SAMP(amount, discount) AS sample_covariance,
                  KURTOSIS_POP(amount) AS population_kurtosis,
                  KURTOSIS_SAMP(amount) AS sample_kurtosis,
                  SKEWNESS_POP(amount) AS population_skewness,
                  SKEWNESS_SAMP(amount) AS sample_skewness,
                  COUNT(*) FILTER (WHERE amount > 0) AS positive_count,
                  SUM(amount) FILTER (WHERE discount IS NOT NULL) AS discounted_total,
                  SUM(amount) FILTER (WHERE amount > 0) OVER (PARTITION BY region) AS regional_positive_total
                FROM sales
                GROUP BY region;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle aggregate argument modifiers exactly") {
            val sql =
                """
                CREATE TABLE sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  employee_id NUMBER,
                  amount NUMBER,
                  discount NUMBER,
                  enabled BOOLEAN
                );

                SELECT region,
                  COUNT(DISTINCT employee_id) AS distinct_employee_count,
                  COUNT(UNIQUE employee_id) AS unique_employee_count,
                  SUM(ALL amount) AS all_amount,
                  AVG(DISTINCT discount) AS distinct_average_discount,
                  MIN(UNIQUE amount) AS unique_minimum_amount,
                  MAX(ALL amount) AS all_maximum_amount,
                  BIT_XOR_AGG(DISTINCT employee_id) AS distinct_parity_bits,
                  BOOLEAN_OR_AGG(ALL enabled) AS all_any_enabled
                FROM sales
                GROUP BY region;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle LISTAGG ordered aggregate exactly") {
            val sql =
                """
                CREATE TABLE employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  last_name VARCHAR2(100),
                  hire_date DATE
                );

                SELECT department_id,
                  LISTAGG(last_name, '; ') WITHIN GROUP (ORDER BY hire_date, last_name) AS employee_names,
                  LISTAGG(DISTINCT last_name, ', ' ON OVERFLOW TRUNCATE '...' WITH COUNT) WITHIN GROUP (ORDER BY last_name) AS distinct_employee_names
                FROM employees
                GROUP BY department_id;

                SELECT department_id,
                  LISTAGG(last_name, ', ') WITHIN GROUP (ORDER BY hire_date) OVER (PARTITION BY department_id) AS analytic_employee_names
                FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle approximate aggregate functions exactly") {
            val sql =
                """
                CREATE TABLE sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  employee_id NUMBER,
                  amount NUMBER,
                  sold_at DATE
                );

                SELECT region,
                  APPROX_COUNT(*) AS approximate_rows,
                  APPROX_COUNT_DISTINCT(employee_id) AS approximate_employee_count,
                  APPROX_SUM(amount) AS approximate_amount,
                  APPROX_MEDIAN(amount) AS approximate_median_amount,
                  APPROX_PERCENTILE(0.75) WITHIN GROUP (ORDER BY amount) AS approximate_percentile,
                  APPROX_PERCENTILE(0.75 DETERMINISTIC, 'ERROR_RATE') WITHIN GROUP (ORDER BY amount) AS percentile_error_rate
                FROM sales
                GROUP BY region;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle ordered-set percentile aggregate functions exactly") {
            val sql =
                """
                CREATE TABLE sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  amount NUMBER
                );

                SELECT region,
                  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY amount DESC) AS median_continuous,
                  PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY amount DESC) AS median_discrete
                FROM sales
                GROUP BY region;

                SELECT region,
                  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY amount DESC) OVER (PARTITION BY region) AS analytic_continuous,
                  PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY amount DESC) OVER (PARTITION BY region) AS analytic_discrete
                FROM sales;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle bit and boolean aggregate functions exactly") {
            val sql =
                """
                CREATE TABLE feature_flags (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(20),
                  flag_bits NUMBER,
                  enabled BOOLEAN
                );

                SELECT region,
                  BIT_AND_AGG(flag_bits) AS common_bits,
                  BIT_OR_AGG(flag_bits) AS combined_bits,
                  BIT_XOR_AGG(flag_bits) AS parity_bits,
                  BOOLEAN_AND_AGG(enabled) AS all_enabled,
                  BOOLEAN_OR_AGG(enabled) AS any_enabled,
                  CHECKSUM(flag_bits) AS flag_checksum
                FROM feature_flags
                GROUP BY region;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle named window clauses exactly") {
            val sql =
                """
                CREATE TABLE employee_salaries (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  salary NUMBER,
                  hired_at DATE
                );

                SELECT department_id,
                  salary,
                  SUM(salary) OVER department_salary_window AS running_salary,
                  ROW_NUMBER() OVER recent_department_window AS recent_rank
                FROM employee_salaries
                WINDOW
                  department_window AS (PARTITION BY department_id),
                  department_salary_window AS (department_window ORDER BY 1 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),
                  recent_department_window AS (PARTITION BY department_id ORDER BY 1 DESC);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle Machine Learning SQL functions exactly") {
            val sql =
                """
                CREATE TABLE mining_data (
                  cust_id NUMBER PRIMARY KEY,
                  cust_gender VARCHAR2(1),
                  age NUMBER,
                  cust_marital_status VARCHAR2(20),
                  education VARCHAR2(40),
                  household_size NUMBER,
                  text_doc VARCHAR2(4000)
                );

                SELECT cust_id,
                  PREDICTION(dt_sh_clas_sample COST MODEL USING cust_marital_status, education, household_size) AS affinity_prediction,
                  PREDICTION(FOR age USING *) OVER () AS predicted_age,
                  PREDICTION_BOUNDS(glmr_sh_regr_sample, 0.98 USING *) AS prediction_bounds,
                  PREDICTION_COST(dt_sh_clas_sample, 1 USING *) AS prediction_cost,
                  PREDICTION_DETAILS(FOR age ABS USING *) OVER () AS prediction_details,
                  PREDICTION_PROBABILITY(OF ANOMALY, 0 USING *) OVER (PARTITION BY cust_marital_status) AS anomaly_probability,
                  PREDICTION_SET(dt_sh_clas_sample COST MODEL USING *) AS prediction_set,
                  CLUSTER_ID(km_sh_clus_sample USING *) AS cluster_id,
                  CLUSTER_DISTANCE(km_sh_clus_sample USING *) AS cluster_distance,
                  CLUSTER_DETAILS(em_sh_clus_sample, 14, 5 USING *) AS cluster_details,
                  CLUSTER_PROBABILITY(km_sh_clus_sample, 2 USING *) AS cluster_probability,
                  CLUSTER_SET(em_sh_clus_sample, NULL, 0.2 USING *) AS cluster_set,
                  CLUSTER_ID(INTO 4 USING *) OVER () AS analytic_cluster_id,
                  FEATURE_ID(nmf_sh_sample USING *) AS feature_id,
                  FEATURE_COMPARE(esa_wiki_mod USING 'There are several PGA tour golfers from South Africa' AS text AND USING text_doc text) AS feature_distance,
                  FEATURE_DETAILS(nmf_sh_sample, 3 USING *) AS feature_details,
                  FEATURE_SET(nmf_sh_sample, 10 USING *) AS feature_set,
                  FEATURE_VALUE(nmf_sh_sample, 3 USING *) AS feature_value,
                  ORA_DM_PARTITION_NAME(dt_sh_clas_sample USING *) AS partition_name
                FROM mining_data;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle inline column constraints through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE accounts (
                  id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
                  external_id VARCHAR2(64) CONSTRAINT accounts_external_id_unique UNIQUE ENABLE NOVALIDATE,
                  status VARCHAR2(16) DEFAULT 'ACTIVE' NOT NULL,
                  archived_at TIMESTAMP INVISIBLE,
                  parent_id NUMBER REFERENCES accounts(id) DEFERRABLE INITIALLY DEFERRED
                );

                selectAll:
                SELECT id, external_id, status, archived_at, parent_id
                FROM accounts;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle annotations clauses exactly") {
            val sql =
                """
                CREATE TABLE employee (
                  id NUMBER(5) ANNOTATIONS(Identity, Display 'Employee ID', "Group" 'Emp_Info'),
                  employee_name VARCHAR2(50) ANNOTATIONS(Display 'Employee Name', "Group" 'Emp_Info'),
                  salary NUMBER ANNOTATIONS(Display 'Employee Salary', UI_Hidden)
                ) ANNOTATIONS(Display 'Employee Table', ADD IF NOT EXISTS Searchable);

                CREATE INDEX employee_name_idx
                ON employee (employee_name)
                ANNOTATIONS(ADD Display 'Employee Name Index');

                ALTER TABLE employee
                ANNOTATIONS(DROP IF EXISTS Searchable, REPLACE Display 'Employee Directory');

                ALTER TABLE employee
                MODIFY employee_name ANNOTATIONS(
                  DROP "Group",
                  DROP IF EXISTS Missing_Annotation,
                  REPLACE Display 'Employee name'
                );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle boolean literals and test conditions exactly") {
            val sql =
                """
                CREATE TABLE feature_flags (
                  id NUMBER PRIMARY KEY,
                  enabled BOOLEAN,
                  validated BOOLEAN
                );

                SELECT id,
                  TRUE AS literal_true,
                  FALSE AS literal_false,
                  UNKNOWN AS literal_unknown
                FROM feature_flags
                WHERE enabled IS TRUE
                  AND validated IS NOT FALSE
                  OR validated IS UNKNOWN;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle null conditions exactly") {
            val sql =
                """
                CREATE TABLE feature_flags (
                  id NUMBER PRIMARY KEY,
                  enabled BOOLEAN,
                  reviewed BOOLEAN,
                  deleted_at TIMESTAMP,
                  note VARCHAR2(100)
                );

                SELECT id,
                  enabled,
                  reviewed
                FROM feature_flags
                WHERE deleted_at IS NULL
                  AND note IS NOT NULL;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle table constraints through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE account_events (
                  account_id NUMBER NOT NULL,
                  event_id NUMBER NOT NULL,
                  parent_event_id NUMBER,
                  event_type VARCHAR2(64) NOT NULL,
                  event_payload JSON,
                  CONSTRAINT account_events_pk PRIMARY KEY (account_id, event_id) USING INDEX TABLESPACE users ENABLE VALIDATE EXCEPTIONS INTO constraint_exceptions,
                  CONSTRAINT account_events_type_unique UNIQUE (account_id, event_type) DEFERRABLE INITIALLY IMMEDIATE,
                  CONSTRAINT account_events_payload_check CHECK (event_payload IS NOT NULL) PRECHECK NOVALIDATE,
                  CONSTRAINT account_events_type_check CHECK (event_type IS NOT NULL) NOPRECHECK ENABLE,
                  CONSTRAINT account_events_parent_fk FOREIGN KEY (account_id, parent_event_id) REFERENCES account_events(account_id, event_id) DEFERRABLE INITIALLY DEFERRED
                );

                selectAll:
                SELECT account_id, event_id, parent_event_id, event_type, event_payload
                FROM account_events;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create table statement variants through SQLDelight environment exactly") {
            val sql =
                """
                CREATE GLOBAL TEMPORARY TABLE IF NOT EXISTS staged_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  CONSTRAINT staged_accounts_pk PRIMARY KEY (account_id)
                ) ON COMMIT PRESERVE ROWS;

                CREATE PRIVATE TEMPORARY TABLE private_staged_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ON COMMIT DELETE ROWS;

                CREATE IMMUTABLE TABLE immutable_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) NO DROP UNTIL 16 DAYS IDLE NO DELETE UNTIL 30 DAYS AFTER INSERT LOCKED;

                CREATE BLOCKCHAIN TABLE blockchain_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) NO DROP UNTIL 31 DAYS IDLE NO DELETE LOCKED HASHING USING "SHA2_512" VERSION "v1";

                CREATE IMMUTABLE BLOCKCHAIN TABLE immutable_blockchain_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) NO DROP UNTIL 31 DAYS IDLE NO DELETE LOCKED;

                CREATE SHARDED TABLE sharded_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) PARENT accounts MEMOPTIMIZE FOR READ REFRESH INTERVAL 30 SECOND;

                CREATE DUPLICATED TABLE duplicated_accounts SHARING = METADATA (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) MEMOPTIMIZE FOR WRITE SYNCHRONOUS;

                CREATE JSON COLLECTION TABLE account_documents
                SHARING = DATA
                WITH ETAG
                TABLESPACE users
                READ WRITE;

                CREATE TABLE collated_definition_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ON COMMIT DROP DEFINITION DEFAULT COLLATION BINARY_CI;

                CREATE TABLE virtual_column_accounts (
                  account_id NUMBER NOT NULL,
                  first_name VARCHAR2(64),
                  last_name VARCHAR2(64),
                  account_display_name VARCHAR2(129) GENERATED ALWAYS AS (first_name || ' ' || last_name) VIRTUAL,
                  account_search_name VARCHAR2(129) AS (first_name || last_name) INVISIBLE
                );

                CREATE TABLE encrypted_accounts (
                  account_id NUMBER NOT NULL,
                  tax_identifier VARCHAR2(32) ENCRYPT USING 'AES256' IDENTIFIED BY column_key SALT,
                  card_number VARCHAR2(32) ENCRYPT NO SALT,
                  legacy_identifier VARCHAR2(32) DECRYPT
                );

                CREATE TABLE compressed_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) COMPRESS ADVANCED HIGH ROW ARCHIVAL INDEXING ON;

                CREATE TABLE deferred_segment_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) SEGMENT CREATION DEFERRED;

                CREATE TABLE tablespace_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) TABLESPACE users LOGGING PCTFREE 10 PCTUSED 40 INITRANS 2;

                CREATE TABLE tablespace_set_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) TABLESPACE SET user_sets NOLOGGING;

                CREATE TABLE stored_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) STORAGE (
                  INITIAL 8 M
                  NEXT 1 M
                  MINEXTENTS 1
                  MAXEXTENTS UNLIMITED
                  MAXSIZE 1 G
                  PCTINCREASE 0
                  FREELISTS 2
                  FREELIST GROUPS 1
                  OPTIMAL 128 K
                  BUFFER_POOL KEEP
                  FLASH_CACHE DEFAULT
                  CELL_FLASH_CACHE NONE
                  ENCRYPT
                );

                CREATE TABLE result_cache_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) RESULT_CACHE (MODE FORCE, STANDBY ENABLE);

                CREATE TABLE read_only_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) READ ONLY;

                CREATE TABLE movable_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ENABLE ROW MOVEMENT READ WRITE;

                CREATE TABLE replicated_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ENABLE LOGICAL REPLICATION ALL KEYS NO PARTIAL JSON;

                CREATE TABLE flashback_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) BLOCKCHAIN FLASHBACK ARCHIVE account_archive;

                CREATE TABLE heap_organized_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ORGANIZATION HEAP TABLESPACE users ROW STORE COMPRESS ADVANCED;

                CREATE TABLE index_organized_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  CONSTRAINT index_organized_accounts_pk PRIMARY KEY (account_id)
                ) ORGANIZATION INDEX TABLESPACE users PCTTHRESHOLD 50 MAPPING TABLE COMPRESS 1 INCLUDING external_id OVERFLOW TABLESPACE users;

                CREATE TABLE external_accounts (
                  account_id NUMBER,
                  external_id VARCHAR2(64)
                ) ORGANIZATION EXTERNAL (
                  TYPE ORACLE_LOADER
                  DEFAULT DIRECTORY data_dir
                  ACCESS PARAMETERS (RECORDS DELIMITED BY NEWLINE)
                  LOCATION ('accounts.csv')
                ) REJECT LIMIT UNLIMITED;

                CREATE TABLE enabled_constraint_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  CONSTRAINT enabled_constraint_accounts_pk PRIMARY KEY (account_id) DISABLE
                ) ENABLE VALIDATE PRIMARY KEY USING INDEX enabled_constraint_accounts_pk_idx CASCADE KEEP INDEX;

                CREATE TABLE disabled_unique_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  CONSTRAINT disabled_unique_accounts_unique UNIQUE (external_id) ENABLE
                ) DISABLE NOVALIDATE UNIQUE (external_id) DROP INDEX;

                CREATE TABLE ilm_compressed_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ILM ADD POLICY ROW STORE COMPRESS ADVANCED SEGMENT AFTER 30 DAYS OF NO MODIFICATION;

                CREATE TABLE ilm_tiered_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ILM ADD POLICY TIER TO archive_ts READ ONLY SEGMENT AFTER 90 DAYS OF NO ACCESS;

                CREATE TABLE ilm_disabled_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ILM DISABLE POLICY cold_accounts_policy;

                CREATE TABLE supplemental_group_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  status VARCHAR2(16),
                  SUPPLEMENTAL LOG GROUP supplemental_group_accounts_log (external_id, status NO LOG) ALWAYS
                );

                CREATE TABLE supplemental_key_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  parent_account_id NUMBER,
                  SUPPLEMENTAL LOG DATA (PRIMARY KEY, UNIQUE, FOREIGN KEY) COLUMNS
                );

                CREATE TABLE temporal_accounts (
                  account_id NUMBER NOT NULL,
                  valid_from TIMESTAMP,
                  valid_to TIMESTAMP,
                  PERIOD FOR valid_time (valid_from, valid_to)
                );

                CREATE TABLE range_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  created_at DATE,
                  external_id VARCHAR2(64)
                ) PARTITION BY RANGE (created_at)
                INTERVAL (NUMTOYMINTERVAL(1, 'MONTH')) STORE IN (users, archive_ts) (
                  PARTITION p_2025 VALUES LESS THAN (DATE '2026-01-01') TABLESPACE users,
                  PARTITION p_max VALUES LESS THAN (MAXVALUE) READ ONLY
                );

                CREATE TABLE list_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  region_code VARCHAR2(8),
                  external_id VARCHAR2(64)
                ) PARTITION BY LIST (region_code) AUTOMATIC STORE IN (users, archive_ts) (
                  PARTITION p_us VALUES ('US', 'CA') INDEXING ON,
                  PARTITION p_other VALUES (DEFAULT)
                );

                CREATE TABLE hash_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) PARTITION BY HASH (account_id) PARTITIONS 4 STORE IN (users, archive_ts);

                CREATE TABLE individual_hash_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) PARTITION BY HASH (account_id) (
                  PARTITION p_hash_1 TABLESPACE users,
                  PARTITION p_hash_2 READ WRITE ROW STORE COMPRESS BASIC
                );

                CREATE TABLE range_list_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  created_at DATE,
                  region_code VARCHAR2(8)
                ) PARTITION BY RANGE (created_at)
                SUBPARTITION BY LIST (region_code)
                SUBPARTITION TEMPLATE (
                  SUBPARTITION sp_us VALUES ('US', 'CA') TABLESPACE users,
                  SUBPARTITION sp_other VALUES (DEFAULT)
                ) (
                  PARTITION p_2025 VALUES LESS THAN (DATE '2026-01-01'),
                  PARTITION p_max VALUES LESS THAN (MAXVALUE)
                );

                CREATE TABLE range_range_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  created_at DATE,
                  account_id_bucket NUMBER
                ) PARTITION BY RANGE (created_at)
                SUBPARTITION BY RANGE (account_id_bucket)
                SUBPARTITION TEMPLATE (
                  SUBPARTITION sp_low VALUES LESS THAN (1000) TABLESPACE users,
                  SUBPARTITION sp_max VALUES LESS THAN (MAXVALUE)
                ) (
                  PARTITION p_2025 VALUES LESS THAN (DATE '2026-01-01'),
                  PARTITION p_max VALUES LESS THAN (MAXVALUE)
                );

                CREATE TABLE list_hash_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  region_code VARCHAR2(8)
                ) PARTITION BY LIST (region_code)
                SUBPARTITION BY HASH (account_id) SUBPARTITIONS 4 STORE IN (users, archive_ts) (
                  PARTITION p_us VALUES ('US', 'CA') (
                    SUBPARTITION sp_us_1 TABLESPACE users,
                    SUBPARTITION sp_us_2 TABLESPACE archive_ts
                  ),
                  PARTITION p_other VALUES (DEFAULT)
                );

                CREATE TABLE hash_list_partitioned_accounts (
                  account_id NUMBER NOT NULL,
                  region_code VARCHAR2(8)
                ) PARTITION BY HASH (account_id)
                SUBPARTITION BY LIST (region_code) (
                  PARTITION p_hash_1 (
                    SUBPARTITION sp_us VALUES ('US', 'CA'),
                    SUBPARTITION sp_other VALUES (DEFAULT)
                  ),
                  PARTITION p_hash_2 TABLESPACE archive_ts
                );

                CREATE TABLE referenced_parent_accounts (
                  account_id NUMBER PRIMARY KEY,
                  created_at DATE
                ) PARTITION BY RANGE (created_at) (
                  PARTITION p_2025 VALUES LESS THAN (DATE '2026-01-01'),
                  PARTITION p_max VALUES LESS THAN (MAXVALUE)
                );

                CREATE TABLE reference_partitioned_events (
                  event_id NUMBER PRIMARY KEY,
                  account_id NUMBER NOT NULL,
                  CONSTRAINT reference_partitioned_events_account_fk FOREIGN KEY (account_id) REFERENCES referenced_parent_accounts(account_id)
                ) PARTITION BY REFERENCE (reference_partitioned_events_account_fk);

                CREATE TABLE system_partitioned_events (
                  event_id NUMBER PRIMARY KEY,
                  payload JSON
                ) PARTITION BY SYSTEM (
                  PARTITION p_system_1 TABLESPACE users,
                  PARTITION p_system_2 TABLESPACE archive_ts
                );

                CREATE TABLE row_store_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) ROW STORE COMPRESS ADVANCED;

                CREATE TABLE column_store_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) COLUMN STORE COMPRESS FOR QUERY HIGH NO ROW LEVEL LOCKING;

                CREATE TABLE cache_parallel_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) CACHE PARALLEL 4 ROWDEPENDENCIES;

                CREATE TABLE nocache_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) NOCACHE;

                CREATE TABLE noparallel_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) NOPARALLEL NOROWDEPENDENCIES;

                CREATE TABLE inmemory_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64)
                ) INMEMORY MEMCOMPRESS FOR QUERY LOW;

                CREATE TABLE clustered_accounts (
                  account_id NUMBER NOT NULL,
                  external_id VARCHAR2(64),
                  created_at DATE
                ) CLUSTERING BY LINEAR ORDER (account_id, created_at) YES ON LOAD WITH MATERIALIZED ZONEMAP;

                CREATE TABLE account_snapshot AS
                SELECT account_id, external_id
                FROM staged_accounts;

                CREATE GLOBAL TEMPORARY TABLE staged_account_snapshot
                ON COMMIT PRESERVE ROWS
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE compressed_account_snapshot
                COLUMN STORE COMPRESS FOR ARCHIVE LOW
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE collated_account_snapshot
                ON COMMIT PRESERVE DEFINITION
                DEFAULT COLLATION BINARY_AI
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE deferred_account_snapshot
                SEGMENT CREATION IMMEDIATE
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE logged_account_snapshot
                TABLESPACE users NOLOGGING PCTFREE 5 INITRANS 1
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE stored_account_snapshot
                STORAGE (INITIAL 64K NEXT 1M MAXEXTENTS 10 BUFFER_POOL DEFAULT)
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE parallel_account_snapshot
                PARALLEL 8
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE serial_account_snapshot
                NOPARALLEL
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE clustered_account_snapshot
                CLUSTERING BY INTERLEAVED ORDER ((account_id, external_id), created_at) NO ON LOAD
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE cached_account_snapshot
                RESULT_CACHE (STANDBY DISABLE, MODE DEFAULT)
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE read_only_account_snapshot
                READ ONLY
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE movable_account_snapshot
                DISABLE ROW MOVEMENT READ WRITE
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE replicated_account_snapshot
                DISABLE LOGICAL REPLICATION
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE flashback_account_snapshot
                NO FLASHBACK ARCHIVE
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE heap_account_snapshot
                ORGANIZATION HEAP TABLESPACE users
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE external_account_snapshot
                ORGANIZATION EXTERNAL (
                  TYPE ORACLE_DATAPUMP
                  DEFAULT DIRECTORY data_dir
                  LOCATION ('account_snapshot.dmp')
                ) REJECT LIMIT 0
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE disabled_account_snapshot
                DISABLE CONSTRAINT disabled_account_snapshot_pk
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE ilm_account_snapshot
                ILM ADD POLICY NO INMEMORY SEGMENT ON heat_map_policy
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE ilm_delete_account_snapshot
                ILM DELETE_ALL
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE range_partitioned_account_snapshot
                PARTITION BY RANGE ((1)) (
                  PARTITION p_low VALUES LESS THAN (1000),
                  PARTITION p_max VALUES LESS THAN (MAXVALUE)
                )
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE hash_partitioned_account_snapshot
                PARTITION BY HASH ((1)) PARTITIONS 4 STORE IN (users, archive_ts)
                AS SELECT account_id, external_id
                FROM staged_accounts;

                CREATE TABLE consistent_hash_account_snapshot
                PARTITION BY CONSISTENT HASH ((1)) PARTITIONS AUTO TABLESPACE SET tenant_tablespace_set
                AS SELECT account_id, external_id
                FROM staged_accounts;

                selectAll:
                SELECT account_id, external_id
                FROM account_snapshot;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses representative Oracle object table statements exactly") {
            val sql =
                """
                CREATE TABLE account_objects
                OF account_object_type
                OBJECT IDENTIFIER IS PRIMARY KEY
                OIDINDEX account_objects_oid_idx (TABLESPACE users)
                TABLESPACE users;

                CREATE TABLE final_account_objects
                OF hr.account_object_type
                NOT SUBSTITUTABLE AT ALL LEVELS
                OBJECT IDENTIFIER IS SYSTEM GENERATED
                READ ONLY;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle REF column and table constraints exactly") {
            val sql =
                """
                CREATE TABLE employee_ref_constraints (
                  employee_id NUMBER PRIMARY KEY,
                  manager_ref REF SCOPE IS employee_objects,
                  mentor_ref REF WITH ROWID,
                  audit_ref REF,
                  SCOPE FOR (audit_ref) IS account_objects,
                  REF (mentor_ref) WITH ROWID
                );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses representative Oracle XMLType table statements exactly") {
            val sql =
                """
                CREATE TABLE account_xml_documents
                OF XMLTYPE
                XMLSCHEMA 'http://example.com/account.xsd' ELEMENT 'Account'
                STORE AS BINARY XML
                WITH OBJECT ID (account_id)
                OIDINDEX account_xml_documents_oid_idx (TABLESPACE users)
                TABLESPACE users;

                CREATE TABLE account_xml_clobs
                OF XMLTYPE
                STORE AS CLOB
                READ ONLY;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle relational table column property storage clauses exactly") {
            val sql =
                """
                CREATE TABLE account_document_storage (
                  account_id NUMBER PRIMARY KEY,
                  document_text CLOB,
                  document_blob BLOB,
                  document_xml XMLTYPE,
                  line_items NESTED TABLE,
                  attachments VARRAY(10),
                  contact OBJECT,
                  preferred_contact OBJECT
                )
                LOB (document_text, document_blob) STORE AS SECUREFILE (
                  TABLESPACE users
                  ENABLE STORAGE IN ROW
                  CHUNK 8192
                  CACHE READS
                  COMPRESS HIGH
                  DEDUPLICATE
                  ENCRYPT
                )
                XMLTYPE COLUMN document_xml XMLSCHEMA 'http://example.com/document.xsd' ELEMENT 'Document' STORE AS BINARY XML
                NESTED TABLE line_items STORE AS account_line_items RETURN AS LOCATOR
                VARRAY attachments STORE AS BASICFILE LOB account_attachments_lob
                COLUMN preferred_contact ELEMENT IS OF TYPE (ONLY account_contact_type)
                COLUMN contact NOT SUBSTITUTABLE AT ALL LEVELS;

                CREATE TABLE partitioned_document_storage (
                  account_id NUMBER PRIMARY KEY,
                  created_at DATE,
                  document_text CLOB,
                  document_xml XMLTYPE
                )
                PARTITION BY RANGE (created_at) (
                  PARTITION p_2025 VALUES LESS THAN (DATE '2026-01-01'),
                  PARTITION p_max VALUES LESS THAN (MAXVALUE)
                )
                LOB (document_text) STORE AS PARTITION p_2025 (
                  TABLESPACE users
                  COMPRESS MEDIUM
                  KEEP_DUPLICATES
                )
                LOB (document_text) STORE AS SUBPARTITION sp_2025 (
                  TABLESPACE archive_ts
                  NOCACHE
                )
                XMLTYPE COLUMN document_xml STORE AS CLOB (CACHE);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle JSON collection table expression columns exactly") {
            val sql =
                """
                CREATE JSON COLLECTION TABLE account_collection_documents
                WITH ETAG (
                  account_id NUMBER GENERATED ALWAYS AS (1) VIRTUAL INVISIBLE,
                  status VARCHAR2(16) AS ('ACTIVE') VIRTUAL,
                  CONSTRAINT account_collection_status_check CHECK (1 = 1)
                )
                TABLESPACE users;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle quoted table identifiers through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE "CaseSensitiveAccounts" (
                  account_id NUMBER NOT NULL,
                  select_value VARCHAR2(64),
                  CONSTRAINT case_sensitive_accounts_pk PRIMARY KEY (account_id)
                );

                CREATE INDEX case_sensitive_accounts_select_idx
                ON "CaseSensitiveAccounts" (select_value);

                selectAll:
                SELECT account_id, select_value
                FROM "CaseSensitiveAccounts";
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle row limiting clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE ranked_accounts (
                  id NUMBER PRIMARY KEY,
                  status VARCHAR2(32) NOT NULL,
                  score NUMBER,
                  embedding VECTOR(3, FLOAT32)
                );

                selectOffsetFetch:
                SELECT id, status
                FROM ranked_accounts
                ORDER BY score DESC
                OFFSET 10 ROWS FETCH FIRST 5 ROWS ONLY;

                selectApproximate:
                SELECT id
                FROM ranked_accounts
                ORDER BY VECTOR_DISTANCE(embedding, VECTOR('[1,2,3]'))
                FETCH APPROX FIRST 20 ROWS ONLY WITH TARGET 90 PERCENT PARAMETERS (efs = 80);

                selectPartitioned:
                SELECT id, status
                FROM ranked_accounts
                ORDER BY status, score DESC
                FETCH FIRST 3 PARTITIONS BY status, 2 ROWS ONLY;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle set operators through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE inventory_products (
                  product_id NUMBER PRIMARY KEY,
                  product_name VARCHAR2(128)
                );

                CREATE TABLE order_products (
                  product_id NUMBER PRIMARY KEY,
                  product_name VARCHAR2(128)
                );

                selectMinus:
                SELECT product_id FROM inventory_products
                MINUS
                SELECT product_id FROM order_products
                ORDER BY product_id;

                selectUnion:
                SELECT product_id FROM inventory_products
                UNION
                SELECT product_id FROM order_products;

                selectUnionAll:
                SELECT product_id FROM inventory_products
                UNION ALL
                SELECT product_id FROM order_products;

                selectIntersect:
                SELECT product_id FROM inventory_products
                INTERSECT
                SELECT product_id FROM order_products;

                selectMinusAll:
                SELECT product_id FROM inventory_products
                MINUS ALL
                SELECT product_id FROM order_products;

                selectExcept:
                SELECT product_id FROM inventory_products
                EXCEPT
                SELECT product_id FROM order_products;

                selectExceptAll:
                SELECT product_id FROM inventory_products
                EXCEPT ALL
                SELECT product_id FROM order_products;

                selectIntersectAll:
                SELECT product_id FROM inventory_products
                INTERSECT ALL
                SELECT product_id FROM order_products;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle subquery factoring clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE org_units (
                  id NUMBER PRIMARY KEY,
                  parent_id NUMBER,
                  unit_name VARCHAR2(128)
                );

                selectFactored:
                WITH root_units (id, parent_id) AS (
                  SELECT id, parent_id
                  FROM org_units
                  WHERE parent_id IS NULL
                )
                SELECT id
                FROM root_units
                ORDER BY id;

                selectMultipleFactored:
                WITH active_units AS (
                  SELECT id, parent_id
                  FROM org_units
                  WHERE unit_name IS NOT NULL
                ),
                leaf_units AS (
                  SELECT id, parent_id
                  FROM active_units
                  WHERE parent_id IS NOT NULL
                )
                SELECT id
                FROM leaf_units;

                selectNestedFactored:
                WITH outer_units AS (
                  WITH inner_units AS (
                    SELECT id, parent_id
                    FROM org_units
                  )
                  SELECT id, parent_id
                  FROM inner_units
                )
                SELECT id
                FROM outer_units;

                selectSearchCycleFactored:
                WITH unit_tree (id, parent_id) AS (
                  SELECT id, parent_id
                  FROM org_units
                  WHERE parent_id IS NULL
                )
                SEARCH DEPTH FIRST BY id SET traversal_order
                CYCLE id SET is_cycle TO 'Y' DEFAULT 'N'
                SELECT id, parent_id
                FROM unit_tree;

                selectValuesFactored:
                WITH staged_units (id, unit_name) AS (
                  VALUES (1, 'Operations'), (2, 'Finance')
                )
                SELECT id, unit_name
                FROM staged_units;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle order by clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE ordering_units (
                  id NUMBER PRIMARY KEY,
                  parent_id NUMBER,
                  unit_name VARCHAR2(128),
                  sort_score NUMBER
                );

                selectNullOrdering:
                SELECT id AS sort_id, unit_name, sort_score
                FROM ordering_units
                ORDER BY sort_score DESC NULLS LAST, sort_id ASC NULLS FIRST, 1 DESC;

                selectSiblingOrdering:
                SELECT id, parent_id, unit_name
                FROM ordering_units
                ORDER SIBLINGS BY unit_name ASC NULLS LAST, id DESC NULLS FIRST;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle for update clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE lockable_accounts (
                  id NUMBER PRIMARY KEY,
                  owner_name VARCHAR2(128),
                  balance NUMBER
                );

                selectForUpdate:
                SELECT id, owner_name, balance
                FROM lockable_accounts
                WHERE id = 1
                FOR UPDATE;

                selectForUpdateOfNowait:
                SELECT id, owner_name, balance
                FROM lockable_accounts
                WHERE owner_name IS NOT NULL
                FOR UPDATE OF owner_name, balance NOWAIT;

                selectForUpdateQualifiedWait:
                SELECT account_alias.id, account_alias.balance
                FROM lockable_accounts account_alias
                FOR UPDATE OF account_alias.balance WAIT 5 SECONDS;

                selectForUpdateSkipLocked:
                SELECT id, owner_name
                FROM lockable_accounts
                FOR UPDATE SKIP LOCKED;

                selectForUpdateWaitForever:
                SELECT id
                FROM lockable_accounts
                FOR UPDATE WAIT FOREVER;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle group by aggregation extensions through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE grouped_sales (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(64),
                  product_name VARCHAR2(128),
                  amount NUMBER
                );

                selectRollup:
                SELECT region, product_name, SUM(amount) AS total_amount
                FROM grouped_sales
                GROUP BY ROLLUP (region, product_name)
                HAVING SUM(amount) > 0;

                selectCube:
                SELECT region, product_name, SUM(amount) AS total_amount
                FROM grouped_sales
                GROUP BY CUBE (region, product_name);

                selectGroupingSets:
                SELECT region, product_name, SUM(amount) AS total_amount
                FROM grouped_sales
                GROUP BY GROUPING SETS ((region, product_name), (region), ());

                selectCompositeGrouping:
                SELECT region, product_name, SUM(amount) AS total_amount
                FROM grouped_sales
                GROUP BY (region, product_name);

                selectGroupAll:
                SELECT region, SUM(amount) AS total_amount
                FROM grouped_sales
                GROUP BY ALL;

                selectLeadingHaving:
                SELECT region, SUM(amount) AS total_amount
                FROM grouped_sales
                HAVING SUM(amount) > 0
                GROUP BY region;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle select unique quantifier through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE unique_regions (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(64)
                );

                selectUniqueRegions:
                SELECT UNIQUE region
                FROM unique_regions;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle join operators through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE join_departments (
                  id NUMBER PRIMARY KEY,
                  department_name VARCHAR2(128)
                );

                CREATE TABLE join_employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  employee_name VARCHAR2(128)
                );

                selectRightOuterJoin:
                SELECT employee_name, department_name
                FROM join_employees e RIGHT OUTER JOIN join_departments d
                  ON e.department_id = d.id;

                selectFullOuterJoin:
                SELECT employee_name, department_name
                FROM join_employees e FULL OUTER JOIN join_departments d
                  ON e.department_id = d.id;

                selectPartitionedOuterJoin:
                SELECT employee_name, department_name
                FROM join_employees e PARTITION BY (1) RIGHT OUTER JOIN join_departments d
                  ON e.department_id = d.id;

                selectCrossApply:
                SELECT department_name, employee_name
                FROM join_departments d CROSS APPLY (
                  SELECT employee_name
                  FROM join_employees e
                  WHERE e.department_id IS NOT NULL
                ) employee_matches;

                selectOuterApply:
                SELECT department_name, employee_name
                FROM join_departments d OUTER APPLY (
                  SELECT employee_name
                  FROM join_employees e
                  WHERE e.department_id IS NOT NULL
                ) employee_matches;

                selectLegacyOuterJoin:
                SELECT employee_name, department_name
                FROM join_employees e, join_departments d
                WHERE e.department_id = d.id(+);
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle query table expressions through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE partitioned_orders (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(64),
                  created_year NUMBER
                );

                CREATE TABLE external_query_orders (
                  id NUMBER,
                  region VARCHAR2(64)
                );

                CREATE TABLE reporting_orders_view (
                  id NUMBER,
                  region VARCHAR2(64)
                );

                CREATE TABLE reporting_orders_mv (
                  id NUMBER,
                  region VARCHAR2(64)
                );

                CREATE TABLE sales_av (
                  id NUMBER,
                  region VARCHAR2(64)
                );

                CREATE TABLE sales_hier (
                  id NUMBER,
                  region VARCHAR2(64)
                );

                selectOnlyTable:
                SELECT id, region
                FROM ONLY (partitioned_orders) po;

                selectRemoteTable:
                SELECT id, region
                FROM partitioned_orders@orders_link po;

                selectRemoteView:
                SELECT id, region
                FROM reporting_orders_view@orders_link rov;

                selectRemoteMaterializedView:
                SELECT id, region
                FROM reporting_orders_mv@orders_link romv;

                selectRemoteOnlyTable:
                SELECT id, region
                FROM ONLY (partitioned_orders@orders_link) po;

                selectOnlySubquery:
                SELECT id, region
                FROM ONLY ((
                  SELECT id, region
                  FROM partitioned_orders
                )) only_orders;

                selectPartition:
                SELECT id, region
                FROM partitioned_orders PARTITION (p_2026) po;

                selectSubpartitionFor:
                SELECT id, region
                FROM partitioned_orders SUBPARTITION FOR (2026, 'WEST') po;

                selectAnalyticViewHierarchies:
                SELECT id, region
                FROM sales_av HIERARCHIES (time_hier, product_hier) av;

                selectHierarchyReference:
                SELECT id, region
                FROM sales_hier h;

                selectSampleBlock:
                SELECT id, region
                FROM partitioned_orders SAMPLE BLOCK (10) SEED (42) po;

                selectAsOfScn:
                SELECT id, region
                FROM partitioned_orders AS OF SCN 123456 po;

                selectAsOfTimestamp:
                SELECT id, region
                FROM partitioned_orders AS OF TIMESTAMP TIMESTAMP '2026-01-01 00:00:00' po;

                selectVersionsBetweenScn:
                SELECT *
                FROM partitioned_orders VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE po;

                selectVersionsBetweenTimestamp:
                SELECT *
                FROM partitioned_orders VERSIONS BETWEEN TIMESTAMP MINVALUE AND MAXVALUE po;

                selectVersionsBetweenAsOfScn:
                SELECT *
                FROM partitioned_orders VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE AS OF SCN 123456 po;

                selectAsOfPeriod:
                SELECT id, region
                FROM partitioned_orders AS OF PERIOD FOR valid_time TIMESTAMP '2026-01-01 00:00:00' po;

                selectVersionsPeriod:
                SELECT *
                FROM partitioned_orders VERSIONS PERIOD FOR valid_time BETWEEN MINVALUE AND MAXVALUE po;

                selectPivot:
                SELECT *
                FROM partitioned_orders PIVOT (
                  COUNT(*) AS order_count
                  FOR region IN ('WEST' AS west, 'EAST' AS east)
                ) pivoted_orders;

                selectPivotXmlAny:
                SELECT *
                FROM partitioned_orders PIVOT XML (
                  COUNT(*)
                  FOR region IN (ANY)
                ) pivoted_orders;

                selectPivotXmlSubquery:
                SELECT *
                FROM partitioned_orders PIVOT XML (
                  COUNT(*)
                  FOR region IN (
                    SELECT region
                    FROM partitioned_orders
                  )
                ) pivoted_orders;

                selectPivotCompositeColumns:
                SELECT *
                FROM partitioned_orders PIVOT (
                  COUNT(*)
                  FOR (region, created_year) IN (('WEST', 2026) AS west_2026)
                ) pivoted_orders;

                selectUnpivot:
                SELECT *
                FROM partitioned_orders UNPIVOT INCLUDE NULLS (
                  metric_value
                  FOR metric_name IN (id AS 'ID', created_year AS 'CREATED_YEAR')
                ) unpivoted_orders;

                selectUnpivotCompositeColumns:
                SELECT *
                FROM partitioned_orders UNPIVOT EXCLUDE NULLS (
                  (metric_id, metric_year)
                  FOR metric_name IN ((id, created_year) AS ('ID', 'CREATED_YEAR'))
                ) unpivoted_orders;

                selectMatchRecognize:
                SELECT *
                FROM partitioned_orders MATCH_RECOGNIZE (
                  PARTITION BY 1
                  ORDER BY 1
                  MEASURES 1 AS first_id, 2 AS last_id
                  ONE ROW PER MATCH
                  AFTER MATCH SKIP TO LAST rising
                  PATTERN (start_row rising+)
                  DEFINE rising AS 1 > 0
                ) matched_orders;

                selectMatchRecognizeAdvancedPattern:
                SELECT *
                FROM partitioned_orders MATCH_RECOGNIZE (
                  MEASURES RUNNING 1 AS running_id, FINAL 2 AS final_id
                  ALL ROWS PER MATCH WITH UNMATCHED ROWS
                  AFTER MATCH SKIP PAST LAST ROW
                  PATTERN (start_row (rising falling*)+)
                  SUBSET trend = (rising, falling)
                  DEFINE rising AS 1 > 0, falling AS 0 < 1
                ) advanced_matches;

                selectMatchRecognizeReluctantPattern:
                SELECT *
                FROM partitioned_orders MATCH_RECOGNIZE (
                  ALL ROWS PER MATCH OMIT EMPTY MATCHES
                  PATTERN (start_row?? rising+? falling*?)
                  DEFINE start_row AS 1 = 1, rising AS 1 > 0, falling AS 0 < 1
                ) reluctant_matches;

                selectQualify:
                SELECT id, region, ROW_NUMBER() OVER (PARTITION BY region ORDER BY id) AS row_number
                FROM partitioned_orders
                QUALIFY id > 0;

                selectHierarchicalStartFirst:
                SELECT id, region
                FROM partitioned_orders
                START WITH id = 1
                CONNECT BY PRIOR id = created_year;

                selectHierarchicalConnectFirst:
                SELECT id, region
                FROM partitioned_orders
                CONNECT BY NOCYCLE PRIOR id = created_year
                START WITH id = 1;

                selectTableCollection:
                SELECT 1
                FROM TABLE(ODCINUMBERLIST(1, 2)) numbers;

                selectLegacyTheCollection:
                SELECT 1
                FROM THE (
                  SELECT ODCINUMBERLIST(1, 2)
                  FROM partitioned_orders
                ) numbers;

                selectOuterJoinedTableCollection:
                SELECT 1
                FROM partitioned_orders po,
                     TABLE(ODCINUMBERLIST(1))(+) numbers;

                selectValuesTable:
                SELECT *
                FROM (
                  VALUES (1, 'SCOTT'),
                         (2, 'SMITH')
                ) value_employees (employee_id, first_name);

                selectInlineExternalTable:
                SELECT *
                FROM EXTERNAL ((
                  employee_id NUMBER,
                  first_name VARCHAR2(100) NOT NULL
                ) TYPE ORACLE_LOADER
                  DEFAULT DIRECTORY data_dir
                ) external_employees;

                selectInlineExternalTableColumnDefinitions:
                SELECT *
                FROM EXTERNAL ((
                  order_id NUMBER(12) NOT NULL,
                  order_total NUMBER(12, 2),
                  ordered_at TIMESTAMP(6),
                  source_name VARCHAR2(100) NOT NULL
                ) TYPE ORACLE_LOADER
                  DEFAULT DIRECTORY data_dir
                  ACCESS PARAMETERS (RECORDS DELIMITED BY NEWLINE)
                  LOCATION ('orders.csv')
                  REJECT LIMIT UNLIMITED
                ) external_orders;

                selectGraphTable:
                SELECT *
                FROM GRAPH_TABLE (
                  employees_graph
                  MATCH (employee IS employee_node WHERE employee.status = 'ACTIVE')
                  COLUMNS (employee.employee_id AS employee_id, employee.name AS employee_name)
                ) graph_employees;

                selectGraphTableAsOfTimestamp:
                SELECT *
                FROM GRAPH_TABLE (
                  employees_graph AS OF TIMESTAMP CURRENT_TIMESTAMP
                  MATCH (employee IS employee_node)
                  COLUMNS (employee.*)
                ) graph_employee_versions;

                selectGraphqlTableFunction:
                SELECT *
                FROM GRAPHQL('
                  employees {
                    _id: employee_id
                    Name: first_name
                  }
                ') employee_documents;

                selectGraphqlTableFunctionWithPassing:
                SELECT *
                FROM GRAPHQL('
                  employees(employee_id: ${'$'}employee_id) {
                    _id: employee_id
                    Name: first_name
                  }
                ' PASSING 174 AS employee_id) employee_documents;

                selectModifiedExternalTable:
                SELECT *
                FROM external_query_orders EXTERNAL MODIFY (
                  DEFAULT DIRECTORY data_dir
                  LOCATION ('runtime_orders.csv')
                  REJECT LIMIT UNLIMITED
                ) query_orders;

                selectContainersTable:
                SELECT 1
                FROM CONTAINERS(partitioned_orders) container_orders;

                selectShardsTable:
                SELECT 1
                FROM SHARDS(partitioned_orders) shard_orders;

                selectLateralSubquery:
                SELECT region, derived_year
                FROM partitioned_orders po, LATERAL (
                  SELECT created_year AS derived_year
                  FROM partitioned_orders
                ) years;

                selectSubqueryWithCheckOption:
                SELECT id, region
                FROM (
                  SELECT id, region
                  FROM partitioned_orders
                  WITH CHECK OPTION
                ) checked_orders;

                selectSubqueryWithReadOnly:
                SELECT id, region
                FROM (
                  SELECT id, region
                  FROM partitioned_orders
                  WITH READ ONLY
                ) read_only_orders;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle select list aliases and wildcard expansion through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE select_list_orders (
                  id NUMBER PRIMARY KEY,
                  region VARCHAR2(64),
                  created_year NUMBER
                );

                selectListAliases:
                SELECT id AS order_id,
                       region order_region,
                       select_list_orders.*
                FROM select_list_orders;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle alter table statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE alter_targets (
                  id NUMBER PRIMARY KEY,
                  status VARCHAR2(16),
                  valid_from DATE,
                  valid_to DATE
                );

                ALTER TABLE alter_targets ADD created_at TIMESTAMP;
                ALTER TABLE alter_targets ADD (
                  updated_at TIMESTAMP,
                  archived_at TIMESTAMP INVISIBLE
                );
                ALTER TABLE alter_targets ADD CONSTRAINT alter_targets_status_check CHECK (status IS NOT NULL) ENABLE;
                ALTER TABLE alter_targets ADD PERIOD FOR valid_time (valid_from, valid_to);
                ALTER TABLE alter_targets MODIFY (
                  created_at TIMESTAMP NOT NULL,
                  updated_at TIMESTAMP NOT NULL
                );
                ALTER TABLE alter_targets MODIFY status VARCHAR2(32) NOT NULL;
                ALTER TABLE alter_targets MODIFY NESTED TABLE line_items RETURN AS LOCATOR;
                ALTER TABLE alter_targets MODIFY NESTED TABLE line_items RETURN AS VALUE;
                ALTER TABLE alter_targets RENAME COLUMN status TO account_status;
                ALTER TABLE alter_targets DROP COLUMN archived_at CASCADE CONSTRAINTS;
                ALTER TABLE alter_targets DROP COLUMN created_at CASCADE CONSTRAINTS;
                ALTER TABLE alter_targets SET UNUSED COLUMN updated_at ONLINE;
                ALTER TABLE alter_targets SET UNUSED (account_status) CASCADE CONSTRAINTS;
                ALTER TABLE alter_targets DROP PERIOD FOR valid_time;
                ALTER TABLE alter_targets DROP CONSTRAINT alter_targets_status_check;
                ALTER TABLE alter_targets ENABLE ROW MOVEMENT;
                ALTER TABLE alter_targets READ ONLY;
                ALTER TABLE alter_targets READ WRITE;
                ALTER TABLE alter_targets INDEXING OFF;
                ALTER TABLE alter_targets INDEXING ON;
                ALTER TABLE alter_targets MOVE TABLESPACE users COMPRESS ADVANCED;
                ALTER TABLE alter_targets SHRINK SPACE CASCADE;
                ALTER TABLE alter_targets ALLOCATE EXTENT (SIZE 128M DATAFILE '/u01/oradata/users01.dbf' INSTANCE 1);
                ALTER TABLE alter_targets DEALLOCATE UNUSED KEEP 64M;
                ALTER TABLE alter_targets INMEMORY MEMCOMPRESS FOR QUERY LOW;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle advanced alter table clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE alter_advanced_targets (
                  id NUMBER PRIMARY KEY,
                  created_at DATE,
                  payload CLOB,
                  payload_xml XMLTYPE
                );

                CREATE TABLE alter_exchange_stage (
                  id NUMBER PRIMARY KEY,
                  created_at DATE,
                  payload CLOB,
                  payload_xml XMLTYPE
                );

                ALTER TABLE alter_advanced_targets
                MODIFY XMLTYPE COLUMN payload_xml XMLSCHEMA 'http://example.com/payload.xsd' ALLOW NONSCHEMA STORE AS BINARY XML;

                ALTER TABLE alter_advanced_targets
                MODIFY LOB (payload) (
                  CACHE
                  STORAGE (INITIAL 1M NEXT 1M)
                );

                ALTER TABLE alter_advanced_targets
                MODIFY LOB (payload) (RETENTION COMPRESS HIGH ENCRYPT);

                ALTER TABLE alter_advanced_targets
                MODIFY VARRAY attachments STORE AS SECUREFILE LOB attachments_lob (
                  CACHE
                  TABLESPACE users
                );

                ALTER TABLE alter_advanced_targets UPGRADE INCLUDING DATA;
                ALTER TABLE alter_advanced_targets UPGRADE NOT INCLUDING DATA;

                ALTER TABLE alter_advanced_targets MEMOPTIMIZE FOR READ;
                ALTER TABLE alter_advanced_targets NO MEMOPTIMIZE FOR READ;
                ALTER TABLE alter_advanced_targets MEMOPTIMIZE FOR WRITE;
                ALTER TABLE alter_advanced_targets NO MEMOPTIMIZE FOR WRITE;

                ALTER TABLE alter_advanced_targets MINIMIZE RECORDS_PER_BLOCK;
                ALTER TABLE alter_advanced_targets NOMINIMIZE RECORDS_PER_BLOCK;

                ALTER TABLE alter_advanced_targets ENABLE VALIDATE PRIMARY KEY USING INDEX TABLESPACE users;
                ALTER TABLE alter_advanced_targets DISABLE NOVALIDATE CONSTRAINT alter_targets_status_check CASCADE KEEP INDEX;
                ALTER TABLE alter_advanced_targets ENABLE UNIQUE (id) DROP INDEX;

                ALTER TABLE alter_advanced_targets RESULT_CACHE (MODE FORCE, STANDBY ENABLE);
                ALTER TABLE alter_advanced_targets FLASHBACK ARCHIVE account_archive;
                ALTER TABLE alter_advanced_targets NO FLASHBACK ARCHIVE;
                ALTER TABLE alter_advanced_targets ENABLE LOGICAL REPLICATION ALL KEYS;
                ALTER TABLE alter_advanced_targets DISABLE LOGICAL REPLICATION;

                ALTER TABLE alter_advanced_targets
                ILM ADD POLICY ROW STORE COMPRESS ADVANCED SEGMENT AFTER 30 DAYS OF NO MODIFICATION;

                ALTER TABLE alter_advanced_targets ILM DISABLE POLICY cold_targets_policy;
                ALTER TABLE alter_advanced_targets ILM DELETE_ALL;

                ALTER TABLE alter_advanced_targets
                MODIFY CLUSTERING BY LINEAR ORDER (id, created_at) YES ON LOAD WITH MATERIALIZED ZONEMAP;

                ALTER TABLE alter_advanced_targets
                ADD PARTITION p_2026 VALUES LESS THAN (DATE '2027-01-01') TABLESPACE users;

                ALTER TABLE alter_advanced_targets
                TRUNCATE PARTITION p_2025 UPDATE GLOBAL INDEXES;

                ALTER TABLE alter_advanced_targets
                MOVE PARTITION p_2025 TABLESPACE users UPDATE INDEXES;

                ALTER TABLE alter_advanced_targets
                SPLIT PARTITION p_max AT (DATE '2028-01-01')
                INTO (PARTITION p_2027, PARTITION p_future) UPDATE GLOBAL INDEXES;

                ALTER TABLE alter_advanced_targets
                MERGE PARTITIONS p_2025, p_2026 INTO PARTITION p_2025_2026 UPDATE INDEXES;

                ALTER TABLE alter_advanced_targets
                COALESCE PARTITION UPDATE GLOBAL INDEXES;

                ALTER TABLE alter_advanced_targets
                MODIFY PARTITION p_2025 READ ONLY;

                ALTER TABLE alter_advanced_targets
                RENAME PARTITION p_2025 TO p_2025_archived;

                ALTER TABLE alter_advanced_targets
                DROP SUBPARTITION sp_2024 UPDATE GLOBAL INDEXES;

                ALTER TABLE alter_advanced_targets
                SET SUBPARTITION TEMPLATE (
                  SUBPARTITION sp_h1 VALUES LESS THAN (DATE '2026-07-01') TABLESPACE users,
                  SUBPARTITION sp_future VALUES LESS THAN (MAXVALUE)
                );

                ALTER TABLE alter_advanced_targets
                SET SUBPARTITION TEMPLATE ();

                ALTER TABLE alter_advanced_targets
                EXCHANGE PARTITION p_2025 WITH TABLE alter_exchange_stage INCLUDING INDEXES WITHOUT VALIDATION;

                ALTER TABLE alter_advanced_targets
                DEFAULT DIRECTORY data_dir ACCESS PARAMETERS (RECORDS DELIMITED BY NEWLINE) LOCATION ('orders.csv') REJECT LIMIT UNLIMITED;

                ALTER TABLE alter_advanced_targets
                NO DROP UNTIL 31 DAYS IDLE;

                ALTER TABLE alter_advanced_targets
                ADD HASHING USING sha2_512 VERSION v1;

                ALTER TABLE alter_advanced_targets
                MODIFY IMMUTABLE
                NO DROP UNTIL 16 DAYS IDLE
                NO DELETE UNTIL 30 DAYS AFTER INSERT LOCKED
                USE created_at FOR ROW CREATION TIME;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create index statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE indexed_accounts (
                  id NUMBER PRIMARY KEY,
                  status VARCHAR2(32) NOT NULL,
                  created_at TIMESTAMP,
                  archived_at TIMESTAMP,
                  embedding VECTOR(3, FLOAT32),
                  payload XMLTYPE,
                  search_doc JSON
                );

                CREATE UNIQUE INDEX IF NOT EXISTS indexed_accounts_status_idx
                ON indexed_accounts (status) VISIBLE ONLINE;

                CREATE BITMAP INDEX indexed_accounts_archive_idx
                ON indexed_accounts (archived_at, created_at) INVISIBLE NOLOGGING;

                CREATE INDEX indexed_accounts_created_idx
                ON indexed_accounts (created_at DESC) REVERSE COMPUTE STATISTICS;

                CREATE INDEX indexed_accounts_status_upper_idx
                ON indexed_accounts (UPPER(status));

                CREATE INDEX indexed_accounts_text_domain_idx
                ON indexed_accounts (search_doc)
                INDEXTYPE IS CTXSYS.CONTEXT
                PARAMETERS ('SYNC (ON COMMIT)')
                PARALLEL 2;

                CREATE INDEX indexed_accounts_xml_idx
                ON indexed_accounts (payload)
                INDEXTYPE IS XDB.XMLINDEX
                PARAMETERS ('PATH TABLE indexed_accounts_path_tab');

                CREATE JSON MULTIVALUE INDEX indexed_accounts_json_score_idx
                ON indexed_accounts (search_doc)
                TABLESPACE DEFAULT
                SORT
                USABLE
                IMMEDIATE INVALIDATION;

                CREATE INDEX indexed_accounts_created_global_idx
                ON indexed_accounts (created_at)
                GLOBAL PARTITION BY RANGE (created_at)
                (PARTITION p_max VALUES LESS THAN (MAXVALUE))
                TABLESPACE users
                STORAGE (INITIAL 64 K NEXT 64 K)
                PCTFREE 10
                INITRANS 2
                COMPRESS ADVANCED LOW;

                CREATE BITMAP INDEX indexed_accounts_bitmap_join_idx
                ON indexed_accounts (status)
                FROM indexed_accounts
                WHERE status IS NOT NULL
                LOCAL;

                CREATE INDEX indexed_accounts_cluster_idx
                ON CLUSTER indexed_accounts
                TABLESPACE users
                STORAGE (INITIAL 64 K)
                NOSORT;

                CREATE INDEX indexed_accounts_ilm_idx
                ILM ADD POLICY TIER TO users
                ON indexed_accounts (status);

                CREATE VECTOR INDEX indexed_accounts_embedding_hnsw_idx
                ON indexed_accounts (embedding)
                ORGANIZATION INMEMORY NEIGHBOR GRAPH
                DISTANCE COSINE
                WITH TARGET ACCURACY 90
                PARAMETERS (type HNSW, neighbors 40, efconstruction 500)
                DUPLICATE ALL ONLINE;

                CREATE VECTOR INDEX indexed_accounts_embedding_ivf_idx
                ON indexed_accounts (embedding)
                INCLUDE (status)
                ORGANIZATION NEIGHBOR PARTITIONS
                DISTANCE COSINE
                WITH TARGET ACCURACY 95
                PARAMETERS (type IVF, neighbor partitions 10)
                LOCAL;

                CREATE HYBRID VECTOR INDEX indexed_accounts_search_hybrid_idx
                ON indexed_accounts (search_doc)
                PARAMETERS ('JSON($.text) VECTOR($.embedding)');

                ALTER INDEX indexed_accounts_status_idx REBUILD ONLINE TABLESPACE users;

                ALTER INDEX indexed_accounts_created_global_idx REBUILD PARTITION p_max TABLESPACE users;

                selectAll:
                SELECT id, status, created_at, archived_at, embedding, search_doc
                FROM indexed_accounts;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create view statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE view_accounts (
                  id NUMBER PRIMARY KEY,
                  status VARCHAR2(32) NOT NULL,
                  created_at TIMESTAMP,
                  payload XMLTYPE
                );

                CREATE OR REPLACE FORCE EDITIONING VIEW account_status_view AS
                SELECT id AS account_id, status AS account_status, created_at AS account_created_at
                FROM view_accounts
                WHERE status IS NOT NULL
                BEQUEATH CURRENT_USER
                WITH CHECK OPTION;

                CREATE NO FORCE VIEW readonly_account_view AS
                SELECT id, status
                FROM view_accounts
                WITH READ ONLY;

                CREATE VIEW IF NOT EXISTS constrained_account_view
                  SHARING = EXTENDED DATA
                  (
                    account_id VISIBLE UNIQUE RELY DISABLE NOVALIDATE,
                    account_status INVISIBLE,
                    CONSTRAINT account_view_pk PRIMARY KEY (account_id) RELY DISABLE NOVALIDATE
                  )
                  DEFAULT COLLATION BINARY
                  BEQUEATH DEFINER
                AS
                SELECT id AS account_id, status AS account_status
                FROM view_accounts
                WITH CHECK OPTION CONSTRAINT account_view_check
                CONTAINER_MAP;

                CREATE OR REPLACE VIEW account_object_view
                  OF account_object_type
                  WITH OBJECT IDENTIFIER (id)
                AS
                SELECT id, status, created_at
                FROM view_accounts;

                CREATE VIEW account_xml_view
                  OF XMLTYPE
                  XMLSCHEMA 'http://example.com/account.xsd'
                  ELEMENT 'Account'
                  WITH OBJECT ID (id)
                AS
                SELECT payload
                FROM view_accounts;

                CREATE JSON COLLECTION VIEW account_json_collection_view
                  SHARING = METADATA
                AS
                SELECT id AS DATA
                FROM view_accounts
                CONTAINERS_DEFAULT;

                selectAll:
                SELECT account_id, account_status, account_created_at
                FROM account_status_view;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create sequence statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE SEQUENCE IF NOT EXISTS account_id_seq
                  SHARING = METADATA
                  START WITH 1
                  INCREMENT BY 1
                  MINVALUE 1
                  NOMAXVALUE
                  CACHE 20
                  NOCYCLE
                  ORDER
                  KEEP
                  SCALE EXTEND
                  NOSHARD
                  GLOBAL;

                CREATE SEQUENCE session_event_seq
                  START WITH 100
                  INCREMENT BY 10
                  MAXVALUE 1000000
                  NOMINVALUE
                  NOCACHE
                  CYCLE
                  NOORDER
                  NOKEEP
                  NOSCALE
                  SHARD NOEXTEND
                  SESSION;

                CREATE SEQUENCE hr.employee_seq
                  SHARING = NONE
                  START WITH 500
                  NOCACHE;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle drop sequence statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP SEQUENCE IF EXISTS account_id_seq;

                DROP SEQUENCE hr.employee_seq;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle alter sequence statements through SQLDelight environment exactly") {
            val sql =
                """
                ALTER SEQUENCE IF EXISTS account_id_seq
                  RESTART
                  START WITH 42
                  INCREMENT BY 2
                  MAXVALUE 10000
                  MINVALUE 1
                  CACHE 10
                  NOCYCLE
                  NOORDER
                  NOKEEP
                  SCALE NOEXTEND
                  SHARD EXTEND
                  GLOBAL;

                ALTER SEQUENCE hr.employee_seq
                  NOMAXVALUE
                  NOMINVALUE
                  NOCACHE
                  CYCLE
                  ORDER
                  KEEP
                  NOSCALE
                  NOSHARD
                  SESSION;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle merge statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE account_balance (
                  account_id NUMBER PRIMARY KEY,
                  balance NUMBER,
                  updated_at TIMESTAMP
                );

                CREATE TABLE account_delta (
                  account_id NUMBER PRIMARY KEY,
                  delta NUMBER
                );

                CREATE TABLE merge_error_log (
                  message VARCHAR2(100)
                );

                CREATE TABLE audit_target (
                  id NUMBER PRIMARY KEY,
                  payload VARCHAR2(100)
                );

                CREATE TABLE audit_source (
                  id NUMBER PRIMARY KEY,
                  payload VARCHAR2(100)
                );

                MERGE INTO account_balance target
                USING (
                  SELECT account_id, delta
                  FROM account_delta
                ) source
                ON (1 = 1)
                WHEN MATCHED THEN UPDATE SET
                  balance = 100,
                  updated_at = CURRENT_TIMESTAMP
                  WHERE 1 <> 0
                  DELETE WHERE 0 = 1
                WHEN NOT MATCHED THEN INSERT (account_id, balance, updated_at)
                  VALUES (1, DEFAULT, CURRENT_TIMESTAMP)
                  WHERE 1 <> 0
                LOG ERRORS INTO merge_error_log ('account-balance') REJECT LIMIT UNLIMITED;

                MERGE INTO audit_target
                USING audit_source source
                ON (1 = 0)
                WHEN NOT MATCHED THEN INSERT
                  SET id = 1,
                      payload = DEFAULT;

                MERGE INTO account_balance target
                USING account_delta source
                ON (1 = 0)
                WHEN MATCHED THEN UPDATE SET
                  balance = 1,
                  updated_at = CURRENT_TIMESTAMP
                WHEN NOT MATCHED THEN INSERT (account_id, balance, updated_at)
                  VALUES (1, 1, CURRENT_TIMESTAMP)
                WAIT 10 SECONDS
                RETURNING 1 INTO ?;

                MERGE INTO account_balance target
                USING account_delta source
                ON (1 = 0)
                WHEN MATCHED THEN UPDATE SET
                  balance = 2
                RETURN NEW 1 INTO ?;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle insert default values through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE default_value_orders (
                  id NUMBER DEFAULT 1 PRIMARY KEY,
                  status VARCHAR2(16) DEFAULT 'NEW'
                );

                insertDefaultValues:
                INSERT INTO default_value_orders DEFAULT VALUES;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle insert error logging clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE import_orders (
                  order_id NUMBER PRIMARY KEY,
                  customer_name VARCHAR2(128) NOT NULL,
                  order_total NUMBER(10, 2)
                );

                insertValuesWithErrorLogging:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                VALUES (?, ?, ?)
                LOG ERRORS INTO import_order_errors ('values-load') REJECT LIMIT 25;

                insertValuesReturning:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                VALUES (?, ?, ?)
                RETURNING order_id, order_total INTO ?, ?;

                insertValuesReturnAlias:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                VALUES (?, ?, ?)
                RETURN NEW order_id INTO ?;

                insertValuesReturningWait:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                VALUES (?, ?, ?)
                RETURNING order_id INTO ?
                WAIT FOREVER;

                insertSelectWithErrorLogging:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                SELECT order_id, customer_name, order_total
                FROM import_orders
                WHERE order_total IS NOT NULL
                LOG ERRORS REJECT LIMIT UNLIMITED;

                insertSelectReturning:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                SELECT order_id, customer_name, order_total
                FROM import_orders
                WHERE order_total IS NOT NULL
                RETURNING order_id INTO ?;

                insertSelectByName:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                SELECT order_id, customer_name, order_total
                FROM import_orders
                BY NAME;

                insertSelectByPosition:
                INSERT INTO import_orders (order_id, customer_name, order_total)
                SELECT order_id, customer_name, order_total
                FROM import_orders
                BY POSITION;

                insertDefaultWithErrorLogging:
                INSERT INTO import_orders DEFAULT VALUES
                LOG ERRORS INTO import_order_errors REJECT LIMIT 1;

                insertDefaultReturning:
                INSERT INTO import_orders DEFAULT VALUES
                RETURNING order_id INTO ?;

                insertRemoteTarget:
                INSERT INTO import_orders@orders_link (order_id, customer_name, order_total)
                VALUES (?, ?, ?);

                insertSetClause:
                INSERT INTO import_orders
                SET order_id = ?, customer_name = ?, order_total = DEFAULT
                RETURNING order_id INTO ?;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle multi-table insert statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE staged_order_lines (
                  order_id NUMBER PRIMARY KEY,
                  region_code VARCHAR2(16),
                  order_total NUMBER(10, 2)
                );

                CREATE TABLE archived_order_lines (
                  order_id NUMBER PRIMARY KEY,
                  region_code VARCHAR2(16),
                  order_total NUMBER(10, 2)
                );

                CREATE TABLE regional_order_lines (
                  order_id NUMBER PRIMARY KEY,
                  region_code VARCHAR2(16)
                );

                archiveAndRoute:
                INSERT ALL
                  INTO archived_order_lines (order_id, order_total)
                    VALUES (1, 100)
                  WHEN 1 = 1 THEN
                    INTO regional_order_lines (order_id, region_code)
                      VALUES (1, 'US')
                  ELSE
                    INTO regional_order_lines (order_id, region_code)
                      VALUES (1, DEFAULT)
                SELECT order_id, region_code, order_total
                FROM staged_order_lines;

                firstMatchingRoute:
                INSERT FIRST
                  WHEN 1001 > 1000 THEN
                    INTO archived_order_lines (order_id, order_total)
                      VALUES (2, 1001)
                  WHEN 'EU' IS NOT NULL THEN
                    INTO regional_order_lines (order_id, region_code)
                      VALUES (2, 'EU')
                SELECT order_id, region_code, order_total
                FROM staged_order_lines;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle insert partition extension clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE partitioned_orders (
                  order_id NUMBER PRIMARY KEY,
                  region_code VARCHAR2(16) NOT NULL,
                  order_total NUMBER(10, 2)
                );

                insertPartitionByName:
                INSERT INTO partitioned_orders PARTITION (orders_2026_q1) (order_id, region_code, order_total)
                VALUES (?, ?, ?);

                insertPartitionForKey:
                INSERT INTO partitioned_orders PARTITION FOR (2026, 1) (order_id, region_code, order_total)
                VALUES (?, ?, ?);

                insertSubpartitionByName:
                INSERT INTO partitioned_orders SUBPARTITION (orders_2026_q1_us) (order_id, region_code, order_total)
                VALUES (?, ?, ?);

                insertSubpartitionForKey:
                INSERT INTO partitioned_orders SUBPARTITION FOR ('US') (order_id, region_code, order_total)
                VALUES (?, ?, ?)
                LOG ERRORS INTO partitioned_order_errors REJECT LIMIT 10;

                insertOnlyTarget:
                INSERT INTO ONLY (partitioned_orders) (order_id, region_code, order_total)
                VALUES (?, ?, ?);
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle update and delete partition extension clauses through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE partitioned_order_updates (
                  order_id NUMBER PRIMARY KEY,
                  region_code VARCHAR2(16) NOT NULL,
                  order_total NUMBER(10, 2),
                  archived_at TIMESTAMP
                );

                CREATE TABLE partitioned_order_adjustments (
                  order_id NUMBER PRIMARY KEY,
                  adjustment_total NUMBER(10, 2)
                );

                CREATE TABLE partitioned_order_update_errors (
                  order_id NUMBER,
                  error_message VARCHAR2(4000)
                );

                CREATE TABLE partitioned_order_delete_errors (
                  order_id NUMBER,
                  error_message VARCHAR2(4000)
                );

                updatePartitionByName:
                UPDATE partitioned_order_updates PARTITION (orders_2026_q1)
                SET order_total = order_total + 1
                WHERE region_code = ?;

                updateSubpartitionForKey:
                UPDATE partitioned_order_updates SUBPARTITION FOR ('US')
                SET archived_at = CURRENT_TIMESTAMP
                WHERE order_id = ?;

                updateOnlyTarget:
                UPDATE ONLY (partitioned_order_updates)
                SET order_total = order_total + 1
                WHERE region_code = ?;

                updateSingleColumnDefault:
                UPDATE partitioned_order_updates
                SET archived_at = DEFAULT
                WHERE order_id = ?;

                updateMultipleColumnDefault:
                UPDATE partitioned_order_updates
                SET order_total = DEFAULT, archived_at = DEFAULT
                WHERE region_code = ?;

                updateReturning:
                UPDATE partitioned_order_updates
                SET order_total = order_total + ?
                WHERE order_id = ?
                RETURNING NEW order_total, OLD archived_at INTO ?, ?;

                updateReturnAlias:
                UPDATE partitioned_order_updates
                SET order_total = order_total + ?
                WHERE order_id = ?
                RETURN NEW order_total INTO ?;

                updateWithWaitAndErrorLogging:
                UPDATE partitioned_order_updates
                SET archived_at = CURRENT_TIMESTAMP
                WHERE order_id = ?
                WAIT 5 SECONDS
                LOG ERRORS INTO partitioned_order_update_errors ('update-load') REJECT LIMIT UNLIMITED;

                updateFromUsing:
                UPDATE partitioned_order_updates target
                SET order_total = order_total + 1
                FROM partitioned_order_adjustments source
                WHERE order_id = ?;

                updateTupleFromSubquery:
                UPDATE partitioned_order_updates target
                SET (order_total, archived_at) = (
                  SELECT adjustment_total, CURRENT_TIMESTAMP
                  FROM partitioned_order_adjustments source
                  WHERE order_id = ?
                )
                WHERE order_id = ?;

                updateRemoteTarget:
                UPDATE partitioned_order_updates@orders_link
                SET archived_at = CURRENT_TIMESTAMP
                WHERE order_id = ?;

                updateWhereCurrentOf:
                UPDATE partitioned_order_updates
                SET archived_at = CURRENT_TIMESTAMP
                WHERE CURRENT OF order_cursor;

                deletePartitionForKey:
                DELETE FROM partitioned_order_updates PARTITION FOR (2026, 1)
                WHERE archived_at IS NOT NULL;

                deleteOnlyTarget:
                DELETE FROM ONLY (partitioned_order_updates)
                WHERE order_total = 0;

                deleteSubpartitionByName:
                DELETE FROM partitioned_order_updates SUBPARTITION (orders_2026_q1_us)
                WHERE region_code = ?;

                deleteCorrelatedSubquery:
                DELETE FROM partitioned_order_updates
                WHERE EXISTS (
                  SELECT 1
                  FROM partitioned_order_adjustments source
                  WHERE source.order_id = partitioned_order_updates.order_id
                );

                deleteReturning:
                DELETE FROM partitioned_order_updates
                WHERE order_id = ?
                RETURNING OLD order_total INTO ?;

                deleteReturnAlias:
                DELETE FROM partitioned_order_updates
                WHERE order_id = ?
                RETURN OLD order_total INTO ?;

                deleteWithErrorLogging:
                DELETE FROM partitioned_order_updates
                WHERE archived_at IS NOT NULL
                LOG ERRORS INTO partitioned_order_delete_errors ('delete-load') REJECT LIMIT 10;

                deleteRemoteTarget:
                DELETE FROM partitioned_order_updates@orders_link
                WHERE order_id = ?;

                deleteWhereCurrentOf:
                DELETE FROM partitioned_order_updates
                WHERE CURRENT OF order_cursor;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle call statements through SQLDelight environment exactly") {
            val sql =
                """
                CALL remove_dept(162);

                CALL emp_mgmt.remove_dept(162);

                CALL hr.employee_api.adjust_salary(100, 2500, CURRENT_TIMESTAMP);

                CALL hr.employee_api.adjust_salary(employee_id => 100, delta => 2500, effective_at => CURRENT_TIMESTAMP);

                CALL hr.employee_api.adjust_salary(100, delta => 2500, effective_at => CURRENT_TIMESTAMP);

                CALL hr.remote_api.refresh_cache@reporting_link(tenant_id => 42);

                CALL hr.employee_api.current_salary(employee_id => 100) INTO :salary_out;

                CALL hr.remote_api.current_status@reporting_link(tenant_id => 42) INTO :status_out;

                CALL warehouse_typ(456, 'Warehouse 456', 2236).ret_name() INTO :warehouse_name;

                CALL ret_warehouse_typ(warehouse_typ(234, 'Warehouse 234', 2235)).ret_name() INTO :warehouse_name;

                CALL refresh_cache();
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle lock table statements through SQLDelight environment exactly") {
            val sql =
                """
                LOCK TABLE employees IN EXCLUSIVE MODE NOWAIT;

                LOCK TABLE hr.employees, hr.departments IN SHARE MODE;

                LOCK TABLE sales PARTITION (sales_q1) IN ROW SHARE MODE WAIT 30;

                LOCK TABLE sales PARTITION FOR (2026, 1), sales SUBPARTITION (sales_q1_north)
                IN SHARE ROW EXCLUSIVE MODE;

                LOCK TABLE hr.remote_sales@reporting_link IN ROW EXCLUSIVE MODE WAIT 5;

                LOCK TABLE sales SUBPARTITION FOR ('NORTH') IN SHARE UPDATE MODE;
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors = emptyList(),
                )
        }

        test("parses Oracle explain plan statements through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE explain_employees (
                  id NUMBER PRIMARY KEY,
                  department_id NUMBER,
                  salary NUMBER
                );

                CREATE TABLE explain_departments (
                  id NUMBER PRIMARY KEY
                );

                EXPLAIN PLAN
                  SET STATEMENT_ID = 'employee lookup'
                  INTO plan_table@reporting_link
                  FOR SELECT id, salary
                      FROM explain_employees
                      WHERE department_id = 10;

                EXPLAIN PLAN FOR
                  UPDATE explain_employees
                  SET salary = salary + 100
                  WHERE department_id = 10;

                EXPLAIN PLAN FOR
                  DELETE FROM explain_employees
                  WHERE department_id = 20;

                EXPLAIN PLAN FOR
                  INSERT INTO explain_employees (id, department_id, salary)
                  VALUES (100, 10, 1200);

                EXPLAIN PLAN FOR
                  INSERT INTO explain_employees DEFAULT VALUES;

                EXPLAIN PLAN FOR
                  MERGE INTO explain_employees target
                  USING explain_departments source
                  ON (1 = 1)
                  WHEN MATCHED THEN UPDATE SET salary = 100;

                EXPLAIN PLAN FOR
                  CREATE TABLE explain_snapshot AS
                  SELECT id, salary
                  FROM explain_employees;

                EXPLAIN PLAN FOR
                  CREATE INDEX explain_employees_department_idx
                  ON explain_employees (department_id);

                EXPLAIN PLAN FOR
                  ALTER INDEX explain_employees_department_idx REBUILD ONLINE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle truncate table statements through SQLDelight environment exactly") {
            val sql =
                """
                TRUNCATE TABLE employees_demo;

                TRUNCATE TABLE hr.sales_demo PRESERVE MATERIALIZED VIEW LOG DROP STORAGE;

                TRUNCATE TABLE orders_demo PURGE SNAPSHOT LOG DROP ALL STORAGE CASCADE;

                TRUNCATE TABLE private_temp_orders REUSE STORAGE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle truncate cluster statements through SQLDelight environment exactly") {
            val sql =
                """
                TRUNCATE CLUSTER personnel;

                TRUNCATE CLUSTER hr.personnel DROP STORAGE;

                TRUNCATE CLUSTER archived_personnel REUSE STORAGE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle materialized drop statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP MATERIALIZED VIEW LOG ON customers;

                DROP MATERIALIZED VIEW LOG IF EXISTS ON oe.customers;

                DROP SNAPSHOT LOG ON legacy_customers;

                DROP MATERIALIZED ZONEMAP sales_zmap;

                DROP MATERIALIZED ZONEMAP IF EXISTS reporting.sales_zmap;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create materialized zonemap on table statement exactly") {
            val sql =
                """
                CREATE MATERIALIZED ZONEMAP sales_zmap
                  ON sales(cust_id, prod_id);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create materialized zonemap attributes statement exactly") {
            val sql =
                """
                CREATE MATERIALIZED ZONEMAP IF NOT EXISTS reporting.sales_zmap
                  TABLESPACE users
                  SCALE 10
                  PCTFREE 20
                  PCTUSED 50
                  NOCACHE
                  REFRESH FAST ON LOAD DATA MOVEMENT
                  DISABLE PRUNING
                  ON sales(cust_id);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create materialized zonemap subquery statement exactly") {
            val sql =
                """
                CREATE TABLE sales(
                  cust_id NUMBER,
                  prod_id NUMBER
                );

                CREATE MATERIALIZED ZONEMAP sales_zmap
                  AS SELECT SYS_OP_ZONE_ID(rowid),
                            MIN(cust_id), MAX(cust_id),
                            MIN(prod_id), MAX(prod_id)
                     FROM sales
                     GROUP BY SYS_OP_ZONE_ID(rowid);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle drop synonym statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP SYNONYM customers_synonym;

                DROP SYNONYM IF EXISTS oe.customers_synonym;

                DROP PUBLIC SYNONYM customers;

                DROP PUBLIC SYNONYM IF EXISTS customer_type_synonym FORCE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle materialized log and zonemap alter statements exactly") {
            val sql =
                """
                ALTER MATERIALIZED VIEW LOG ON order_items ADD ROWID;

                ALTER MATERIALIZED VIEW LOG IF EXISTS ON hr.employees
                  ADD (commission_pct)
                  EXCLUDING NEW VALUES;

                ALTER SNAPSHOT LOG ON legacy_customers INCLUDING NEW VALUES;

                ALTER MATERIALIZED VIEW LOG ON sales
                  PURGE IMMEDIATE ASYNCHRONOUS;

                ALTER MATERIALIZED VIEW LOG ON sales
                  FOR SYNCHRONOUS REFRESH;

                ALTER MATERIALIZED VIEW LOG ON sales
                  PCTFREE 20 PCTUSED 50 NOCACHE PARALLEL 4;

                ALTER MATERIALIZED ZONEMAP sales_zmap
                  PCTFREE 20 PCTUSED 50 NOCACHE;

                ALTER MATERIALIZED ZONEMAP IF EXISTS reporting.sales_zmap
                  REFRESH FAST ON DEMAND ENABLE PRUNING;

                ALTER MATERIALIZED ZONEMAP sales_zmap COMPILE;

                ALTER MATERIALIZED ZONEMAP sales_zmap REBUILD;

                ALTER MATERIALIZED ZONEMAP sales_zmap UNUSABLE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle drop trigger statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP TRIGGER salary_check;

                DROP TRIGGER IF EXISTS hr.salary_check;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle PL/SQL create boundary statements exactly") {
            val sql =
                """
                CREATE OR REPLACE EDITIONABLE FUNCTION hr.second_max
                  RETURN NUMBER
                  AS LANGUAGE JAVA
                  NAME 'SecondMax.evaluate(int) return int';

                CREATE NONEDITIONABLE FUNCTION IF NOT EXISTS reporting.calculate_bonus
                  RETURN NUMBER
                  AS LANGUAGE JAVASCRIPT
                  NAME 'bonus.calculate';

                CREATE OR REPLACE PACKAGE BODY emp_mgmt
                  AS END emp_mgmt;

                CREATE TYPE BODY IF NOT EXISTS data_typ1
                  AS END data_typ1;

                CREATE OR REPLACE LIBRARY ext_lib
                  AS '/u01/app/oracle/lib/ext_lib.so';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle alter PL/SQL and Java statements exactly") {
            val sql =
                """
                ALTER FUNCTION IF EXISTS hr.second_max COMPILE DEBUG REUSE SETTINGS;

                ALTER FUNCTION reporting.calculate_bonus NONEDITIONABLE;

                ALTER PROCEDURE hr.remove_emp COMPILE;

                ALTER PACKAGE emp_mgmt COMPILE BODY REUSE SETTINGS;

                ALTER LIBRARY ext_lib COMPILE;

                ALTER JAVA SOURCE IF EXISTS AgentSource COMPILE AUTHID CURRENT_USER;

                ALTER JAVA CLASS Agent RESOLVE AUTHID DEFINER;

                ALTER SYNONYM offices COMPILE;

                ALTER PUBLIC SYNONYM emp_table COMPILE;

                ALTER TRIGGER IF EXISTS hr.salary_check DISABLE;

                ALTER TRIGGER salary_check RENAME TO salary_check_new;

                ALTER TYPE IF EXISTS data_typ1 COMPILE BODY REUSE SETTINGS;

                ALTER TYPE person_t FINAL;

                ALTER TYPE address_t ADD ATTRIBUTE postal_code VARCHAR2;

                ALTER TYPE address_t CASCADE INCLUDING TABLE DATA;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle drop type statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP TYPE person_t;

                DROP TYPE IF EXISTS hr.person_t FORCE;

                DROP TYPE employee_subtype VALIDATE;

                DROP TYPE BODY data_typ1;

                DROP TYPE BODY IF EXISTS hr.data_typ1;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle PL/SQL and Java drop statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP FUNCTION oe.SecondMax;

                DROP FUNCTION IF EXISTS reporting.calculate_bonus;

                DROP PROCEDURE hr.remove_emp;

                DROP PROCEDURE IF EXISTS archive.purge_history;

                DROP PACKAGE emp_mgmt;

                DROP PACKAGE BODY IF EXISTS hr.emp_mgmt;

                DROP LIBRARY ext_lib;

                DROP LIBRARY IF EXISTS hr.ext_lib;

                DROP JAVA SOURCE IF EXISTS AgentSource;

                DROP JAVA CLASS Agent;

                DROP JAVA RESOURCE IF EXISTS hr.config_properties;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle analytic drop statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP ANALYTIC VIEW sales_av;

                DROP ANALYTIC VIEW IF EXISTS sh.sales_av;

                DROP ATTRIBUTE DIMENSION product_attr_dim;

                DROP ATTRIBUTE DIMENSION IF EXISTS sh.product_attr_dim;

                DROP HIERARCHY product_hier;

                DROP HIERARCHY IF EXISTS sh.product_hier;

                DROP DIMENSION customers_dim;

                DROP DIMENSION sh.customers_dim;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create attribute dimension statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE ATTRIBUTE DIMENSION product_attr_dim
                USING product_dim
                ATTRIBUTES
                 (department_id,
                  department_name,
                  category_id,
                  category_name)
                LEVEL department
                  KEY department_id
                  ALTERNATE KEY department_name
                  MEMBER NAME department_name
                  MEMBER CAPTION department_name
                  ORDER BY department_name
                LEVEL category
                  KEY category_id
                  ALTERNATE KEY category_name
                  MEMBER NAME category_name
                  MEMBER CAPTION category_name
                  ORDER BY category_name
                  DETERMINES(department_id)
                ALL MEMBER NAME 'ALL PRODUCTS';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create hierarchy statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE HIERARCHY product_hier
                USING product_attr_dim
                 (category CHILD OF department);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create analytic view statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE FORCE ANALYTIC VIEW IF NOT EXISTS sales_av
                USING sales_fact
                DIMENSION BY
                  (product_attr_dim
                    KEY category_id REFERENCES category_id
                    HIERARCHIES (
                      product_hier DEFAULT))
                MEASURES
                 (sales FACT sales,
                  units FACT units)
                DEFAULT MEASURE sales;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create dimension statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE DIMENSION customers_dim
                  LEVEL customer IS customers.cust_id
                  LEVEL city IS customers.cust_city
                  HIERARCHY geography (customer CHILD OF city)
                  ATTRIBUTE customer DETERMINES (cust_first_name, cust_last_name);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle analytic dimension alter statements through SQLDelight environment exactly") {
            val sql =
                """
                ALTER ANALYTIC VIEW sales_av RENAME TO mysales_av;

                ALTER ANALYTIC VIEW IF EXISTS sh.sales_av COMPILE;

                ALTER ANALYTIC VIEW sales_av
                  ADD CACHE MEASURE GROUP (sales, units, cost)
                  LEVELS (time.fiscal.fiscal_quarter, warehouse);

                ALTER ANALYTIC VIEW sales_av
                  DROP CACHE MEASURE GROUP (sales)
                  LEVELS (time.fiscal.fiscal_quarter);

                ALTER ATTRIBUTE DIMENSION product_attr_dim RENAME TO my_product_attr_dim;

                ALTER ATTRIBUTE DIMENSION IF EXISTS sh.product_attr_dim COMPILE;

                ALTER HIERARCHY product_hier RENAME TO myproduct_hier;

                ALTER HIERARCHY IF EXISTS sh.product_hier COMPILE;

                ALTER DIMENSION customers_dim DROP ATTRIBUTE country;

                ALTER DIMENSION sh.customers_dim
                  ADD LEVEL zone IS customers.cust_postal_code
                  ADD ATTRIBUTE zone DETERMINES (cust_city);

                ALTER DIMENSION customers_dim COMPILE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle schema object drop statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP CLUSTER language;

                DROP CLUSTER IF EXISTS hr.personnel INCLUDING TABLES CASCADE CONSTRAINTS;

                DROP PUBLIC DATABASE LINK remote;

                DROP DATABASE LINK IF EXISTS ralph.linktosales;

                DROP DIRECTORY IF EXISTS bfile_dir;

                DROP INDEXTYPE IF EXISTS hr.position_indextype FORCE;

                DROP OPERATOR eq_op;

                DROP OPERATOR IF EXISTS hr.eq_op FORCE;

                DROP OUTLINE salaries;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle schema object alter statements through SQLDelight environment exactly") {
            val sql =
                """
                ALTER CLUSTER personnel SIZE 1024 CACHE;

                ALTER CLUSTER IF EXISTS hr.language DEALLOCATE UNUSED KEEP 30 K;

                ALTER DATABASE LINK private_link
                  CONNECT TO hr IDENTIFIED BY hr_new_password;

                ALTER PUBLIC DATABASE LINK public_link
                  CONNECT TO scott IDENTIFIED BY scott_new_password;

                ALTER SHARED PUBLIC DATABASE LINK shared_pub_link
                  CONNECT TO scott IDENTIFIED BY scott_new_password
                  AUTHENTICATED BY hr IDENTIFIED BY hr_new_password;

                ALTER DATABASE LINK IF EXISTS private_credential_link
                  CONNECT WITH hr_credential;

                ALTER INDEXTYPE position_indextype COMPILE;

                ALTER INDEXTYPE IF EXISTS hr.position_indextype
                  ADD OPERATOR eq_op(NUMBER, NUMBER);

                ALTER OPERATOR eq_op COMPILE;

                ALTER OPERATOR IF EXISTS hr.eq_op
                  DROP BINDING (NUMBER, NUMBER) FORCE;

                ALTER OUTLINE salaries REBUILD;

                ALTER PRIVATE OUTLINE session_outline RENAME TO session_outline_v2;

                ALTER PUBLIC OUTLINE salaries CHANGE CATEGORY TO reporting;

                ALTER OUTLINE salaries DISABLE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create operator statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OPERATOR eq_op
                  BINDING (VARCHAR2, VARCHAR2)
                  RETURN NUMBER
                  USING eq_f;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create operator if not exists statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OPERATOR IF NOT EXISTS hr.contains_op
                  SHARING = METADATA
                  BINDING (VARCHAR2, VARCHAR2)
                  RETURN NUMBER
                  WITH INDEX CONTEXT
                  COMPUTE ANCILLARY DATA
                  USING hr.contains_impl;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create outline from statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE PRIVATE OUTLINE my_salaries
                  FROM PUBLIC salaries;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create outline on statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OUTLINE salaries
                  FOR CATEGORY special
                  ON SELECT last_name, salary FROM employees;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create property graph vertex statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE PROPERTY GRAPH my_graph VERTEX TABLES (my_table_1);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create property graph if not exists statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE PROPERTY GRAPH IF NOT EXISTS sales_graph VERTEX TABLES (customers);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create property graph edge statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE PROPERTY GRAPH sales_graph
                  VERTEX TABLES (
                    customers KEY(customer_id) PROPERTIES ARE ALL COLUMNS,
                    products KEY(product_id) NO PROPERTIES)
                  EDGE TABLES (
                    orders
                      SOURCE KEY(customer_id) REFERENCES customers(customer_id)
                      DESTINATION KEY(product_id) REFERENCES products(product_id)
                      LABEL purchased
                      PROPERTIES(order_id AS id))
                  OPTIONS (TRUSTED MODE ALLOW MIXED PROPERTY TYPES);
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create Iceberg table statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE ICEBERG TABLE lake.sales_orders
                  (order_id NUMBER, order_status STRING, order_created_at TIMESTAMP)
                  WITHIN CATALOG lake_catalog
                  STORAGE LOCATION 's3://warehouse/sales_orders';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create Iceberg table without columns statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE ICEBERG TABLE sales_orders_archive
                  WITHIN CATALOG archive_catalog
                  STORAGE LOCATION 's3://warehouse/sales_orders_archive';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create profile statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE PROFILE new_profile
                  LIMIT PASSWORD_REUSE_MAX 10
                        PASSWORD_REUSE_TIME 30;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create mandatory profile statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE MANDATORY PROFILE c##cdb_profile
                  LIMIT PASSWORD_VERIFY_FUNCTION my_mandatory_function
                        PASSWORD_GRACE_TIME DEFAULT
                  CONTAINER = ALL;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create profile resource statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE PROFILE app_user LIMIT
                  SESSIONS_PER_USER UNLIMITED
                  CPU_PER_SESSION UNLIMITED
                  CPU_PER_CALL 3000
                  CONNECT_TIME 45
                  LOGICAL_READS_PER_SESSION DEFAULT
                  LOGICAL_READS_PER_CALL 1000
                  PRIVATE_SGA 15K
                  COMPOSITE_LIMIT 5000000
                  PASSWORD_ROLLOVER_TIME 1;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create schema statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE SCHEMA AUTHORIZATION oe
                  CREATE TABLE new_product (
                    color VARCHAR2(10) PRIMARY KEY,
                    quantity NUMBER
                  )
                  CREATE VIEW new_product_view AS
                    SELECT 1 AS color, 10 AS quantity
                  GRANT CREATE SESSION TO hr;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create flashback archive statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE FLASHBACK ARCHIVE DEFAULT test_archive1
                  TABLESPACE example
                  QUOTA 1 M
                  RETENTION 1 DAY;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create flashback archive optimized statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE FLASHBACK ARCHIVE test_archive2
                  TABLESPACE example
                  QUOTA UNLIMITED
                  RETENTION 1 MONTH
                  OPTIMIZE DATA;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create context statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE CONTEXT hr_context USING emp_mgmt;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create context global statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE CONTEXT hr_context USING hr.emp_mgmt
                  SHARING = METADATA
                  INITIALIZED GLOBALLY
                  ACCESSED GLOBALLY;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create MLE env statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE MLE ENV scott.myenv;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create MLE env clone statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE PURE MLE ENV IF NOT EXISTS scott.myenv CLONE other_env;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create MLE env imports statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE MLE ENV scott.analytics_env
                  IMPORTS ("math" MODULE scott.math_module, "util" MODULE scott.util_module)
                  LANGUAGE OPTIONS 'js.strict=true';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create MLE module statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE MLE MODULE IF NOT EXISTS scott.my_mle_module
                  LANGUAGE JAVASCRIPT
                  VERSION '1.0.0'
                  USING CLOB (
                    SELECT 'export function add(a, b) { return a + b; }' FROM dual
                  );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create assertion statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE ASSERTION IF NOT EXISTS company_must_have_a_president
                  CHECK (
                    EXISTS (
                      SELECT 'a president'
                      FROM employees
                      WHERE job_id = 'AD_PRES'
                    )
                  );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create assertion universal statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE ASSERTION employee_in_every_dept
                  CHECK (
                    ALL (
                      SELECT department_id
                      FROM departments
                    ) dept
                    SATISFY (
                      EXISTS (
                        SELECT 'an employee'
                        FROM employees
                        WHERE department_id = dept.department_id
                      )
                    )
                  )
                  DEFERRABLE INITIALLY DEFERRED;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create directive statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE DIRECTIVE validate_order UPDATE, INSERT BEFORE OBJECT ENABLE VALIDATE
                  USING FUNCTION validate_order_fn;

                CREATE DIRECTIVE validate_read SELECT AFTER OBJECT DISABLE NOVALIDATE
                  USING JSON '$.status';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create edition statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE EDITION test_ed;

                CREATE EDITION IF NOT EXISTS test_ed2 AS CHILD OF test_ed;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create inmemory join group statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE INMEMORY JOIN GROUP prod_id1
                  (inventories(product_id), order_items(product_id));

                CREATE INMEMORY JOIN GROUP IF NOT EXISTS oe.prod_id2
                  (inventories(product_id), pm.online_media(product_id));
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create Java statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE JAVA SOURCE NAMED "Welcome" AS 'public class Welcome {}';

                CREATE OR REPLACE AND RESOLVE NOFORCE JAVA CLASS SCHEMA app
                  USING CLOB (
                    SELECT 'compiled class bytes' FROM dual
                  );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create logical partition tracking statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE LOGICAL PARTITION TRACKING ON sales
                  PARTITION BY RANGE (sale_date)
                  INTERVAL ('1')
                  (
                    PARTITION p2025 VALUES LESS THAN ('2026-01-01'),
                    PARTITION pmax VALUES LESS THAN (MAXVALUE)
                  );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle graph, Iceberg, context, and MLE drop statements through SQLDelight environment exactly") {
            val sql =
                """
                DROP PROPERTY GRAPH IF EXISTS hr.customer_graph;

                DROP PROPERTY GRAPH sales_graph;

                DROP ICEBERG TABLE lake.sales_orders WITHIN CATALOG lake_catalog PURGE;

                DROP ICEBERG TABLE sales_orders_archive WITHIN CATALOG archive_catalog;

                DROP CONTEXT hr_context;

                DROP MLE ENV IF EXISTS hr.analytics_env;

                DROP MLE ENV scratch_env;

                DROP MLE MODULE IF EXISTS hr.forecast_module;

                DROP MLE MODULE cleanup_module;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle graph and JSON duality view alter statements through SQLDelight environment exactly") {
            val sql =
                """
                ALTER PROPERTY GRAPH g COMPILE;

                ALTER PROPERTY GRAPH IF EXISTS hr.customer_graph COMPILE;

                ALTER JSON DUALITY VIEW orders_dv ENABLE LOGICAL REPLICATION;

                ALTER JSON RELATIONAL DUALITY VIEW IF EXISTS hr.orders_dv DISABLE LOGICAL REPLICATION;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create JSON relational duality view statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE OR REPLACE JSON RELATIONAL DUALITY VIEW orders_ov AS
                SELECT JSON_OBJECT('_id' VALUE ord.order_id, 'OrderStatus' VALUE ord.order_status)
                FROM orders ord;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle create JSON duality view replication statement through SQLDelight environment exactly") {
            val sql =
                """
                CREATE NO FORCE EDITIONABLE JSON DUALITY VIEW IF NOT EXISTS orders_ov
                  ENABLE LOGICAL REPLICATION AS
                SELECT JSON_OBJECT('_id' VALUE order_id)
                FROM orders;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle MLE alter statements through SQLDelight environment exactly") {
            val sql =
                """
                ALTER MLE ENV scott.myenv
                  ADD IMPORTS ("math" MODULE scott.math_module);

                ALTER MLE ENV IF EXISTS scott.myenv
                  ALTER IMPORTS ("util" MODULE scott.util_module, "config" MODULE scott.config_module);

                ALTER MLE ENV scott.myenv
                  DROP IMPORTS ("math", "util");

                ALTER MLE ENV scott.myenv
                  SET LANGUAGE OPTIONS 'js.strict=true';

                ALTER MLE ENV scott.myenv COMPILE;

                ALTER MLE MODULE IF EXISTS scott.my_mle_module
                  SET METADATA USING CLOB (
                    SELECT JSON('{ "version": "1.2.0" }')
                  );
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle assertion, directive, edition, flashback, and In-Memory drop statements exactly") {
            val sql =
                """
                DROP ASSERTION IF EXISTS company_must_have_a_president;

                DROP ASSERTION hr.salary_within_job_limit;

                DROP DIRECTIVE validate_email_directive;

                DROP EDITION old_release IF EXISTS CASCADE;

                DROP EDITION patch_release;

                DROP FLASHBACK ARCHIVE archive_a;

                DROP INMEMORY JOIN GROUP IF EXISTS sh.prod_id1;

                DROP INMEMORY JOIN GROUP prod_id2;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle assertion, directive, flashback, In-Memory, and lockdown alter statements exactly") {
            val sql =
                """
                ALTER ASSERTION IF EXISTS company_must_have_a_president DISABLE;

                ALTER ASSERTION staff_earn_less_than_manager VALIDATE;

                ALTER ASSERTION hr.salary_within_job_limit ENABLE NOVALIDATE;

                ALTER DIRECTIVE validate_email_directive ENABLE;

                ALTER DIRECTIVE validate_email_directive DISABLE;

                ALTER FLASHBACK ARCHIVE fla1 SET DEFAULT;

                ALTER FLASHBACK ARCHIVE fla1 ADD TABLESPACE tbs1 QUOTA 10 G;

                ALTER FLASHBACK ARCHIVE fla1 MODIFY RETENTION 1 YEAR;

                ALTER FLASHBACK ARCHIVE fla1 PURGE BEFORE SCN 728969;

                ALTER FLASHBACK ARCHIVE fla1 NO OPTIMIZE DATA;

                ALTER INMEMORY JOIN GROUP prod_id1 ADD(product_descriptions(product_id));

                ALTER INMEMORY JOIN GROUP IF EXISTS sh.prod_id1 REMOVE(product_descriptions(product_id));

                ALTER LOCKDOWN PROFILE hr_prof DISABLE FEATURE = ('NETWORK_ACCESS');

                ALTER LOCKDOWN PROFILE hr_prof
                  ENABLE OPTION = ('PARTITIONING');

                ALTER LOCKDOWN PROFILE hr_prof
                  DISABLE STATEMENT = ('ALTER SESSION') CLAUSE = ('SET') OPTION = ('QUERY_REWRITE_ENABLED') VALUE = ('FALSE')
                  USERS = LOCAL;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative legacy drop statements exactly") {
            val sql =
                """
                DROP LOCKDOWN PROFILE hr_prof;

                DROP PMEM FILESTORE cloud_db_1 EXCLUDING CONTENTS;

                DROP PMEM FILESTORE cloud_db_2 INCLUDING CONTENTS;

                DROP PMEM FILESTORE cloud_db_3 FORCE INCLUDING CONTENTS;

                DROP PROFILE app_user CASCADE;

                DROP PROFILE reporting_user;

                DROP ROLLBACK SEGMENT rbs_one;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative legacy alter statements exactly") {
            val sql =
                """
                ALTER PMEM FILESTORE cloud_db_1 ADD MOUNTPOINT '/pmem/cloud_db_1' SIZE 10 G;

                ALTER PMEM FILESTORE cloud_db_1 RESIZE SIZE 20 G;

                ALTER PROFILE new_profile
                   LIMIT PASSWORD_REUSE_TIME 90
                   PASSWORD_REUSE_MAX UNLIMITED;

                ALTER PROFILE app_user LIMIT
                   FAILED_LOGIN_ATTEMPTS 5
                   PASSWORD_LOCK_TIME 1;

                ALTER PROFILE app_user2 LIMIT
                   PASSWORD_LIFE_TIME 90
                   PASSWORD_GRACE_TIME 5;

                ALTER RESOURCE COST
                   CPU_PER_SESSION 100
                   CONNECT_TIME 1;

                ALTER RESOURCE COST
                   LOGICAL_READS_PER_SESSION 2
                   CONNECT_TIME 0;

                ALTER ROLLBACK SEGMENT rbs_one ONLINE;

                ALTER ROLLBACK SEGMENT rbs_one OFFLINE;

                ALTER ROLLBACK SEGMENT rbs_one SHRINK TO 100 M;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative drop statements exactly") {
            val sql =
                """
                DROP DATABASE;

                DROP PLUGGABLE DATABASE pdb1 INCLUDING DATAFILES;

                DROP PLUGGABLE DATABASE app_root_clone FORCE KEEP DATAFILES;

                DROP TABLESPACE IF EXISTS tbs_01 DROP QUOTA INCLUDING CONTENTS CASCADE CONSTRAINTS;

                DROP TABLESPACE tbs_02 INCLUDING CONTENTS AND DATAFILES;

                DROP TABLESPACE SET ts1 KEEP QUOTA INCLUDING CONTENTS KEEP DATAFILES;

                DROP DISKGROUP dgroup_01 INCLUDING CONTENTS;

                DROP DISKGROUP dgroup_02 FORCE INCLUDING CONTENTS;

                DROP RESTORE POINT good_data;

                DROP RESTORE POINT pdb_good_data FOR PLUGGABLE DATABASE pdb1;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative database create statements exactly") {
            val sql =
                """
                CREATE DATABASE mynewdb
                  USER SYS IDENTIFIED BY sys_password
                  USER SYSTEM IDENTIFIED BY system_password
                  LOGFILE GROUP 1 ('/u01/log1a.rdo', '/u02/log1b.rdo') SIZE 100 M
                  MAXLOGFILES 5
                  MAXDATAFILES 100
                  CHARACTER SET AL32UTF8
                  NATIONAL CHARACTER SET AL16UTF16
                  EXTENT MANAGEMENT LOCAL
                  DATAFILE '/u01/system01.dbf' SIZE 325 M
                  SYSAUX DATAFILE '/u01/sysaux01.dbf' SIZE 325 M
                  DEFAULT TABLESPACE users
                  DEFAULT TEMPORARY TABLESPACE temp TEMPFILE '/u01/temp01.dbf' SIZE 20 M
                  UNDO TABLESPACE undotbs DATAFILE '/u01/undotbs01.dbf' SIZE 200 M
                  ENABLE PLUGGABLE DATABASE;

                CREATE CONTROLFILE REUSE DATABASE prod RESETLOGS ARCHIVELOG
                  MAXLOGFILES 16
                  MAXLOGMEMBERS 3
                  MAXDATAFILES 100
                  LOGFILE GROUP 1 '/u01/log1.log' SIZE 100 M
                  DATAFILE '/u01/system01.dbf';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle pluggable database create statements exactly") {
            val sql =
                """
                CREATE PLUGGABLE DATABASE pdb1
                  ADMIN USER pdb_admin IDENTIFIED BY pdb_password
                  ROLES = (DBA)
                  DEFAULT TABLESPACE users
                  DATAFILE '/u01/pdb1/users01.dbf' SIZE 250 M
                  FILE_NAME_CONVERT = ('/pdbseed/', '/pdb1/');

                CREATE PLUGGABLE DATABASE pdb_clone FROM pdb1
                  FILE_NAME_CONVERT = ('/pdb1/', '/pdb_clone/')
                  STORAGE MAXSIZE 2 G;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle tablespace and diskgroup create statements exactly") {
            val sql =
                """
                CREATE TABLESPACE app_data
                  DATAFILE '/u01/app_data01.dbf' SIZE 100 M
                  AUTOEXTEND ON NEXT 10 M MAXSIZE 1 G
                  LOGGING
                  EXTENT MANAGEMENT LOCAL
                  SEGMENT SPACE MANAGEMENT AUTO;

                CREATE BIGFILE TABLESPACE big_data
                  DATAFILE '/u01/big_data01.dbf' SIZE 1 G
                  NOLOGGING;

                CREATE TEMPORARY TABLESPACE temp_ts
                  TEMPFILE '/u01/temp_ts01.dbf' SIZE 100 M;

                CREATE TABLESPACE SET ts1
                  IN SHARDSPACE sgr1
                  USING TEMPLATE
                  (DATAFILE SIZE 100 M EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO);

                CREATE DISKGROUP data NORMAL REDUNDANCY
                  FAILGROUP controller1 DISK '/devices/diska1' NAME diska1 SIZE 100 G
                  FAILGROUP controller2 DISK '/devices/diskb1' NAME diskb1 SIZE 100 G
                  ATTRIBUTE 'compatible.asm' = '19.0';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle parameter file and true cache create statements exactly") {
            val sql =
                """
                CREATE SPFILE = 's_params.ora' FROM PFILE = '${'$'}ORACLE_HOME/work/t_init1.ora';

                CREATE SPFILE FROM MEMORY;

                CREATE PFILE = 'my_init.ora' FROM SPFILE = 's_params.ora';

                CREATE PFILE FROM MEMORY;

                CREATE TRUE CACHE true_cache1 FOR primary_db
                  CONNECT TO primary_user IDENTIFIED BY primary_password
                  CACHE DIRECTORY '/u01/true_cache';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative legacy create statements exactly") {
            val sql =
                """
                CREATE LOCKDOWN PROFILE hr_prof;

                CREATE LOCKDOWN PROFILE hr_prof_copy INCLUDING PRIVATE_DBAAS;

                CREATE PMEM FILESTORE cloud_db_1
                  MOUNTPOINT '/corp/db/cloud_db_1'
                  BACKINGFILE '/var/pmem/foo_1' SIZE 2 T
                  BLOCKSIZE 8 K
                  AUTOEXTEND ON NEXT 10 G MAXSIZE 3 T;

                CREATE RESTORE POINT good_data;

                CREATE RESTORE POINT pdb_good_data
                  FOR PLUGGABLE DATABASE pdb1
                  AS OF SCN 123456
                  PRESERVE;

                CREATE CLEAN RESTORE POINT before_upgrade
                  FOR PLUGGABLE DATABASE salespdb
                  GUARANTEE FLASHBACK DATABASE;

                CREATE ROLLBACK SEGMENT rbs_one
                  TABLESPACE rbs_ts
                  STORAGE (INITIAL 50 K NEXT 50 K OPTIMAL 100 K);

                CREATE PUBLIC ROLLBACK SEGMENT public_rbs TABLESPACE rbs_ts;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle database and pluggable database alter statements exactly") {
            val sql =
                """
                ALTER DATABASE MOUNT;

                ALTER DATABASE MOUNT STANDBY DATABASE;

                ALTER DATABASE OPEN READ ONLY;

                ALTER DATABASE OPEN RESETLOGS;

                ALTER DATABASE ARCHIVELOG;

                ALTER DATABASE FLASHBACK ON;

                ALTER DATABASE RENAME GLOBAL_NAME TO sales.us.example.com;

                ALTER DATABASE DEFAULT TABLESPACE users;

                ALTER DATABASE DATAFILE '/u01/oradata/users01.dbf' ONLINE;

                ALTER DATABASE ADD LOGFILE GROUP 3 ('/u01/oradata/redo03.log') SIZE 100 M;

                ALTER DATABASE DICTIONARY ENCRYPT CREDENTIALS;

                ALTER DATABASE DICTIONARY REKEY CREDENTIALS;

                ALTER DATABASE DICTIONARY DELETE CREDENTIALS KEY;

                ALTER PLUGGABLE DATABASE pdb1 OPEN READ WRITE;

                ALTER PLUGGABLE DATABASE pdb1 CLOSE IMMEDIATE;

                ALTER PLUGGABLE DATABASE pdb1 SAVE STATE;

                ALTER PLUGGABLE DATABASE pdb1 DISCARD STATE;

                ALTER PLUGGABLE DATABASE pdb1 UNPLUG INTO '/tmp/pdb1.xml';

                ALTER PLUGGABLE DATABASE ALL OPEN;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administrative storage alter statements exactly") {
            val sql =
                """
                ALTER TABLESPACE IF EXISTS users ONLINE;

                ALTER TABLESPACE users OFFLINE NORMAL;

                ALTER TABLESPACE users READ ONLY;

                ALTER TABLESPACE users READ WRITE;

                ALTER TABLESPACE users BEGIN BACKUP;

                ALTER TABLESPACE users END BACKUP;

                ALTER TABLESPACE users ADD DATAFILE '/u01/oradata/users02.dbf' SIZE 100 M;

                ALTER TABLESPACE users DROP DATAFILE '/u01/oradata/users02.dbf';

                ALTER TABLESPACE users RENAME TO app_users;

                ALTER TABLESPACE temp SHRINK SPACE KEEP 128 M;

                ALTER TABLESPACE SET ts1 ADD TABLESPACE users;

                ALTER TABLESPACE SET ts1 DROP TABLESPACE users;

                ALTER TABLESPACE SET ts1 RENAME TO ts2;

                ALTER DISKGROUP data MOUNT;

                ALTER DISKGROUP data DISMOUNT;

                ALTER DISKGROUP data ADD DISK '/devices/diskb1' NAME diskb1;

                ALTER DISKGROUP data DROP DISK diskb1;

                ALTER DISKGROUP data REBALANCE POWER 4;

                ALTER DISKGROUP data CHECK ALL REPAIR;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle transaction constraint statements exactly") {
            val sql =
                """
                COMMIT;

                COMMIT WORK;

                COMMIT COMMENT 'monthly close';

                COMMIT WORK WRITE NOWAIT BATCH;

                COMMIT FORCE '22.57.53', 1;

                SAVEPOINT before_batch;

                ROLLBACK;

                ROLLBACK WORK TO before_batch;

                ROLLBACK TO SAVEPOINT before_batch;

                ROLLBACK FORCE '25.32.87';

                SET TRANSACTION READ ONLY;

                SET TRANSACTION READ WRITE NAME 'bulk load';

                SET TRANSACTION ISOLATION LEVEL SERIALIZABLE NAME 'serial report';

                SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

                SET TRANSACTION USE ROLLBACK SEGMENT rbs_one;

                SET TRANSACTION NAME 'named transaction';

                SET CONSTRAINTS ALL IMMEDIATE;

                SET CONSTRAINT emp_job_nn DEFERRED;

                SET CONSTRAINTS emp_job_nn, emp_salary_min, hr.jhist_dept_fk DEFERRED;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle role and data grant session statements exactly") {
            val sql =
                """
                SET ROLE dw_manager IDENTIFIED BY warehouse_password;

                SET ROLE app_user, reporting_user IDENTIFIED BY reporting_password;

                SET ROLE ALL;

                SET ROLE ALL EXCEPT dw_manager, app_admin;

                SET ROLE NONE;

                SET USE DATA GRANTS ONLY ON hr.employees ENABLED;

                SET USE DATA GRANTS ONLY ON employees_view DISABLED;

                SET USE DATA GRANTS ONLY ON reporting.sales_summary;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle alter session and alter system statements exactly") {
            val sql =
                """
                ALTER SESSION ADVISE COMMIT;

                ALTER SESSION CLOSE DATABASE LINK local;

                ALTER SESSION ENABLE PARALLEL DML;

                ALTER SESSION FORCE PARALLEL QUERY PARALLEL 8;

                ALTER SESSION ENABLE RESUMABLE TIMEOUT 3600 NAME 'bulk load';

                ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY MM DD HH24:MI:SS';

                ALTER SESSION SET CONTAINER = pdb1 SERVICE = app_service;

                ALTER SESSION SET ROW ARCHIVAL VISIBILITY = ALL;

                ALTER SYSTEM ARCHIVE LOG CHANGE 9356083;

                ALTER SYSTEM CHECKPOINT;

                ALTER SYSTEM FLUSH SHARED_POOL;

                ALTER SYSTEM SWITCH LOGFILE;

                ALTER SYSTEM ENABLE RESTRICTED SESSION;

                ALTER SYSTEM DISABLE DISTRIBUTED RECOVERY;

                ALTER SYSTEM KILL SESSION '39, 23' IMMEDIATE;

                ALTER SYSTEM DISCONNECT SESSION '13, 8' POST_TRANSACTION;

                ALTER SYSTEM SET QUERY_REWRITE_ENABLED = TRUE;

                ALTER SYSTEM SET DISPATCHERS = '(INDEX=0)(PROTOCOL=TCP)(DISPATCHERS=5)', '(INDEX=1)(PROTOCOL=ipc)(DISPATCHERS=10)';

                ALTER SYSTEM SET RESOURCE_LIMIT = TRUE SCOPE = BOTH SID = '*';

                ALTER SYSTEM RESET resource_limit SCOPE = SPFILE SID = '*';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle administer key management statements exactly") {
            val sql =
                """
                ADMINISTER KEY MANAGEMENT
                  CREATE KEYSTORE '/etc/ORACLE/WALLETS/orcl'
                  IDENTIFIED BY password;

                ADMINISTER KEY MANAGEMENT
                  CREATE AUTO_LOGIN KEYSTORE FROM KEYSTORE '/etc/ORACLE/WALLETS/orcl'
                  IDENTIFIED BY password;

                ADMINISTER KEY MANAGEMENT
                  SET KEYSTORE OPEN
                  IDENTIFIED BY password
                  CONTAINER = CURRENT;

                ADMINISTER KEY MANAGEMENT
                  SET KEYSTORE CLOSE;

                ADMINISTER KEY MANAGEMENT
                  SET KEYSTORE OPEN
                  IDENTIFIED BY EXTERNAL STORE;

                ADMINISTER KEY MANAGEMENT
                  BACKUP KEYSTORE USING 'hr.emp_keystore'
                  IDENTIFIED BY password
                  TO '/etc/ORACLE/KEYSTORE/DB1/';

                ADMINISTER KEY MANAGEMENT
                  ALTER KEYSTORE PASSWORD IDENTIFIED BY old_password
                  SET new_password WITH BACKUP USING 'pwd_change';

                ADMINISTER KEY MANAGEMENT
                  MERGE KEYSTORE '/etc/ORACLE/KEYSTORE/DB1'
                  AND KEYSTORE '/etc/ORACLE/KEYSTORE/DB2'
                  IDENTIFIED BY existing_keystore_password
                  INTO NEW KEYSTORE '/etc/ORACLE/KEYSTORE/DB3'
                  IDENTIFIED BY new_keystore_password;

                ADMINISTER KEY MANAGEMENT
                  MERGE KEYSTORE '/etc/ORACLE/KEYSTORE/DB1'
                  INTO EXISTING KEYSTORE '/etc/ORACLE/KEYSTORE/DB2'
                  IDENTIFIED BY existing_keystore_password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  ISOLATE KEYSTORE IDENTIFIED BY isolated_keystore_password
                  FROM ROOT KEYSTORE FORCE KEYSTORE
                  IDENTIFIED BY united_keystore_password
                  WITH BACKUP USING 'isolate_backup';

                ADMINISTER KEY MANAGEMENT
                  UNITE KEYSTORE IDENTIFIED BY isolated_keystore_password
                  WITH ROOT KEYSTORE
                  IDENTIFIED BY EXTERNAL STORE
                  WITH BACKUP USING 'unite_backup';

                ADMINISTER KEY MANAGEMENT
                  SET KEY USING ALGORITHM 'AES256'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  SET ENCRYPTION KEY 'mkid:mk' USING TAG 'rotation-2026' USING ALGORITHM 'AES256'
                  FORCE KEYSTORE
                  IDENTIFIED BY EXTERNAL STORE
                  WITH BACKUP
                  CONTAINER = ALL;

                ADMINISTER KEY MANAGEMENT
                  CREATE KEY USING TAG 'mykey1'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  USE KEY 'ARgEtzPxpE'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  SET TAG 'mykey2' FOR 'ARgEtzPxpE'
                  FORCE KEYSTORE
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  EXPORT KEYS WITH SECRET 'my_secret'
                  TO '/etc/TDE/export.exp'
                  IDENTIFIED BY password
                  WITH IDENTIFIER IN 'AdoxnJ0uH08', 'AW5z3CoyKE';

                ADMINISTER KEY MANAGEMENT
                  IMPORT KEYS WITH SECRET 'my_secret'
                  FROM '/etc/TDE/export.exp'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  SET ENCRYPTION KEY IDENTIFIED BY user_password
                  MIGRATE USING software_keystore_password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  SET ENCRYPTION KEY IDENTIFIED BY software_keystore_password
                  REVERSE MIGRATE USING user_password;

                ADMINISTER KEY MANAGEMENT
                  ADD SECRET 'secret1' FOR CLIENT 'client1'
                  USING TAG 'My first secret'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  UPDATE SECRET 'secret1' FOR CLIENT 'client1'
                  USING TAG 'New Tag 1'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  DELETE SECRET FOR CLIENT 'client1'
                  IDENTIFIED BY password
                  WITH BACKUP;

                ADMINISTER KEY MANAGEMENT
                  SWITCHOVER TO LIBRARY 'updated_pkcs11.so' FOR ALL CONTAINERS;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle unified audit statements exactly") {
            val sql =
                """
                CREATE AUDIT POLICY table_pol
                  PRIVILEGES CREATE ANY TABLE, DROP ANY TABLE;

                CREATE AUDIT POLICY dml_pol
                  ACTIONS DELETE ON hr.employees,
                          INSERT ON hr.employees,
                          UPDATE ON hr.employees,
                          ALL ON hr.departments;

                CREATE AUDIT POLICY read_dir_pol
                  ACTIONS READ ON DIRECTORY bfile_dir;

                CREATE AUDIT POLICY security_pol
                  ACTIONS ADMINISTER KEY MANAGEMENT;

                CREATE AUDIT POLICY dp_actions_pol
                  ACTIONS COMPONENT = datapump IMPORT;

                CREATE AUDIT POLICY java_pol
                  ROLES java_admin, java_deploy;

                CREATE AUDIT POLICY order_updates_pol
                  ACTIONS UPDATE ON oe.orders
                  WHEN 'SYS_CONTEXT(''USERENV'', ''IDENTIFICATION_TYPE'') = ''EXTERNAL'''
                  EVALUATE PER SESSION;

                CREATE AUDIT POLICY local_table_pol
                  PRIVILEGES CREATE ANY TABLE, DROP ANY TABLE
                  CONTAINER = CURRENT;

                CREATE AUDIT POLICY c##common_role1_pol
                  ROLES c##role1
                  CONTAINER = ALL;

                CREATE AUDIT POLICY column_pol
                  ACTIONS GRANT(job) ON scott.emp;

                ALTER AUDIT POLICY dml_pol
                  ADD PRIVILEGES CREATE ANY TABLE, DROP ANY TABLE;

                ALTER AUDIT POLICY security_pol
                  ADD PRIVILEGES CREATE ANY LIBRARY, DROP ANY LIBRARY
                      ACTIONS DELETE ON hr.employees,
                              INSERT ON hr.employees,
                              UPDATE ON hr.employees,
                              ALL ON hr.departments
                      ROLES dba, connect;

                ALTER AUDIT POLICY dml_pol
                  DROP ACTIONS INSERT ON hr.employees,
                               UPDATE ON hr.employees;

                ALTER AUDIT POLICY dp_actions_pol
                  ADD ACTIONS COMPONENT = datapump EXPORT
                  DROP ACTIONS COMPONENT = datapump IMPORT;

                ALTER AUDIT POLICY order_updates_pol
                  CONDITION DROP;

                ALTER AUDIT POLICY emp_updates_pol
                  CONDITION WHEN 'UID = 102'
                  EVALUATE PER STATEMENT;

                ALTER AUDIT POLICY hr_audit_policy ADD ONLY TOPLEVEL;

                ALTER AUDIT POLICY hr_audit_policy DROP ONLY TOPLEVEL;

                AUDIT POLICY table_pol;

                AUDIT POLICY dml_pol BY hr, sh;

                AUDIT POLICY read_dir_pol EXCEPT hr;

                AUDIT POLICY security_pol BY hr WHENEVER NOT SUCCESSFUL;

                AUDIT POLICY common_role1_pol BY USERS WITH GRANTED ROLES c##role1;

                AUDIT CONTEXT NAMESPACE userenv
                  ATTRIBUTES current_user, db_name
                  BY hr;

                NOAUDIT POLICY table_pol;

                NOAUDIT POLICY dml_pol BY hr, sh;

                NOAUDIT POLICY common_role1_pol BY USERS WITH GRANTED ROLES c##role1;

                NOAUDIT CONTEXT NAMESPACE userenv
                  ATTRIBUTES current_user, db_name
                  BY hr WHENEVER NOT SUCCESSFUL;

                DROP AUDIT POLICY table_pol;

                AUDIT ROLE;

                AUDIT ROLE WHENEVER SUCCESSFUL;

                AUDIT ROLE WHENEVER NOT SUCCESSFUL;

                AUDIT SELECT TABLE, UPDATE TABLE BY hr, oe;

                AUDIT DELETE ANY TABLE BY ACCESS WHENEVER NOT SUCCESSFUL;

                AUDIT SELECT ON hr.employees WHENEVER SUCCESSFUL;

                AUDIT READ ON DIRECTORY bfile_dir BY ACCESS;

                AUDIT ALL ON DEFAULT;

                AUDIT NETWORK;

                AUDIT DIRECT_PATH LOAD;

                NOAUDIT ROLE;

                NOAUDIT SELECT TABLE BY hr;

                NOAUDIT DELETE ANY TABLE;

                NOAUDIT SELECT ON hr.employees WHENEVER SUCCESSFUL;

                NOAUDIT ALL ON DEFAULT;

                NOAUDIT NETWORK;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle role and user security statements exactly") {
            val sql =
                """
                CREATE DATA ROLE manager_role;

                CREATE OR REPLACE DATA ROLE employee_role MAPPED TO 'AZURE_ROLE=employee';

                CREATE DATA ROLE IF NOT EXISTS it_support_role ENABLED;

                CREATE DATA ROLE disabled_support_role DISABLED;

                GRANT DATA ROLE manager_role TO marvin;

                GRANT DATA ROLE IF EXISTS employee_role TO DATA ROLE manager_role
                  START TIME TO_TIMESTAMP_TZ('2025-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')
                  END TIME TO_TIMESTAMP_TZ('2026-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM');

                GRANT DATA ROLE it_support_role TO APPLICATION IDENTITY hcm_app;

                REVOKE DATA ROLE IF EXISTS manager_role FROM marvin;

                REVOKE DATA ROLE employee_role FROM DATA ROLE manager_role;

                DROP DATA ROLE IF EXISTS old_manager_role;

                DROP DATA ROLE disabled_support_role;

                CREATE OR REPLACE DATA GRANT app_admin.ManagerDirectReports AS
                  SELECT (ALL COLUMNS EXCEPT ssn), UPDATE (salary, commission_pct)
                  ON hr.employees
                  WHERE manager = ORA_APP_USER.username
                  TO app_manager_role
                  START TIME TO_TIMESTAMP_TZ('2025-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')
                  END TIME TO_TIMESTAMP_TZ('2026-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM');

                CREATE DATA GRANT IF NOT EXISTS app_admin.EndUserContextAccess AS
                  SELECT, UPDATE
                  ON sys.end_user_context
                  WHERE owner = 'HR' AND name = 'HCM'
                  TO END USER emma;

                DROP DATA GRANT IF EXISTS app_admin.ManagerDirectReports;

                DROP DATA GRANT app_admin.EndUserContextAccess;

                CREATE APPLICATION IDENTITY hcm_app MAPPED TO 'AZURE_CLIENT_ID = f1fab37e-7aa2-4ff8-849c-7e731fea3b48';

                CREATE OR REPLACE APPLICATION IDENTITY iam_app MAPPED TO 'IAM_OAUTH_CLIENT_ID = ocid1.oauthclient.oc1..example';

                CREATE APPLICATION IDENTITY IF NOT EXISTS payroll_app MAPPED TO 'AZURE_CLIENT_ID = 384ff83f-4a20-46be-88a9-2d6d88a7d520';

                DROP APPLICATION IDENTITY IF EXISTS old_hcm_app;

                DROP APPLICATION IDENTITY old_payroll_app;

                CREATE END USER IF NOT EXISTS emma
                  IDENTIFIED BY password
                  PROFILE app_profile
                  PASSWORD EXPIRE
                  ACCOUNT UNLOCK
                  SCHEMA hr
                  START TIME TO_TIMESTAMP_TZ('2025-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')
                  END TIME TO_TIMESTAMP_TZ('2026-03-01 19:30:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM');

                CREATE END USER marvin SCHEMA hr ACCOUNT LOCK;

                ALTER END USER IF EXISTS emma IDENTIFIED BY new_password REPLACE password
                  PROFILE app_profile
                  PASSWORD EXPIRE
                  ACCOUNT LOCK
                  SCHEMA reporting
                  NO START TIME
                  NO END TIME;

                ALTER END USER marvin ACCOUNT UNLOCK SCHEMA hr;

                DROP END USER IF EXISTS old_emma;

                DROP END USER marvin;

                CREATE END USER CONTEXT hr.hcm USING JSON SCHEMA '{"type":"object","properties":{"emp_id":{"type":"integer","default":1}}}';

                CREATE OR REPLACE END USER CONTEXT IF NOT EXISTS hr.payroll
                  USING JSON SCHEMA '{"type":"object","properties":{"region":{"type":"string","default":"US"}}}';

                DROP END USER CONTEXT IF EXISTS hr.old_hcm;

                DROP END USER CONTEXT hr.old_payroll;

                UPDATE sys.end_user_context t SET t.context.emp_id = 3
                  WHERE owner = 'HR' AND name = 'HCM';

                CREATE USER IF NOT EXISTS app_user IDENTIFIED BY app_password
                  DEFAULT COLLATION binary_ci
                  DEFAULT TABLESPACE users
                  TEMPORARY TABLESPACE temp
                  QUOTA 100 M ON users
                  PROFILE app_profile
                  PASSWORD EXPIRE
                  ACCOUNT UNLOCK
                  ENABLE EDITIONS FORCE
                  CONTAINER = CURRENT
                  READ WRITE;

                CREATE USER external_user IDENTIFIED EXTERNALLY AS 'CN=External User'
                  ACCOUNT LOCK;

                CREATE USER global_user IDENTIFIED GLOBALLY AS 'IAM_GROUP_NAME = analytics'
                  QUOTA UNLIMITED ON data;

                CREATE USER no_auth_user NO AUTHENTICATION DEFAULT TABLESPACE users;

                ALTER USER IF EXISTS app_user IDENTIFIED BY new_password REPLACE app_password
                  DEFAULT ROLE app_reader, app_writer
                  PASSWORD EXPIRE
                  ACCOUNT LOCK;

                ALTER USER app_user ADD ( FACTOR 'otp' AS 'external-otp' );

                ALTER USER app_user UPDATE ( FACTOR 'otp' AS 'external-otp-v2' );

                ALTER USER app_user DROP FACTOR 'otp';

                ALTER USER app_user DEFAULT ROLE ALL EXCEPT app_admin;

                ALTER USER app_user EXPIRE PASSWORD ROLLOVER PERIOD;

                ALTER USER app_user ENABLE DICTIONARY PROTECTION READ ONLY;

                ALTER USER app_user SET CONTAINER_DATA = ALL;

                ALTER USER app_user SET CONTAINER_DATA = DEFAULT FOR sys.cdb_users;

                ALTER USER app_user ADD CONTAINER_DATA = (pdb1, pdb2) FOR sys.cdb_tables;

                ALTER USER app_user REMOVE CONTAINER_DATA = (pdb3);

                ALTER USER app_user GRANT CONNECT THROUGH sh WITH ROLE warehouse_user AUTHENTICATION REQUIRED;

                ALTER USER app_user GRANT CONNECT THROUGH app_proxy WITH NO ROLES;

                ALTER USER app_user GRANT CONNECT THROUGH ENTERPRISE USERS;

                ALTER USER app_user REVOKE CONNECT THROUGH sh;

                ALTER USER app_user, reporting_user REVOKE CONNECT THROUGH ENTERPRISE USERS;

                CREATE ROLE IF NOT EXISTS app_reader NOT IDENTIFIED;

                CREATE ROLE app_writer IDENTIFIED BY writer_password CONTAINER = CURRENT;

                CREATE ROLE app_external IDENTIFIED EXTERNALLY CONTAINER = ALL;

                CREATE ROLE app_global IDENTIFIED GLOBALLY AS 'IAM_GROUP_NAME = analytics';

                CREATE ROLE app_package IDENTIFIED USING security.role_auth;

                ALTER ROLE IF EXISTS app_reader IDENTIFIED BY reader_password;

                ALTER ROLE app_writer NOT IDENTIFIED CONTAINER = ALL;

                ALTER ROLE app_external IDENTIFIED GLOBALLY AS 'AZURE_ROLE = app-role';

                GRANT CREATE SESSION, CREATE TABLE TO app_user WITH ADMIN OPTION;

                GRANT SELECT ANY TABLE TO app_reader CONTAINER = CURRENT;

                GRANT app_runtime TO app_user IDENTIFIED BY runtime_password;

                GRANT SELECT, INSERT, UPDATE (name, status) ON hr.employees TO app_user WITH GRANT OPTION;

                GRANT app_role TO PROCEDURE hr.run_job;

                REVOKE CREATE SESSION FROM app_user;

                REVOKE SELECT ANY TABLE FROM app_reader CONTAINER = ALL;

                REVOKE SELECT, UPDATE ON hr.employees FROM app_user CASCADE CONSTRAINTS;

                REVOKE app_role FROM PROCEDURE hr.run_job;

                DROP ROLE IF EXISTS app_reader;

                DROP ROLE app_writer;

                DROP USER IF EXISTS old_app_user CASCADE;

                DROP USER staging_user;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle analyze statements exactly") {
            val sql =
                """
                ANALYZE TABLE hr.employees VALIDATE STRUCTURE CASCADE;

                ANALYZE TABLE customers VALIDATE REF UPDATE SET DANGLING TO NULL;

                ANALYZE INDEX hr.employee_name_idx PARTITION (sales_q1)
                  VALIDATE STRUCTURE CASCADE COMPLETE ONLINE INTO invalid_rows;

                ANALYZE CLUSTER personnel LIST CHAINED ROWS INTO chained_rows;

                ANALYZE TABLE orders DELETE SYSTEM STATISTICS;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle associate and disassociate statistics statements exactly") {
            val sql =
                """
                ASSOCIATE STATISTICS WITH COLUMNS hr.employees.salary, hr.employees.commission_pct
                  USING salary_stats WITH SYSTEM MANAGED STORAGE TABLES;

                ASSOCIATE STATISTICS WITH FUNCTIONS hr.compensation_cost USING cost_stats;

                ASSOCIATE STATISTICS WITH PACKAGES hr.estimates USING NULL;

                ASSOCIATE STATISTICS WITH TYPES hr.employee_t
                  DEFAULT COST (1, 2, 3), DEFAULT SELECTIVITY 10;

                ASSOCIATE STATISTICS WITH INDEXTYPES hr.position_indextype
                  DEFAULT SELECTIVITY 5, DEFAULT COST (10, 20, 30)
                  WITH USER MANAGED STORAGE TABLES;

                DISASSOCIATE STATISTICS FROM COLUMNS hr.employees.salary, hr.employees.commission_pct FORCE;

                DISASSOCIATE STATISTICS FROM FUNCTIONS hr.compensation_cost;

                DISASSOCIATE STATISTICS FROM INDEXTYPES hr.position_indextype FORCE;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle rename and comment statements exactly") {
            val sql =
                """
                RENAME departments_new TO emp_departments;

                COMMENT ON AUDIT POLICY policy_user_actions IS 'Unified audit policy comment';

                COMMENT ON COLUMN employees.job_id IS 'abbreviated job title';

                COMMENT ON COLUMN hr.employees.salary IS '';

                COMMENT ON EDITION patch_release IS 'Patch release edition';

                COMMENT ON INDEXTYPE hr.position_indextype IS 'Position indextype';

                COMMENT ON MATERIALIZED VIEW reporting.sales_mv IS 'Sales materialized view';

                COMMENT ON MINING MODEL sh.sales_model IS 'Sales mining model';

                COMMENT ON OPERATOR hr.eq_op IS 'Equality operator';

                COMMENT ON TABLE employees IS 'Employee table';

                COMMENT ON TABLE hr.employee_view IS 'Employee view';

                COMMENT ON PROPERTY GRAPH hr.customer_graph IS 'Customer graph';
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("parses Oracle purge and flashback statements exactly") {
            val sql =
                """
                PURGE TABLE employees;

                PURGE INDEX hr.employee_name_idx;

                PURGE TABLESPACE users USER hr;

                PURGE TABLESPACE SET shard_ts USER app_user;

                PURGE RECYCLEBIN;

                PURGE DBA_RECYCLEBIN;

                FLASHBACK TABLE hr.employees, hr.departments TO SCN 123456 ENABLE TRIGGERS;

                FLASHBACK TABLE employees TO TIMESTAMP CURRENT_TIMESTAMP DISABLE TRIGGERS;

                FLASHBACK TABLE old_employees TO RESTORE POINT before_cleanup;

                FLASHBACK TABLE dropped_employees TO BEFORE DROP RENAME TO employees;

                FLASHBACK DATABASE TO RESTORE POINT before_upgrade;

                FLASHBACK STANDBY PLUGGABLE DATABASE pdb1 TO BEFORE RESETLOGS;
                """.trimIndent()

            parseOracleSql(sql, fileName = "1.sqm") shouldBe
                ParseResult(
                    fileNames = emptyList(),
                    errors = emptyList(),
                )
        }

        test("reports malformed Oracle SQL through SQLDelight environment exactly") {
            val sql =
                """
                CREATE TABLE sample (
                  id NUMBER(10),
                  name VARCHAR2(100)
                """.trimIndent()

            parseOracleSql(sql) shouldBe
                ParseResult(
                    fileNames = listOf("Test.sq"),
                    errors =
                        listOf(
                            "Test.sq: (3, 20): ')', ',', <column constraint real> or AS expected",
                        ),
                )
        }
    })

private data class ParseResult(
    val fileNames: List<String>,
    val errors: List<String>,
)

private fun parseOracleSql(
    sql: String,
    fileName: String = "Test.sq",
): ParseResult {
    val root = Files.createTempDirectory("sqldelight-oracle-parser-test").toFile()
    val sourceDirectory = File(root, "com/example").apply { mkdirs() }
    File(sourceDirectory, fileName).writeText(sql)

    val errors = mutableListOf<String>()
    val compilationUnit = OracleParserTestCompilationUnit(File(root, "output"))
    val environment =
        SqlDelightEnvironment(
            sourceFolders = listOf(root),
            dependencyFolders = emptyList(),
            properties = OracleParserTestDatabaseProperties(root, compilationUnit),
            dialect = OracleDialect(),
            verifyMigrations = true,
            moduleName = "oracle-parser-test",
            compilationUnit = compilationUnit,
        )

    LanguageParserDefinitions.INSTANCE.forLanguage(SqlDelightLanguage).createParser(environment.project)
    LanguageParserDefinitions.INSTANCE.forLanguage(MigrationLanguage).createParser(environment.project)
    environment.annotate(listOf(OptimisticLockCompilerAnnotator()), createAnnotationHolder(errors))

    val fileNames = mutableListOf<String>()
    environment.forSourceFiles { psiFile ->
        if (psiFile is SqlDelightQueriesFile) {
            fileNames += psiFile.name
        }
    }

    return ParseResult(
        fileNames = fileNames.sorted(),
        errors = errors,
    )
}

private data class OracleParserTestCompilationUnit(
    override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit {
    override val name: String = "test"
    override val sourceFolders: Set<SqlDelightSourceFolder> = emptySet()
}

private data class OracleParserTestDatabaseProperties(
    override val rootDirectory: File,
    private val compilationUnit: SqlDelightCompilationUnit,
) : SqlDelightDatabaseProperties {
    override val packageName: String = "com.example"
    override val className: String = "TestDatabase"
    override val dependencies: List<SqlDelightDatabaseName> = emptyList()
    override val compilationUnits: List<SqlDelightCompilationUnit> = listOf(compilationUnit)
    override val deriveSchemaFromMigrations: Boolean = false
    override val generateAsync: Boolean = false
    override val expandSelectStar: Boolean = true
    override val treatNullAsUnknownForEquality: Boolean = false
}

private fun createAnnotationHolder(errors: MutableList<String>): SqlAnnotationHolder =
    SqlAnnotationHolder { element, message ->
        val documentManager = PsiDocumentManager.getInstance(element.project)
        val document = requireNotNull(documentManager.getDocument(element.containingFile))
        val lineNumber = document.getLineNumber(element.textOffset)
        val offsetInLine = element.textOffset - document.getLineStartOffset(lineNumber)
        errors += "${element.containingFile.name}: (${lineNumber + 1}, $offsetInLine): $message"
    }
