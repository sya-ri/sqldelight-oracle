# SQLDelight Dialect Implementation Notes

Baseline: SQLDelight `2.3.2` and sql-psi `0.7.3`.

This document tracks what `sqldelight-oracle-dialect` needs in order to behave like a real SQLDelight dialect artifact, not only a type resolver.

## Reference Implementation

- [SQLDelight SQLite 3.38 dialect source](https://github.com/sqldelight/sqldelight/tree/2.3.2/dialects/sqlite-3-38-dialect)
- [SQLDelight dialect API source](https://github.com/sqldelight/sqldelight/tree/2.3.2/dialect-api)
- [SQLDelight core grammar](https://github.com/sqldelight/sqldelight/blob/2.3.2/sqldelight-compiler/src/main/kotlin/app/cash/sqldelight/core/sqldelight.bnf)

The SQLite dialect artifact includes:

- generated parser and PSI classes
- a source BNF file
- a generated `*_gen.bnf` file
- a parser utility object that resets and installs parser overrides
- a `SqlDelightDialect.setup()` implementation that installs those overrides
- a dialect-specific `TypeResolver`
- dialect entry point and type resolver classes in the dialect package root, with generated parser classes under `grammar`
- a `META-INF/services/app.cash.sqldelight.dialect.api.SqlDelightDialect` provider

## Current Oracle State

- [x] ServiceLoader provider for `SqlDelightDialect`
- [x] JDBC runtime type mapping
- [x] Oracle scalar type mapping
- [x] Oracle function return type mapping
- [x] Argument-dependent function return type mapping
- [x] Initial Oracle `type_name` parser override hook
- [x] Oracle BNF source file
- [x] Generated Oracle parser and PSI classes via `app.cash.grammarkit-composer`
- [x] Generated Oracle parser utility object
- [x] `setup()` installs parser overrides
- [x] Public package layout follows the official dialect shape: `dev.s7a.sqldelight.oracle.dialects.oracle` and `dev.s7a.sqldelight.oracle.dialects.oracle.grammar`
- [x] Parser-backed SQLDelight tests for Oracle `.sq` files

## Parser Override Targets

The sql-psi core grammar is SQLite-shaped and exposes parser hooks through `SqlParserUtil`.
Oracle should add BNF overrides incrementally, with exact parser tests for each supported query.

- [x] `type_name`: initial hook for Oracle multi-word and parameterized data types such as `TIMESTAMP WITH TIME ZONE`, `INTERVAL YEAR TO MONTH`, `DOUBLE PRECISION`, `LONG RAW`, `NATIONAL CHARACTER VARYING`
- [x] `column_constraint`: Oracle `identity_clause`, inline `ENABLE` / `DISABLE`, `DEFERRABLE`, `INITIALLY`, `RELY`, `NOVALIDATE`, `VISIBLE`, `INVISIBLE`
- [x] `table_constraint`: Oracle constraint states for table-level `PRIMARY KEY`, `UNIQUE`, `CHECK`, and `FOREIGN KEY`
- [ ] `create_table_stmt`: Oracle relational table clauses, object table clauses, temporary tables, external tables, blockchain/immutable table clauses, and `CREATE TABLE AS SELECT`
- [x] `create_table_stmt` baseline: Oracle `IF NOT EXISTS`, temporary table scopes, `ON COMMIT`, and `CREATE TABLE AS SELECT`
- [ ] `alter_table_stmt`: Oracle add/modify/drop/rename column and constraint operations
- [x] `alter_table_stmt` baseline: official SQLDelight-style `ALTER TABLE ... ADD column` parser/stub support
- [x] `alter_table_stmt` single-operation support: `ADD` table constraint, `MODIFY` column, `DROP COLUMN`, `DROP CONSTRAINT`, and `RENAME COLUMN`
- [x] `alter_table_stmt` multiple-column support: Oracle `ADD (...)` and `MODIFY (...)`
- [ ] `create_index_stmt`: Oracle bitmap, unique, function-based, domain, reverse, invisible, partial, and local/global index clauses
- [x] `create_index_stmt` baseline: Oracle `UNIQUE`, `BITMAP`, `IF NOT EXISTS`, ordinary/function-based indexed columns, visibility, `ONLINE`, `REVERSE`, logging, compression, and `INDEXING FULL/PARTIAL`
- [x] `create_view_stmt` baseline: Oracle `OR REPLACE`, `FORCE` / `NO FORCE`, editioning keywords, `BEQUEATH`, `WITH CHECK OPTION`, and `WITH READ ONLY`
- [x] `extension_stmt` baseline: Oracle `CREATE SEQUENCE` parser support for sequence options that are not represented by core schema contributor statements
- [ ] `select_stmt`: Oracle `hierarchical_query_clause`, `flashback_query_clause`, `pivot_clause`, `unpivot_clause`, `model_clause`, `row_pattern_clause`, `qualify_clause`, and row limiting
- [ ] `table_or_subquery`: Oracle `JSON_TABLE`, `XMLTABLE`, `GRAPH_TABLE`, `NESTED`, `LATERAL`, flashback table syntax, and partition extension syntax
- [ ] `result_column`: Oracle aliases, `JSON`/`XML` returning clauses where expressions expose column names
- [ ] `function_expr`: Oracle aggregate, analytic, `WITHIN GROUP`, `KEEP`, `OVER`, `FILTER`, `RETURNING`, and special argument separators
- [ ] `insert_stmt`: Oracle single-table insert, multi-table insert, direct-path hints, subquery insert, and `RETURNING`
- [ ] `update_stmt`: Oracle update extensions including subquery assignment, `RETURNING`, partition extension clauses, and DML error logging
- [ ] `delete_stmt`: Oracle delete extensions including `RETURNING`, partition extension clauses, and DML error logging
- [ ] `extension_stmt`: Oracle statements not represented by the core grammar, including `MERGE`, `CALL`, transaction/session/security statements, and non-table DDL
- [ ] `extension_expr`: Oracle expression extensions such as `PRIOR`, `CURSOR`, `TREAT`, `XMLQUERY`, `JSON_EXISTS`, `JSON_VALUE`, vector operators, and legacy outer join `(+)`

## Build Work

- [x] Add the official SQLDelight `app.cash.grammarkit-composer` generation path
- [x] Include generated parser/PSI sources in the compiled and published artifact
- [x] Keep generated files deterministic and reproducible from `src/main/kotlin/**/grammar/*.bnf`
- [x] Add tests that fail when `setup()` does not install Oracle parser overrides
- [x] Add parser tests using exact expected success/failure results for representative Oracle `.sq` files
