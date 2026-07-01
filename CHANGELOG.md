# Changelog

## 0.1.3

### Fixed

- Fix SQLDelight custom type columns with adjacent comments so commented `AS <KotlinType>` column definitions compile correctly.
- Fix filtered queries that reference `AS <KotlinType>` columns in predicates, including literal comparisons, bind parameters, `IS NULL`, and `COALESCE`.

## 0.1.2

### Added

- Expand Oracle result-column resolution for functions, casts, literals, pseudocolumns, sequence pseudocolumns, numeric and concatenation operators, datetime arithmetic, grouping functions, aggregates, pivot/unpivot, row pattern measures, collection tables, XMLTABLE, JSON_TABLE, inline external tables, `CONTAINERS` / `SHARDS`, `VALUES` table references, object pseudocolumns, `AT TIME ZONE`, `CASE` expressions, and typed null casts.
- Propagate Oracle result nullability for scalar, conditional, aggregate, numeric, datetime, conversion, vector, XML, JSON, RAW, NLS metadata, charset declaration, `CASE`, empty string, SQL/JSON, XML, and BFILE expressions.
- Add parser coverage for analytic null treatment clauses, hypothetical rank aggregates, cast multiset subqueries, bulk collect returning clauses, `OLD` / `NEW` returning expressions, `CHR` character set clauses, drop unused columns, drop column variants, CTAS column aliases, quoted table aliases, current datetime precision, JSON_TABLE default column types, and Oracle national literals in `VALUES` tables.
- Add sqldelight-check validation for Oracle function arity, no-parentheses expressions, `CONCAT`, aggregate, statistical, vector, XML, JSON, utility, domain, metadata, calendar, collection conversion functions, pseudocolumn parentheses, row limiting, `RETURNING` clauses, CTAS/view alias lists, `WAIT` clauses, unsafe DDL, outer join restrictions, JSON condition/table options, empty-string predicates, `NOT IN` nullable filters, NUMBER conversion clauses, and quoted or alternative-quoted literal forms.
- Add statement and clause scoping for Oracle sqldelight-check rules so checks respect labels, nested subqueries, table clauses, constraint states, identity triggers, annotations, set role clauses, and qualified or aliased references.

### Fixed

- Fix Oracle type and column resolution for `NVL`, `NVL2`, `DECODE`, `CONCAT`, current-user expressions, pivot aliases and quoted pivot values, `FLOAT`, numeric math and distribution functions, vector functions, JSON/XML expressions, XMLROOT/XMLQUERY/XMLSERIALIZE, `TREAT`, JSON id operators, hierarchical operators, and nullable `VALUES` columns.
- Fix Qodana findings by excluding generated build outputs from Qodana and simplifying duplicated scanner helpers and generated-column resolution helpers.

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
