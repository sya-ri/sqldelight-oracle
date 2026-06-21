# sqldelight oracle dialect

`sqldelight-oracle-dialect` is the SQLDelight dialect artifact for Oracle Database.

It provides the SQLDelight dialect entry point, JDBC runtime types, Oracle type resolution, and Oracle function return type mapping.

## Install

Use the dialect in the Gradle project that owns `.sq` and `.sqm` files:

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

## Type Mapping

The dialect maps Oracle scalar type names to SQLDelight Kotlin types, including:

- `NUMBER` precision and scale
- `BINARY_FLOAT` and `BINARY_DOUBLE`
- ANSI character and numeric aliases
- `DATE`, `TIMESTAMP`, and timestamp with time zone names
- text, binary, JSON, XML, spatial, collection, URI, and vector type names

Function return type mapping covers deterministic Oracle functions such as `SYSDATE`, `SYSTIMESTAMP`, `TO_CHAR`, `TO_NUMBER`, JSON/XML constructors, UUID helpers, vector constructors and distance functions, domain predicates, analytic rank functions, numeric math functions, and argument-dependent functions such as `COALESCE`, `NVL`, `NVL2`, `GREATEST`, `LEAST`, `MAX`, `MIN`, and `SUM`.

Coverage is tracked in [../docs/oracle-syntax.md](../docs/oracle-syntax.md).

## Runtime

The dialect uses SQLDelight JDBC runtime types:

```text
app.cash.sqldelight.driver.jdbc.JdbcCursor
app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
```

## ServiceLoader

The artifact publishes:

```text
META-INF/services/app.cash.sqldelight.dialect.api.SqlDelightDialect
```

with:

```text
dev.s7a.sqldelight.oracle.dialect.OracleDialect
```
