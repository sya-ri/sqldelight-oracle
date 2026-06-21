# sqldelight-oracle

Oracle Database support for SQLDelight and sqldelight-check.

Published artifact plan:

- `dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule`

The first planned release version is `0.1.0`.

## Modules

- `sqldelight-oracle-dialect`: SQLDelight dialect artifact.
- `sqldelight-check-oracle-dialect`: sqldelight-check dialect metadata for Oracle source scanning.
- `sqldelight-check-oracle-rule`: Oracle-specific sqldelight-check rules.

## Install

Use the SQLDelight dialect in the project that owns `.sq` and `.sqm` files:

```kotlin
sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
            dialect("dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect:0.1.0")
        }
    }
}
```

Add sqldelight-check Oracle metadata and rules to the checked project:

```kotlin
dependencies {
    sqldelightCheckDialects("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect:0.1.0")
    sqldelightCheckRuleSet("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule:0.1.0")
}
```

## Oracle Syntax Coverage

Oracle syntax coverage is tracked in [docs/oracle-syntax.md](docs/oracle-syntax.md).

The baseline reference is Oracle AI Database 26ai SQL Language Reference:

- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html
- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html

## Verification

Unit tests use Kotest `FunSpec`.

```shell
./gradlew check
./gradlew publishToMavenLocal
./gradlew releaseCheck
```

Oracle read/write verification uses Testcontainers with the Oracle XE module when a real database is required:

```shell
ORACLE_TESTCONTAINERS=true ./gradlew :sqldelight-oracle-dialect:test
```

Set `ORACLE_TESTCONTAINERS_IMAGE` to override the default `gvenzl/oracle-xe:21-slim-faststart` image.
For example, `ORACLE_TESTCONTAINERS_IMAGE=gvenzl/oracle-free:23-slim-faststart` runs the same gated tests against Oracle Free 23.
The container uses a 2 GiB `/dev/shm` by default; set `ORACLE_TESTCONTAINERS_SHM_BYTES` to override it.
