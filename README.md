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

Apply sqldelight-check and add Oracle metadata and rules to the checked project:

```kotlin
plugins {
    id("dev.s7a.sqldelight.check") version "0.3.0"
}

dependencies {
    sqldelightCheckDialects("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect:0.1.0")
    sqldelightCheckRuleSet("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule:0.1.0")
}
```

Run the sqldelight-check tasks from the SQLDelight project:

```shell
./gradlew sqldelightCheck
./gradlew sqldelightFix
```

## Oracle Syntax Coverage

Oracle syntax coverage is tracked in [docs/oracle-syntax.md](docs/oracle-syntax.md).
The baseline reference is [Oracle AI Database 26ai SQL Language Reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html).

## License

MIT License. See [LICENSE](LICENSE).

## Contributing

Development checks and Testcontainers notes are in [CONTRIBUTION.md](CONTRIBUTION.md).
