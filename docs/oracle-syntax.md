# Oracle SQL Syntax Coverage

Baseline: [Oracle AI Database 26ai SQL Language Reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html), May 2026.

This checklist tracks parser/source-scanner/rule support. Check an item only after it has targeted FunSpec tests.

## Queries

- [x] [`SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) source-scanner statement and major clause boundaries
- [x] [`hierarchical_query_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `START WITH`, `CONNECT BY`
- [x] [`row_limiting_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `OFFSET`, `FETCH FIRST`, `FETCH NEXT`
- [x] [`pivot_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) / [`unpivot_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`model_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`row_pattern_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) / [`MATCH_RECOGNIZE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`qualify_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`flashback_query_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `AS OF SCN`, `AS OF TIMESTAMP`, `VERSIONS BETWEEN`
- [x] [`JSON_TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/JSON_TABLE.html)
- [x] [`XMLTABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/XMLTABLE.html)
- [x] [Analytic view](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ANALYTIC-VIEW.html) query syntax source-scanner boundaries
- [x] [SQL/PGQ graph query](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph_table-operator.html) syntax source-scanner boundaries

## DML

- [x] [`MERGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html) statement start
- [x] [`RETURNING`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html) clause boundary
- [x] [`INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- [x] [Multi-table `INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- [x] [`UPDATE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/UPDATE.html)
- [x] [`DELETE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DELETE.html)
- [x] [`CALL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CALL.html)
- [x] [`LOCK TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html)
- [x] [`EXPLAIN PLAN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/EXPLAIN-PLAN.html)

## DDL

- [x] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) type names and statement baseline
- [x] [`CREATE SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SEQUENCE.html)
- [x] [`CREATE MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW.html)
- [x] [`CREATE MATERIALIZED VIEW LOG`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW-LOG.html)
- [x] [`CREATE SYNONYM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SYNONYM.html)
- [x] [`CREATE TRIGGER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TRIGGER.html)
- [x] [`CREATE TYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TYPE.html)
- [x] [`CREATE PACKAGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PACKAGE.html)
- [x] [`CREATE PROCEDURE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PROCEDURE.html)
- [x] [`CREATE DIRECTORY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DIRECTORY.html)
- [x] [`CREATE DATABASE LINK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DATABASE-LINK.html)
- [x] [`CREATE CLUSTER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-CLUSTER.html)
- [x] [`CREATE INDEXTYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEXTYPE.html)
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html)
- [x] [`ALTER INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEX.html)
- [x] [`ALTER SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SEQUENCE.html)
- [x] [`ALTER VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-VIEW.html)
- [x] [`ALTER MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW.html)
- [x] [`DROP TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLE.html)
- [x] [`DROP INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INDEX.html)
- [x] [`DROP SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-SEQUENCE.html)
- [x] [`DROP VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-VIEW.html)
- [x] [`DROP MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-MATERIALIZED-VIEW.html)
- [x] [`TRUNCATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html)
- [x] [`RENAME`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/RENAME.html)
- [x] [`COMMENT ON`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMENT.html)

## Transactions And Session Statements

- [x] [`ALTER SESSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SESSION.html)
- [x] [`ALTER SYSTEM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYSTEM.html)
- [x] [`COMMIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMIT.html)
- [x] [`ROLLBACK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ROLLBACK.html)
- [x] [`SAVEPOINT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SAVEPOINT.html)
- [x] [`SET TRANSACTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-TRANSACTION.html)
- [x] [`SET CONSTRAINTS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-CONSTRAINTS.html)

## Security And Privileges

- [x] [`GRANT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GRANT.html)
- [x] [`REVOKE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/REVOKE.html)
- [x] [`CREATE USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-USER.html)
- [x] [`ALTER USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-USER.html)
- [x] [`DROP USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-USER.html)
- [x] [`CREATE ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLE.html)
- [x] [`ALTER ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLE.html)
- [x] [`DROP ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ROLE.html)

## Data Types

- [x] [Character types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `CHAR`, `NCHAR`, `VARCHAR2`, `NVARCHAR2`
- [x] [Numeric types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `NUMBER`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- [x] [Date/time types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `DATE`, `TIMESTAMP`, `INTERVAL`
- [x] [LOB and raw types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `BLOB`, `CLOB`, `NCLOB`, `BFILE`, `RAW`, `LONG RAW`
- [x] [Row id types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `ROWID`, `UROWID`
- [x] [JSON/XML/spatial/vector names](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `JSON`, `XMLTYPE`, `SDO_GEOMETRY`, `VECTOR`
- [x] [Object/reference type names](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `OBJECT`, `REF`, `ANYDATA`
- [x] [Collection types and nested tables](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [x] Data use case domains: [`CREATE DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html), [`ALTER DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-domain.html), [`DROP DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-domain.html)
- [x] SQLDelight type resolver mapping for Oracle scalar data types, including `NUMBER` precision/scale, ANSI character/numeric aliases, `BINARY_INTEGER`, `PLS_INTEGER`, `DATE`, `TIMESTAMP`, text, binary, JSON/XML, spatial, collection, URI, and vector names
- [x] SQLDelight function resolver mapping for deterministic Oracle functions such as `SYSDATE`, `SYSTIMESTAMP`, `TO_CHAR`, `TO_NUMBER`, `HEXTORAW`, JSON/XML constructors, analytic rank functions, and numeric math functions
- [x] SQLDelight argument-dependent function resolver mapping for [`COALESCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COALESCE.html), [`NVL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL.html), [`NVL2`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL2.html), [`GREATEST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GREATEST.html), [`LEAST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LEAST.html), [`MAX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MAX.html), [`MIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MIN.html), and [`SUM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SUM.html)
- [x] sqldelight-check source scanner data type and common function patterns synchronized with SQLDelight Oracle type and function resolver coverage

## Rules

- [x] `oracle:no-empty-string-comparison` for Oracle [null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html)
- [x] Prefer Oracle [`identity_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) columns over sequence-trigger pairs
- [x] Flag nullable [`NOT IN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IN-Condition.html) predicates where [Oracle null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html) are likely unintended
- [x] Flag unsafe [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) / [`TRUNCATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html) migration DDL that rewrites, locks, or destructively changes large tables
- [x] Require explicit precision for [`NUMBER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html) where generated Kotlin type would be ambiguous

## Database Verification

- [x] Gated Testcontainers smoke test for connection, DDL, and DML execution
- [x] Testcontainers smoke test executed against local Docker with `gvenzl/oracle-free:23-slim-faststart` and `ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1`
- [x] Gated Testcontainers DDL round-trip test for representative supported type names
- [x] Gated Testcontainers DML round-trip test for `INSERT`, `UPDATE`, `DELETE`, `MERGE`
