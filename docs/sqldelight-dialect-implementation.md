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
- [x] Public package and file layout follows the official [`dialects/<name>`](https://github.com/sqldelight/sqldelight/tree/master/dialects) shape: `dev.s7a.sqldelight.oracle.dialects.oracle`, `grammar/Oracle.bnf`, and `grammar/mixins`
- [x] Parser-backed SQLDelight tests for Oracle `.sq` files
- [ ] Lexer-level Oracle tokens: SQL block comments and optimizer hints (`/* ... */`, `/*+ ... */`), model cell brackets, alternative quoted literals, and other Oracle delimiters need a SQLDelight lexer extension point or upstream lexer support; `SqlParserDefinition` currently creates the core `SqlLexerAdapter`

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
- [x] `create_index_stmt` baseline: Oracle `UNIQUE`, `BITMAP`, `MULTIVALUE`, `VECTOR`, `HYBRID VECTOR`, `IF NOT EXISTS`, ordinary/function-based indexed columns, domain/XMLIndex/cluster/bitmap join index variants, local/global partition boundaries, ILM boundaries, visibility, vector organization/parameters, `ONLINE`, `LOCAL`, `REVERSE`, logging, compression, `TABLESPACE`, `PARALLEL`, and `INDEXING FULL/PARTIAL`
- [x] `create_view_stmt` baseline: Oracle `OR REPLACE`, `FORCE` / `NO FORCE`, editioning keywords, `IF NOT EXISTS`, `JSON COLLECTION`, `SHARING`, object/XMLType view clauses, view column constraint boundaries, `DEFAULT COLLATION`, `BEQUEATH`, `WITH CHECK OPTION`, `WITH READ ONLY`, `CONTAINER_MAP`, and `CONTAINERS_DEFAULT`
- [x] `drop_trigger_stmt` baseline: Oracle `IF EXISTS` and schema-qualified trigger names
- [x] `commit_stmt` / `rollback_stmt` / `savepoint_stmt`: Oracle `WORK`, `COMMENT`, `WRITE`, `FORCE`, and `TO SAVEPOINT` parser overrides while keeping core SQLDelight PSI interfaces
- [x] `stmt` / `extension_stmt` baseline: Oracle `CREATE SEQUENCE`, `ALTER SEQUENCE`, `DROP SEQUENCE`, PL/SQL create/alter body boundaries, analytic view/attribute dimension/hierarchy/dimension creation, materialized zonemap creation, property graph creation, JSON relational duality view creation, Iceberg table creation, operator/outline creation, profile/schema/flashback archive creation, context creation, MLE environment/module creation, assertion/directive/edition/In-Memory join group/Java/logical partition tracking creation, database/controlfile/pluggable database/tablespace/tablespace set/diskgroup/parameter file/true cache creation, lockdown profile/PMEM filestore/restore point/rollback segment creation, materialized view log and zonemap alteration, analytic view/attribute dimension/hierarchy/dimension alteration, cluster/database-link/indextype/operator/outline alteration, MLE environment/module alteration, property graph and JSON duality view alteration, assertion/directive/flashback archive/In-Memory join group/lockdown profile alteration, PMEM filestore/profile/resource cost/rollback segment alteration, database/dictionary/pluggable database alteration, tablespace/tablespace set/diskgroup alteration, materialized/analytic/object/schema/graph/MLE/security/administrative and legacy drops, synonym/type/PLSQL/Java drops, user/role/grant/revoke/key-management/unified-audit security statements, `ALTER SESSION`, `ALTER SYSTEM`, `SET TRANSACTION`, `SET CONSTRAINTS`, `SET ROLE`, `SET USE DATA GRANTS ONLY`, `ANALYZE`, `ASSOCIATE STATISTICS`, `DISASSOCIATE STATISTICS`, `RENAME`, `COMMENT ON`, `PURGE`, `FLASHBACK`, `MERGE`, `CALL`, `LOCK TABLE`, `EXPLAIN PLAN`, `TRUNCATE TABLE`, and `TRUNCATE CLUSTER` parser support for statements that are not represented by core schema contributor/query statements
- [ ] `select_stmt`: targeted parser coverage is staged for `hierarchical_query_clause`, representative `flashback_query_clause`, representative `pivot_clause` / `unpivot_clause`, representative `row_pattern_clause`, and `qualify_clause`; remaining work includes optimizer hints, `model_clause`, and full semantic validation
- [ ] `table_or_subquery`: targeted parser coverage is staged for database links, representative `ONLY (query_table_expression)`, legacy `THE (subquery)` table collections, representative analytic view/hierarchy table references, representative flashback table syntax, representative pivot/unpivot/row-pattern table syntax, inline/modified external table references, and subquery restriction clauses; remaining work includes analytic view/hierarchy semantic validation, join-to-one table expressions, and full external table property validation
- [ ] `result_column`: Oracle aliases, `JSON`/`XML` returning clauses where expressions expose column names
- [ ] `function_expr`: Oracle aggregate, analytic, `WITHIN GROUP`, `KEEP`, `OVER`, `FILTER`, `RETURNING`, and special argument separators
- [ ] `insert_stmt`: representative Oracle single-table insert extensions are staged in BNF for remote targets, `RETURNING`, `WAIT`, `BY NAME` / `BY POSITION`, and DML error logging, and multi-table insert coverage exists; remaining work includes local parser verification plus direct-path hints, subquery insert variants, and `static_returning_clause`
- [ ] `update_stmt`: representative Oracle update extensions are staged in BNF for partition and remote targets, tuple subquery assignment, `WHERE CURRENT OF`, `RETURNING`, `WAIT`, direct-join `FROM`, and DML error logging; remaining work is local parser verification plus object-table and full `DML_table_expression_clause` coverage
- [ ] `delete_stmt`: representative Oracle delete extensions are staged in BNF for partition and remote targets, `WHERE CURRENT OF`, `RETURNING`, and DML error logging; remaining work is local parser verification plus object-table and full `DML_table_expression_clause` coverage
- [ ] `extension_stmt`: Oracle statements not represented by the core grammar, including transaction/session/security statements and non-table DDL
- [ ] `extension_expr`: Oracle expression extensions such as `PRIOR`, `CURSOR`, `TREAT`, `XMLQUERY`, `JSON_EXISTS`, `JSON_VALUE`, vector operators, and legacy outer join `(+)`

## Build Work

- [x] Add the official SQLDelight `app.cash.grammarkit-composer` generation path
- [x] Include generated parser/PSI sources in the compiled and published artifact
- [x] Keep generated files deterministic and reproducible from `src/main/kotlin/**/grammar/*.bnf`
- [x] Add tests that fail when `setup()` does not install Oracle parser overrides
- [x] Add parser tests using exact expected success/failure results for representative Oracle `.sq` files
