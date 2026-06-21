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
- [ ] Flashback query clauses
- [ ] `JSON_TABLE`
- [ ] `XMLTABLE`
- [ ] Analytic view query syntax
- [ ] SQL/PGQ graph query syntax

## DML

- [x] [`MERGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html) statement start
- [x] `RETURNING` clause boundary
- [ ] `INSERT`
- [ ] Multi-table `INSERT`
- [ ] `UPDATE`
- [ ] `DELETE`
- [ ] `CALL`
- [ ] `LOCK TABLE`
- [ ] `EXPLAIN PLAN`

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
- [ ] `ALTER TABLE`
- [ ] `ALTER INDEX`
- [ ] `ALTER SEQUENCE`
- [ ] `ALTER VIEW`
- [ ] `ALTER MATERIALIZED VIEW`
- [ ] `DROP TABLE`
- [ ] `DROP INDEX`
- [ ] `DROP SEQUENCE`
- [ ] `DROP VIEW`
- [ ] `DROP MATERIALIZED VIEW`
- [ ] `TRUNCATE TABLE`
- [ ] `RENAME`
- [ ] `COMMENT ON`

## Transactions And Session Statements

- [x] `ALTER SESSION`
- [x] `ALTER SYSTEM`
- [ ] `COMMIT`
- [ ] `ROLLBACK`
- [ ] `SAVEPOINT`
- [ ] `SET TRANSACTION`
- [ ] `SET CONSTRAINT`

## Security And Privileges

- [x] `GRANT`
- [x] `REVOKE`
- [ ] `CREATE USER`
- [ ] `ALTER USER`
- [ ] `DROP USER`
- [ ] `CREATE ROLE`
- [ ] `ALTER ROLE`
- [ ] `DROP ROLE`

## Data Types

- [x] Character types: `CHAR`, `NCHAR`, `VARCHAR2`, `NVARCHAR2`
- [x] Numeric types: `NUMBER`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- [x] Date/time types: `DATE`, `TIMESTAMP`, `INTERVAL`
- [x] LOB and raw types: `BLOB`, `CLOB`, `NCLOB`, `BFILE`, `RAW`, `LONG RAW`
- [x] Row id types: `ROWID`, `UROWID`
- [x] JSON/XML/spatial/vector names: `JSON`, `XMLTYPE`, `SDO_GEOMETRY`, `VECTOR`
- [ ] Object types and collection types
- [ ] Data use case domains

## Rules

- [x] `oracle:no-empty-string-comparison`
- [ ] Prefer identity columns over sequence-trigger pairs
- [ ] Flag nullable `NOT IN` predicates where Oracle null semantics are likely unintended
- [ ] Flag unsafe DDL in migrations that rewrites or locks large tables
- [ ] Require explicit precision for `NUMBER` where generated Kotlin type would be ambiguous

## Database Verification

- [ ] Testcontainers smoke test for connection and simple query execution
- [ ] Testcontainers DDL round-trip for supported type names
- [ ] Testcontainers DML round-trip for `INSERT`, `UPDATE`, `DELETE`, `MERGE`
