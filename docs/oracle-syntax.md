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
- [ ] Analytic view query syntax
- [ ] SQL/PGQ graph query syntax

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
- [ ] Data use case domains

## Rules

- [x] `oracle:no-empty-string-comparison`
- [ ] Prefer identity columns over sequence-trigger pairs
- [x] Flag nullable [`NOT IN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IN-Condition.html) predicates where [Oracle null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html) are likely unintended
- [ ] Flag unsafe DDL in migrations that rewrites or locks large tables
- [x] Require explicit precision for `NUMBER` where generated Kotlin type would be ambiguous

## Database Verification

- [x] Gated Testcontainers smoke test for connection, DDL, and DML execution
- [ ] Testcontainers smoke test executed against local Docker
- [ ] Testcontainers DDL round-trip for supported type names
- [ ] Testcontainers DML round-trip for `INSERT`, `UPDATE`, `DELETE`, `MERGE`
