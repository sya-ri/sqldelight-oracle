# Changelog

## 0.1.1

### Added

- Support `?` and `:name` bind parameters in Oracle SQL.
- Expand parser coverage for Oracle JSON, XML, LISTAGG, vector, conversion, DML extension, model, match recognize, inline PL/SQL, hierarchical query, and drop table syntax.
- Resolve additional Oracle query sources and generated columns, including synonyms, merge aliases, CTE search/cycle columns, view and materialized view column aliases, collection table values, XMLTable column values, pivot/unpivot sources, graph table columns, inline external columns, and table function timestamp columns.
- Resolve additional Oracle expression types for datetime, conversion, JSON generation, JSON mergepatch, JSON transform, and extract/round/trunc functions.
- Add parser-backed Oracle SQL regression coverage.
- Add release branch and pull request publishing workflows.

### Fixed

- Deduplicate generated column query result handling and Oracle string parsing helpers to satisfy Qodana duplicate-code checks.

## 0.1.0

### Added

- Initial release of Oracle Database support for SQLDelight and sqldelight-check.
- Publish `dev.s7a.sqldelight.oracle:sqldelight-oracle-dialect`.
- Publish `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-dialect`.
- Publish `dev.s7a.sqldelight.oracle:sqldelight-check-oracle-rule`.
- Support Oracle SQL parsing for the documented syntax in `sqldelight-oracle-dialect/README.md`.
- Provide Oracle-specific sqldelight-check rules for common dialect mistakes.
