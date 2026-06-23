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
| [`oracle:no-conflicting-sequence-clauses`](#oracleno-conflicting-sequence-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE SEQUENCE` and `ALTER SEQUENCE` clauses. |
| [`oracle:no-conflicting-table-clauses`](#oracleno-conflicting-table-clauses) | Yes | Warning |  | Avoid mutually exclusive `CREATE TABLE` and `ALTER TABLE` clauses. |
| [`oracle:prefer-identity-column`](#oracleprefer-identity-column) | Yes | Warning |  | Prefer Oracle identity columns over sequence-trigger generated keys. |
| [`oracle:prefer-unified-auditing`](#oracleprefer-unified-auditing) | Yes | Warning |  | Prefer unified auditing over traditional `AUDIT` and `NOAUDIT` statements. |
| [`oracle:require-number-precision`](#oraclerequire-number-precision) | Yes | Warning |  | Require explicit precision and scale for `NUMBER` declarations. |
| [`oracle:unsafe-ddl-migration`](#oracleunsafe-ddl-migration) | Yes | Warning |  | Flag migration DDL that can rewrite, lock, or destructively change large tables. |

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

## `oracle:no-conflicting-table-clauses`

Reports mutually exclusive Oracle [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) and [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) clauses in one statement.
The rule checks static statement-level clause groups such as `LOGGING` / `NOLOGGING`, `CACHE` / `NOCACHE`, `COMPRESS` / `NOCOMPRESS`, and `READ ONLY` / `READ WRITE`.

Prefer one choice per semantic group:

```sql
ALTER TABLE customer READ WRITE;
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
- `ALTER TABLE ... MOVE`
- `ALTER TABLE ... SHRINK SPACE`
- `ALTER TABLE ... ADD ... NOT NULL` without a `DEFAULT`
- `ALTER TABLE ... MODIFY ... NOT NULL` without a `DEFAULT`

The rule is intentionally conservative. Review the migration plan, online DDL options, backfill strategy, and deployment sequencing before suppressing the warning.

## Notes

The Oracle rules are source-text checks built for SQLDelight projects.
They avoid requiring a live database and rely on sqldelight-check's resolved dialect metadata to activate only for Oracle databases.
