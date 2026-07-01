# sqldelight-check oracle dialect

`sqldelight-check-oracle-dialect` provides Oracle Database dialect metadata for sqldelight-check.

sqldelight-check loads this artifact through Java `ServiceLoader` from the `sqldelightCheckDialects` Gradle configuration.
The provider resolves SQLDelight databases that use:

```text
dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect
```

## Install

Add this artifact to the SQLDelight project checked by sqldelight-check:

```kotlin
dependencies {
    sqldelightCheckDialects("dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect:0.1.3")
}
```

## Dialect ID

The dialect metadata exposes the `oracle` dialect ID:

```kotlin
sqldelightCheck {
    databases {
        database("Database") {
            ruleSets {
                oracle {
                    enabled.set(true)
                }
            }
        }
    }
}
```

## Source Scanner Coverage

The source scanner patterns include Oracle-specific statement starts, clause boundaries, transaction statements, data type names, pseudocolumns, common function names, DML `RETURNING` and `WAIT` forms, row limiting clauses, and quoted or alternative-quoted literal-safe scan metadata.
They are conservative source-text metadata for sqldelight-check rules; SQLDelight remains responsible for parsing concrete `.sq` and `.sqm` files.

## ServiceLoader

The artifact publishes:

```text
META-INF/services/dev.s7a.sqldelight.check.api.SqlDialectProvider
```

with:

```text
dev.s7a.sqldelight.oracle.check.dialect.OracleDialectProvider
```
