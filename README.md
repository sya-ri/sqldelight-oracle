# sqldelight-oracle

Oracle Database support for SQLDelight and sqldelight-check.

Published artifact plan:

- `dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule`

## Modules

- `sqldelight-oracle-dialect`: SQLDelight dialect artifact.
- `sqldelight-check-oracle-dialect`: sqldelight-check dialect metadata for Oracle source scanning.
- `sqldelight-check-oracle-rule`: Oracle-specific sqldelight-check rules.

## Oracle Syntax Coverage

Oracle syntax coverage is tracked in [docs/oracle-syntax.md](docs/oracle-syntax.md).

The baseline reference is Oracle AI Database 26ai SQL Language Reference:

- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html

## Verification

Unit tests use Kotest `FunSpec`. Oracle read/write verification should use Testcontainers with the Oracle XE module
when a real database is required.
