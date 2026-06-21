# Oracle SQL Syntax Coverage

Baseline: [Oracle AI Database 26ai SQL Language Reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html), May 2026.

This checklist tracks parser/source-scanner/rule support. Check an item only after it has targeted FunSpec tests.

## Queries

- [x] [`SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) source-scanner statement and major clause boundaries
- [x] Hierarchical query clauses: `START WITH`, `CONNECT BY`
- [x] Row limiting clauses: `OFFSET`, `FETCH FIRST`, `FETCH NEXT`
- [x] `PIVOT` / `UNPIVOT`
- [x] `MODEL`
- [x] `MATCH_RECOGNIZE`
- [x] `QUALIFY`
- [x] Flashback query clauses: `AS OF SCN`, `AS OF TIMESTAMP`, `VERSIONS BETWEEN`
- [x] `JSON_TABLE`
- [x] `XMLTABLE`
- [x] [Analytic view](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ANALYTIC-VIEW.html) query syntax source-scanner boundaries
- [x] [SQL/PGQ graph query](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph_table-operator.html) syntax source-scanner boundaries

## DML

- [x] [`MERGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html) statement start
- [x] `RETURNING` clause boundary
- [x] `INSERT`
- [x] [Multi-table `INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- [x] `UPDATE`
- [x] `DELETE`
- [x] `CALL`
- [x] `LOCK TABLE`
- [x] `EXPLAIN PLAN`

## DDL

- [x] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) type names and statement baseline
- [x] `CREATE SEQUENCE`
- [x] `CREATE MATERIALIZED VIEW`
- [x] `CREATE MATERIALIZED VIEW LOG`
- [x] `CREATE SYNONYM`
- [x] `CREATE TRIGGER`
- [x] `CREATE TYPE`
- [x] `CREATE PACKAGE`
- [x] `CREATE PROCEDURE`
- [x] `CREATE DIRECTORY`
- [x] `CREATE DATABASE LINK`
- [x] `CREATE CLUSTER`
- [x] `CREATE INDEXTYPE`
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html)
- [x] [`ALTER INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEX.html)
- [x] [`ALTER SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SEQUENCE.html)
- [x] [`ALTER VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-VIEW.html)
- [x] [`ALTER MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW.html)
- [x] [`DROP TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLE.html)
- [x] [`DROP INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INDEX.html)
- [x] `DROP SEQUENCE`
- [x] `DROP VIEW`
- [x] `DROP MATERIALIZED VIEW`
- [x] `TRUNCATE TABLE`
- [x] `RENAME`
- [x] `COMMENT ON`

## Transactions And Session Statements

- [x] `ALTER SESSION`
- [x] `ALTER SYSTEM`
- [x] [`COMMIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMIT.html)
- [x] `ROLLBACK`
- [x] `SAVEPOINT`
- [x] `SET TRANSACTION`
- [x] `SET CONSTRAINT`

## Security And Privileges

- [x] `GRANT`
- [x] `REVOKE`
- [x] `CREATE USER`
- [x] `ALTER USER`
- [x] `DROP USER`
- [x] `CREATE ROLE`
- [x] `ALTER ROLE`
- [x] `DROP ROLE`

## Data Types

- [x] Character types: `CHAR`, `NCHAR`, `VARCHAR2`, `NVARCHAR2`
- [x] Numeric types: `NUMBER`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- [x] Date/time types: `DATE`, `TIMESTAMP`, `INTERVAL`
- [x] LOB and raw types: `BLOB`, `CLOB`, `NCLOB`, `BFILE`, `RAW`, `LONG RAW`
- [x] Row id types: `ROWID`, `UROWID`
- [x] JSON/XML/spatial/vector names: `JSON`, `XMLTYPE`, `SDO_GEOMETRY`, `VECTOR`
- [x] Object/reference type names: `OBJECT`, `REF`, `ANYDATA`
- [x] [Collection types and nested tables](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [x] Data use case domains: [`CREATE DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html), [`ALTER DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-domain.html), [`DROP DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-domain.html)
- [x] SQLDelight type resolver mapping for Oracle scalar data types, including `NUMBER` precision/scale, `DATE`, `TIMESTAMP`, text, binary, JSON/XML, spatial, and vector names
- [x] SQLDelight function resolver mapping for deterministic Oracle functions such as `SYSDATE`, `SYSTIMESTAMP`, `TO_CHAR`, `TO_NUMBER`, `HEXTORAW`, JSON/XML constructors, analytic rank functions, and numeric math functions
- [x] SQLDelight argument-dependent function resolver mapping for [`COALESCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COALESCE.html), [`NVL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL.html), [`NVL2`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL2.html), [`GREATEST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GREATEST.html), [`LEAST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LEAST.html), [`MAX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MAX.html), [`MIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MIN.html), and [`SUM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SUM.html)

## Rules

- [x] `oracle:no-empty-string-comparison`
- [x] Prefer Oracle [`identity_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) columns over sequence-trigger pairs
- [x] Flag nullable [`NOT IN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IN-Condition.html) predicates where [Oracle null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html) are likely unintended
- [x] Flag unsafe [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) / [`TRUNCATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html) migration DDL that rewrites, locks, or destructively changes large tables
- [x] Require explicit precision for `NUMBER` where generated Kotlin type would be ambiguous

## Database Verification

- [x] Gated Testcontainers smoke test for connection, DDL, and DML execution
- [x] Testcontainers smoke test executed against local Docker with `gvenzl/oracle-free:23-slim-faststart` and `ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1`
- [x] Gated Testcontainers DDL round-trip test for representative supported type names
- [x] Gated Testcontainers DML round-trip test for `INSERT`, `UPDATE`, `DELETE`, `MERGE`
