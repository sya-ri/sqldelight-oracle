# sqldelight-check oracle rules

`sqldelight-check-oracle-rule` is the Oracle Database-specific rule set for sqldelight-check.

These rules are gated by `OracleDialectId` and focus on Oracle null semantics, generated keys, numeric type declarations, sequence semantics, and migration DDL that can surprise live Oracle databases.

## Install

Add this artifact to the SQLDelight project checked by sqldelight-check:

```kotlin
dependencies {
    sqldelightCheckRuleSet("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule:0.1.0")
}
```

Oracle rules need Oracle dialect metadata from `sqldelight-check-oracle-dialect` so sqldelight-check can resolve the `oracle` dialect ID:

```kotlin
dependencies {
    sqldelightCheckDialects("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect:0.1.0")
    sqldelightCheckRuleSet("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule:0.1.0")
}
```

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        oracle {
            enabled.set(false)
        }
    }
}
```

Rule IDs use the `oracle:<rule-name>` form.

## Configuration Model

- `Yes` in the Enable column means the rule is enabled automatically for Oracle dialects.
- `Warning` in the Severity column shows the built-in default severity.
- The Fix column is blank when write tasks do not attach a fix.

Built-in rules use `Severity.Warning` for visible Oracle-specific findings that need a human migration or query decision.
`Severity.Info` and `Severity.Error` are supported through user configuration.

Users can override enablement and severity in `build.gradle.kts`:

```kotlin
import dev.s7a.sqldelight.check.api.Severity

sqldelightCheck {
    ruleSets {
        oracle {
            enabled.set(true)
        }
    }
    rules {
        rule("oracle:unsafe-ddl-migration") {
            severity.set(Severity.Error)
        }
        rule("oracle:prefer-identity-column") {
            enabled.set(false)
        }
    }
}
```

Database-specific overrides use the SQLDelight database name:

```kotlin
sqldelightCheck {
    databases {
        database("MainDatabase") {
            rules {
                rule("oracle:require-number-precision") {
                    severity.set(Severity.Error)
                }
            }
        }
    }
}
```

## Rule Summary

| Rule ID | Enable | Severity | Fix | Purpose |
| --- | --- | --- | --- | --- |
| [`oracle:nullable-not-in-predicate`](#oraclenullable-not-in-predicate) | Yes | Warning |  | Flag `NOT IN (subquery)` predicates that do not explicitly filter nullable values. |
| [`oracle:no-empty-string-comparison`](#oracleno-empty-string-comparison) | Yes | Warning |  | Avoid comparing to `''`, because Oracle treats zero-length strings as `NULL`. |
| [`oracle:no-conflicting-annotation-operations`](#oracleno-conflicting-annotation-operations) | Yes | Warning |  | Avoid conflicting annotation operations for the same annotation name. |
| [`oracle:no-conflicting-commit-write-clauses`](#oracleno-conflicting-commit-write-clauses) | Yes | Warning |  | Avoid conflicting `COMMIT WRITE` clauses. |
| [`oracle:no-conflicting-constraint-state`](#oracleno-conflicting-constraint-state) | Yes | Warning |  | Avoid mutually exclusive Oracle constraint state clauses. |
| [`oracle:no-conflicting-create-view-clauses`](#oracleno-conflicting-create-view-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE VIEW` clauses. |
| [`oracle:no-conflicting-drop-clauses`](#oracleno-conflicting-drop-clauses) | Yes | Warning |  | Avoid conflicting Oracle `DROP` clauses. |
| [`oracle:no-conflicting-flashback-clause`](#oracleno-conflicting-flashback-clause) | Yes | Warning |  | Avoid duplicate Oracle flashback query clauses on one table reference. |
| [`oracle:no-conflicting-index-clauses`](#oracleno-conflicting-index-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE INDEX` and `ALTER INDEX` clauses. |
| [`oracle:no-conflicting-json-storage-clauses`](#oracleno-conflicting-json-storage-clauses) | Yes | Warning |  | Avoid multiple JSON storage clauses for one column. |
| [`oracle:no-conflicting-sequence-clauses`](#oracleno-conflicting-sequence-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE SEQUENCE` and `ALTER SEQUENCE` clauses. |
| [`oracle:no-conflicting-set-role-clauses`](#oracleno-conflicting-set-role-clauses) | Yes | Warning |  | Avoid conflicting `SET ROLE` forms. |
| [`oracle:no-conflicting-set-constraints-clauses`](#oracleno-conflicting-set-constraints-clauses) | Yes | Warning |  | Avoid conflicting `SET CONSTRAINTS` timing clauses. |
| [`oracle:no-conflicting-set-transaction-clauses`](#oracleno-conflicting-set-transaction-clauses) | Yes | Warning |  | Avoid conflicting `SET TRANSACTION` clauses. |
| [`oracle:no-conflicting-synonym-clauses`](#oracleno-conflicting-synonym-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE SYNONYM` clauses. |
| [`oracle:no-conflicting-table-clauses`](#oracleno-conflicting-table-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE TABLE` and `ALTER TABLE` clauses. |
| [`oracle:no-conflicting-xmlschema-permissions`](#oracleno-conflicting-xmlschema-permissions) | Yes | Warning |  | Avoid conflicting XMLSchema permission clauses. |
| [`oracle:no-uppercase-rowid-column`](#oracleno-uppercase-rowid-column) | Yes | Warning |  | Avoid quoted uppercase `"ROWID"` Oracle column names. |
| [`oracle:prefer-identity-column`](#oracleprefer-identity-column) | Yes | Warning |  | Prefer Oracle identity columns over sequence-trigger generated keys. |
| [`oracle:prefer-unified-auditing`](#oracleprefer-unified-auditing) | Yes | Warning |  | Prefer unified auditing over traditional `AUDIT` and `NOAUDIT` statements. |
| [`oracle:require-number-precision`](#oraclerequire-number-precision) | Yes | Warning |  | Require explicit precision and scale for `NUMBER` declarations. |
| [`oracle:unsafe-ddl-migration`](#oracleunsafe-ddl-migration) | Yes | Warning |  | Flag migration DDL that can rewrite, lock, or destructively change large tables. |
| [`oracle:valid-audit-policy-form`](#oraclevalid-audit-policy-form) | Yes | Warning |  | Validate static unified audit policy statement forms. |
| [`oracle:valid-boolean-test-condition`](#oraclevalid-boolean-test-condition) | Yes | Warning |  | Validate static Oracle boolean test conditions. |
| [`oracle:valid-nls-parameter`](#oraclevalid-nls-parameter) | Yes | Warning |  | Validate static Oracle NLS parameter literals in conversion functions. |
| [`oracle:valid-json-condition-options`](#oraclevalid-json-condition-options) | Yes | Warning |  | Validate static SQL/JSON condition option combinations. |
| [`oracle:valid-returning-clause`](#oraclevalid-returning-clause) | Yes | Warning |  | Validate static Oracle `RETURNING` clause forms. |
| [`oracle:valid-row-limiting-clause`](#oraclevalid-row-limiting-clause) | Yes | Warning |  | Validate static Oracle row limiting clause values and `WITH TIES` ordering. |
| [`oracle:valid-like-escape`](#oraclevalid-like-escape) | Yes | Warning |  | Validate static Oracle `LIKE ... ESCAPE` literals. |
| [`oracle:valid-lock-table-wait-clause`](#oraclevalid-lock-table-wait-clause) | Yes | Warning |  | Validate static Oracle `LOCK TABLE` wait clauses. |
| [`oracle:valid-outer-join-operator`](#oraclevalid-outer-join-operator) | Yes | Warning |  | Validate static legacy Oracle outer join operator restrictions. |
| [`oracle:valid-segment-creation-clause`](#oraclevalid-segment-creation-clause) | Yes | Warning |  | Avoid duplicate Oracle `SEGMENT CREATION` clauses. |
| [`oracle:valid-subquery-restriction-clause`](#oraclevalid-subquery-restriction-clause) | Yes | Warning |  | Avoid conflicting Oracle subquery restriction clauses. |

## `oracle:nullable-not-in-predicate`

Reports `NOT IN (subquery)` predicates whose subquery does not explicitly filter null values.
In Oracle, a null returned by the subquery can make the `NOT IN` predicate produce no expected matches.

Prefer filtering the subquery:

```sql
SELECT *
FROM customer
WHERE id NOT IN (
    SELECT customer_id
    FROM invoice
    WHERE customer_id IS NOT NULL
);
```

Or use `NOT EXISTS` when that better expresses the relationship.

## `oracle:no-empty-string-comparison`

Reports equality and inequality comparisons against zero-length text literals.
Oracle treats a zero-length [text literal](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html) as `NULL`, so `= ''`, `= N''`, `'' =`, `<> ''`, `!= ''`, and their reversed forms are misleading.

Prefer null predicates:

```sql
SELECT *
FROM customer
WHERE name IS NULL;
```

## `oracle:no-conflicting-annotation-operations`

Reports conflicting Oracle [`annotations_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/annotations_clause.html) operations for the same annotation name.
The rule checks repeated or opposing `ADD`, `DROP`, and `REPLACE` operations, including `ADD IF NOT EXISTS` and `DROP IF EXISTS` forms, when they target the same static annotation identifier.

Prefer one operation per annotation name:

```sql
ALTER TABLE customer
    ANNOTATIONS (
        ADD sensitive,
        ADD owner
    );
```

## `oracle:no-conflicting-commit-write-clauses`

Reports conflicting Oracle [`COMMIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMIT.html) `WRITE` clauses.
The rule checks repeated `WRITE`, `WAIT` / `NOWAIT`, and `IMMEDIATE` / `BATCH` combinations.

Prefer one write behavior and one write mode:

```sql
COMMIT WRITE WAIT IMMEDIATE;
```

## `oracle:no-conflicting-constraint-state`

Reports mutually exclusive Oracle [`constraint_state`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html) clauses in one table statement.
The rule checks static groups such as `ENABLE` / `DISABLE`, `VALIDATE` / `NOVALIDATE`, `DEFERRABLE` / `NOT DEFERRABLE`, `INITIALLY IMMEDIATE` / `INITIALLY DEFERRED`, and `RELY` / `NORELY`.

Prefer one choice per semantic group:

```sql
ALTER TABLE customer
    ADD CONSTRAINT customer_name_check CHECK (name IS NOT NULL)
    ENABLE VALIDATE;
```

## `oracle:no-conflicting-create-view-clauses`

Reports mutually exclusive Oracle [`CREATE VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-VIEW.html) clauses.
The rule checks `FORCE` / `NO FORCE`, `EDITIONING` / `NONEDITIONING`, and `EDITIONABLE` / `NONEDITIONABLE` before the view query.

Prefer one choice per group:

```sql
CREATE OR REPLACE FORCE EDITIONABLE VIEW customer_view AS
SELECT id FROM customers;
```

## `oracle:no-conflicting-drop-clauses`

Reports conflicting Oracle [`DROP TYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TYPE.html) and repeated [`DROP TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLE.html) clauses.
The rule checks `DROP TYPE` finality choices (`FORCE` / `VALIDATE`) and repeated `DROP TABLE ... PURGE` clauses.

Prefer one choice per statement:

```sql
DROP TYPE address_type FORCE;
DROP TABLE audit_event PURGE;
```

## `oracle:no-conflicting-flashback-clause`

Reports duplicate Oracle flashback query clauses on one table reference.
Oracle [`flashback_query_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) forms allow an `AS OF` point and `VERSIONS` range in documented combinations, but repeating the same flashback clause group on the same table reference is ambiguous.

Prefer one `AS OF` clause and one `VERSIONS` clause at most:

```sql
SELECT *
FROM orders VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE AS OF SCN 123456;
```

## `oracle:no-conflicting-index-clauses`

Reports mutually exclusive Oracle [`CREATE INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEX.html) and [`ALTER INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEX.html) clauses in one statement.
The rule checks static clause groups such as `UNIQUE` / `BITMAP`, `LOGGING` / `NOLOGGING`, `VISIBLE` / `INVISIBLE`, `USABLE` / `UNUSABLE`, `COMPRESS` / `NOCOMPRESS`, `PARALLEL` / `NOPARALLEL`, `ONLINE` / `OFFLINE`, and `INDEXING FULL` / `INDEXING PARTIAL`.

Prefer one choice per semantic group:

```sql
CREATE INDEX customer_email_ix
    ON customer (email)
    NOLOGGING
    INVISIBLE;
```

## `oracle:no-conflicting-json-storage-clauses`

Reports multiple Oracle JSON storage clauses for the same column in one table statement.
The rule is based on Oracle [`json_storage_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/json_storage_clause.html) forms such as `JSON COLUMN column STORE AS ...` and `JSON (column, ...) STORE AS ...`.

Prefer one storage choice per JSON column:

```sql
ALTER TABLE documents
  MODIFY JSON COLUMN payload STORE AS JSON;
```

## `oracle:no-conflicting-sequence-clauses`

Reports mutually exclusive Oracle sequence clauses in one `CREATE SEQUENCE` or `ALTER SEQUENCE` statement.
The rule checks repeated or opposing clause groups such as `SHARING`, `MAXVALUE` / `NOMAXVALUE`, `MINVALUE` / `NOMINVALUE`, `CACHE` / `NOCACHE`, `ORDER` / `NOORDER`, `SCALE` / `NOSCALE`, `SHARD` / `NOSHARD`, and `SESSION` / `GLOBAL`.

Prefer one choice per semantic group:

```sql
CREATE SEQUENCE invoice_seq
    SHARING = METADATA
    START WITH 1
    CACHE 20
    NOORDER
    SCALE EXTEND
    GLOBAL;
```

## `oracle:no-conflicting-set-role-clauses`

Reports conflicting Oracle [`SET ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-ROLE.html) forms.
The rule checks mixed `ALL`, `ALL EXCEPT`, `NONE`, and explicit role-list forms in one statement.

Prefer one role mode:

```sql
SET ROLE ALL EXCEPT read_role;
```

## `oracle:no-conflicting-set-constraints-clauses`

Reports conflicting Oracle [`SET CONSTRAINTS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-CONSTRAINTS.html) timing clauses.
The rule checks repeated or opposing `IMMEDIATE` and `DEFERRED` timing choices in one statement.

Prefer one timing choice:

```sql
SET CONSTRAINTS ALL IMMEDIATE;
```

## `oracle:no-conflicting-set-transaction-clauses`

Reports conflicting Oracle [`SET TRANSACTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-TRANSACTION.html) clauses.
The rule checks repeated or opposing `READ ONLY` / `READ WRITE`, `ISOLATION LEVEL`, and `ROLLBACK SEGMENT` clause groups.

Prefer one choice per transaction property:

```sql
SET TRANSACTION READ ONLY ISOLATION LEVEL SERIALIZABLE;
```

## `oracle:no-conflicting-synonym-clauses`

Reports mutually exclusive or repeated Oracle [`CREATE SYNONYM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SYNONYM.html) clauses.
The rule checks static clause groups such as `EDITIONABLE` / `NONEDITIONABLE`, repeated `PUBLIC`, and repeated `SHARING = METADATA | NONE`.

Prefer one choice per semantic group:

```sql
CREATE OR REPLACE EDITIONABLE PUBLIC SYNONYM customer_syn
    SHARING = METADATA
    FOR app.customer;
```

## `oracle:no-conflicting-table-clauses`

Reports mutually exclusive Oracle [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) and [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) clauses in one statement.
The rule checks static statement-level clause groups such as `LOGGING` / `NOLOGGING`, `CACHE` / `NOCACHE`, `COMPRESS` / `NOCOMPRESS`, and `READ ONLY` / `READ WRITE`.

Prefer one choice per semantic group:

```sql
ALTER TABLE customer READ WRITE;
```

## `oracle:no-conflicting-xmlschema-permissions`

Reports conflicting Oracle [`XMLSchema_spec`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/xmlschema_spec.html) permission clauses in XMLType table statements.
The rule checks multiple static `ALLOW` / `DISALLOW` choices for `NONSCHEMA` and `ANYSCHEMA`.

Prefer one XMLSchema permission choice:

```sql
CREATE TABLE xml_documents OF XMLTYPE
    XMLSCHEMA 'http://example.com/doc.xsd'
    ALLOW NONSCHEMA;
```

## `oracle:no-uppercase-rowid-column`

Reports quoted uppercase `"ROWID"` identifiers when they are used as Oracle column names.
Oracle's [database object naming rules](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Database-Object-Names-and-Qualifiers.html) treat uppercase `ROWID` as a special exception: it cannot be used as a column name even when quoted.

Prefer a different column name or mixed-case quoted spelling:

```sql
CREATE TABLE audit_event (
    row_id NUMBER(19) NOT NULL
);
```

## `oracle:prefer-identity-column`

Reports local sequence-trigger generated key patterns when the file also declares the sequence.
For Oracle versions that support identity columns, an identity column keeps generated key behavior on the table definition.

Prefer:

```sql
CREATE TABLE customer (
    id NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY
);
```

## `oracle:prefer-unified-auditing`

Reports traditional Oracle [`AUDIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Traditional-Auditing.html) and [`NOAUDIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Traditional-Auditing.html) statements.
Use unified auditing [`AUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Unified-Auditing.html) and [`NOAUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Unified-Auditing.html) forms for new databases.

Prefer:

```sql
CREATE AUDIT POLICY app_audit_policy
    ACTIONS SELECT ON customer;

AUDIT POLICY app_audit_policy BY app_user WHENEVER SUCCESSFUL;
```

## `oracle:require-number-precision`

Reports `NUMBER` declarations without numeric precision.
Oracle [`NUMBER(p[,s])`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html#GUID-75209AF6-476D-4C44-A5DC-5FA70D701B78) precision and scale affect the Kotlin type generated by the SQLDelight Oracle dialect, so table definitions should be explicit.

Prefer precision or precision and scale:

```sql
CREATE TABLE invoice (
    id NUMBER(19) NOT NULL,
    amount NUMBER(10, 2) NOT NULL
);
```

## `oracle:unsafe-ddl-migration`

Reports migration DDL that can rewrite, lock, or destructively change large Oracle tables.
The checked `ALTER TABLE` column forms are based on Oracle's [`column_clauses`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html#GUID-7D361BC5-2B09-4BBE-8D8E-1CBA20C5363C__BABGAABA) syntax.
The current checks cover:

- `TRUNCATE TABLE`
- `ALTER TABLE ... DROP COLUMN`
- `ALTER TABLE ... DROP COLUMNS`
- `ALTER TABLE ... SET UNUSED COLUMN`
- `ALTER TABLE ... SET UNUSED COLUMNS`
- `ALTER TABLE ... DROP UNUSED COLUMNS`
- `ALTER TABLE ... DROP/TRUNCATE/MOVE/MERGE/SPLIT/EXCHANGE PARTITION`
- `ALTER TABLE ... DROP/TRUNCATE/MOVE/MERGE/SPLIT/EXCHANGE SUBPARTITION`
- `ALTER TABLE ... MOVE`
- `ALTER TABLE ... SHRINK SPACE`
- `ALTER TABLE ... ADD ... NOT NULL` without a `DEFAULT`
- `ALTER TABLE ... MODIFY ... NOT NULL` without a `DEFAULT`

The rule is intentionally conservative. Review the migration plan, online DDL options, backfill strategy, and deployment sequencing before suppressing the warning.

## `oracle:valid-audit-policy-form`

Reports conflicting static Oracle unified auditing policy clauses.
The rule checks [`AUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Unified-Auditing.html) and [`NOAUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Unified-Auditing.html) statements for local contradictions such as `BY` with `EXCEPT`, or repeated `WHENEVER` success filters.

Prefer one target clause and one success filter:

```sql
AUDIT POLICY app_policy BY hr WHENEVER SUCCESSFUL;
```

## `oracle:valid-boolean-test-condition`

Reports statically invalid Oracle [`boolean test conditions`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Conditions.html).
The rule checks malformed double-negative forms such as `IS NOT NOT TRUE`, `IS NOT NOT FALSE`, and `IS NOT NOT UNKNOWN`.

Prefer the documented boolean test forms:

```sql
SELECT *
FROM feature_flags
WHERE enabled IS NOT FALSE;
```

## `oracle:valid-nls-parameter`

Reports statically invalid Oracle NLS parameter literals in conversion functions.
Oracle documents the datetime `nlsparam` as `NLS_DATE_LANGUAGE = language` for [`TO_DATE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_DATE.html), [`TO_TIMESTAMP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_TIMESTAMP.html), and [`TO_TIMESTAMP_TZ`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_TIMESTAMP_TZ.html).
Number conversion functions accept number NLS parameters such as `NLS_NUMERIC_CHARACTERS`, `NLS_CURRENCY`, and `NLS_ISO_CURRENCY` in [`TO_NUMBER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_NUMBER.html), [`TO_BINARY_FLOAT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_BINARY_FLOAT.html), [`TO_BINARY_DOUBLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_BINARY_DOUBLE.html), and number [`TO_CHAR`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_CHAR-number.html).

Prefer documented static NLS parameters:

```sql
SELECT TO_DATE(created_text, 'Month DD, YYYY', 'NLS_DATE_LANGUAGE = American'),
       TO_NUMBER(amount_text, 'L9G999D99', 'NLS_NUMERIC_CHARACTERS = '',.'' NLS_CURRENCY = ''AusDollars''')
FROM invoice;
```

## `oracle:valid-json-condition-options`

Reports conflicting static SQL/JSON condition options.
The rule checks documented option groups in Oracle [`IS JSON`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SQL-JSON-Conditions.html), [`JSON_EXISTS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SQL-JSON-Conditions.html), [`JSON_EQUAL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SQL-JSON-Conditions.html), and [`JSON_TEXTCONTAINS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SQL-JSON-Conditions.html) conditions, including `STRICT` / `LAX`, `WITH UNIQUE KEYS` / `WITHOUT UNIQUE KEYS`, and repeated `ON ERROR` / `ON EMPTY` clauses.

Prefer one option from each group:

```sql
SELECT *
FROM document_store
WHERE payload IS JSON STRICT WITH UNIQUE KEYS
  AND JSON_EXISTS(payload, '$.items[*]' TRUE ON ERROR FALSE ON EMPTY);
```

## `oracle:valid-returning-clause`

Reports statically invalid Oracle [`returning_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/returning_clause.html) forms in `INSERT`, `UPDATE`, `DELETE`, and `MERGE` statements.
The rule checks repeated `RETURN` / `RETURNING` keywords, repeated `INTO` targets in one returning clause, and mixed `OLD` / `NEW` returning expressions.

Prefer one returning clause with one target list:

```sql
UPDATE customer
SET name = :name
WHERE id = :id
RETURNING id INTO :updated_id;
```

## `oracle:valid-row-limiting-clause`

Reports statically invalid Oracle [`row_limiting_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) forms.
The rule checks static `OFFSET` and `FETCH FIRST` / `FETCH NEXT` numeric literals, `PERCENT` range literals, and `FETCH ... WITH TIES` without an `ORDER BY`.

Prefer positive row counts, percentages from 0 through 100, and deterministic ordering with ties:

```sql
SELECT *
FROM orders
ORDER BY created_at
OFFSET 10 ROWS FETCH NEXT 25 ROWS WITH TIES;
```

## `oracle:valid-like-escape`

Reports static Oracle pattern-matching `ESCAPE` literals that are not exactly one character.
Oracle [`LIKE`, `LIKEC`, `LIKE2`, and `LIKE4`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Pattern-matching-Conditions.html) conditions use a single-character escape expression to treat wildcard characters literally.

Prefer a one-character static escape literal:

```sql
SELECT *
FROM customer
WHERE name LIKE 'A\_%' ESCAPE '\';
```

## `oracle:valid-lock-table-wait-clause`

Reports statically invalid Oracle [`LOCK TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html) wait clauses.
The rule checks conflicting `NOWAIT` / `WAIT` clauses, repeated wait clauses, and non-numeric static `WAIT` values.

Prefer one lock wait behavior:

```sql
LOCK TABLE customer IN EXCLUSIVE MODE WAIT 5;
```

## `oracle:valid-outer-join-operator`

Reports legacy Oracle outer join operator `(+)` usages in statically invalid `OR` or `IN` conditions.
Oracle documents these restrictions in the [`Joins`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Joins.html) reference and recommends ANSI outer joins for clearer semantics.

Prefer ANSI joins:

```sql
SELECT *
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id;
```

## `oracle:valid-segment-creation-clause`

Reports duplicate Oracle [`deferred_segment_creation`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/deferred_segment_creation.html) clauses in one table statement.

Prefer one segment creation choice:

```sql
CREATE TABLE customer (
    id NUMBER(19) NOT NULL
)
SEGMENT CREATION DEFERRED;
```

## `oracle:valid-subquery-restriction-clause`

Reports multiple Oracle [`subquery_restriction_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/subquery_restriction_clause.html) forms in one subquery/table reference.

Prefer one restriction clause:

```sql
SELECT *
FROM (SELECT * FROM orders WITH READ ONLY) o;
```

## Notes

The Oracle rules are source-text checks built for SQLDelight projects.
They avoid requiring a live database and rely on sqldelight-check's resolved dialect metadata to activate only for Oracle databases.
