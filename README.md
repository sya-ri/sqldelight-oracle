# sqldelight-oracle

[![CI](https://github.com/sya-ri/sqldelight-oracle/actions/workflows/ci.yml/badge.svg)](https://github.com/sya-ri/sqldelight-oracle/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/sya-ri/sqldelight-oracle)](https://github.com/sya-ri/sqldelight-oracle/releases)
[![sqldelight-oracle-dialect](https://img.shields.io/maven-central/v/dev.s7a.sqldelight.oracle/sqldelight-oracle-dialect?label=sqldelight-oracle-dialect)](https://central.sonatype.com/artifact/dev.s7a.sqldelight.oracle/sqldelight-oracle-dialect)
[![sqldelight-check-oracle-dialect](https://img.shields.io/maven-central/v/dev.s7a.sqldelight.oracle/sqldelight-check-oracle-dialect?label=sqldelight-check-oracle-dialect)](https://central.sonatype.com/artifact/dev.s7a.sqldelight.oracle/sqldelight-check-oracle-dialect)
[![sqldelight-check-oracle-rule](https://img.shields.io/maven-central/v/dev.s7a.sqldelight.oracle/sqldelight-check-oracle-rule?label=sqldelight-check-oracle-rule)](https://central.sonatype.com/artifact/dev.s7a.sqldelight.oracle/sqldelight-check-oracle-rule)

Oracle Database support for SQLDelight and sqldelight-check.

Artifacts:

- `dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect`
- `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule`

The current release version is `0.1.2`.

Release notes are tracked in [CHANGELOG.md](CHANGELOG.md).

## Modules

- [`sqldelight-oracle-dialect`](sqldelight-oracle-dialect/README.md): SQLDelight dialect artifact.
- [`sqldelight-check-oracle-dialect`](sqldelight-check-oracle-dialect/README.md): sqldelight-check dialect metadata for Oracle source scanning.
- [`sqldelight-check-oracle-rule`](sqldelight-check-oracle-rule/README.md): Oracle-specific sqldelight-check rules.

## Install

Use the SQLDelight dialect in the project that owns `.sq` and `.sqm` files:

```kotlin
sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
            dialect("dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect:0.1.2")
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
    sqldelightCheckDialects("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect:0.1.2")
    sqldelightCheckRuleSet("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule:0.1.2")
}
```

Run the sqldelight-check tasks from the SQLDelight project:

```shell
./gradlew sqldelightCheck
./gradlew sqldelightFix
```

## License

MIT License. See [LICENSE](LICENSE).

## Contributing

Development checks and Testcontainers notes are in [CONTRIBUTING.md](CONTRIBUTING.md).
