# Oracle SQL Syntax Coverage

Baseline: [Oracle AI Database 26ai SQL Language Reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/toc.htm), May 2026.

This checklist tracks parser/source-scanner/rule support. Check an item only after it has targeted FunSpec tests.

## Lexical Syntax And Names

- [ ] [Lexical conventions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Lexical-Conventions.html): case sensitivity, whitespace, delimiters, comments, and statement terminators
- [ ] [Database object names and qualifiers](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Database-Object-Names-and-Qualifiers.html): quoted identifiers, schema qualifiers, database links, and partition/subpartition qualifiers
- [ ] [Schema object reference syntax](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html): remote object names, object attributes/methods, and partitioned table/index references
- [ ] [Oracle reserved words and keywords](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-SQL-Reserved-Words-and-Keywords.html)
- [ ] [SQL comments and hints](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html): `--`, `/* ... */`, optimizer hints, and hint placement in DML/queries

## Source Scanner

- [x] sqldelight-check source scanner statement starts synchronized with the Oracle 26ai SQL Statements TOC

## Literals And Format Models

- [ ] [Text literals](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html): ordinary strings, national character strings, alternative quoting `q'...'`, and `nq'...'`
- [ ] [Numeric literals](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html): signed numbers, exponent notation, `BINARY_FLOAT`/`BINARY_DOUBLE` suffixes, NaN, and infinity literals
- [ ] [Datetime literals](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html): `DATE`, `TIMESTAMP`, and time-zone literal forms
- [ ] [Interval literals](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html): `INTERVAL YEAR TO MONTH` and `INTERVAL DAY TO SECOND`
- [ ] [Number, datetime, and XML format models](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Format-Models.html): format model tokens used by conversion functions

## Queries

- [x] [`SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) source-scanner statement and major clause boundaries
- [x] [`hierarchical_query_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `START WITH`, `CONNECT BY`
- [x] [`row_limiting_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `OFFSET`, `FETCH FIRST`, `FETCH NEXT`
- [x] [`pivot_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) / [`unpivot_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`model_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`row_pattern_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) / [`MATCH_RECOGNIZE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`qualify_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html)
- [x] [`flashback_query_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `AS OF SCN`, `AS OF TIMESTAMP`, `VERSIONS BETWEEN`
- [x] [`JSON_TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/JSON_TABLE.html)
- [x] [`XMLTABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/XMLTABLE.html)
- [x] [Analytic view](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ANALYTIC-VIEW.html) query syntax source-scanner boundaries
- [x] [SQL/PGQ graph query](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph_table-operator.html) syntax source-scanner boundaries
- [ ] [Subquery factoring and recursive subquery factoring](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `WITH`, recursive query name, `SEARCH`, `CYCLE`
- [ ] [Set operators](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/The-UNION-ALL-INTERSECT-MINUS-Operators.html): `UNION`, `UNION ALL`, `INTERSECT`, `MINUS`, `EXCEPT`
- [ ] [Join syntax](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Joins.html): ANSI joins, Oracle outer join operator, partitioned outer joins, lateral inline views
- [ ] [`query_table_expression`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): table collection expressions, `ONLY`, `SAMPLE`, partition extension, `LATERAL`
- [ ] [`CONTAINERS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) and [`SHARDS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html) query clauses
- [ ] [`select_list`, aliases, and wildcard expansion](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `DISTINCT`, `UNIQUE`, `ALL`, object attributes, and column alias forms
- [ ] [`GROUP BY`, `HAVING`, and aggregation extensions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `ROLLUP`, `CUBE`, `GROUPING SETS`, composite columns, and grouping expressions
- [ ] [`ORDER BY` and `order_by_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): positional ordering, aliases, `NULLS FIRST/LAST`, and `SIBLINGS`
- [ ] [`for_update_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html): `FOR UPDATE`, `OF`, `NOWAIT`, `WAIT`, and `SKIP LOCKED`
- [ ] [`GRAPHQL` table function](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graphql-table-function.html)

## DML

- [x] [`MERGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html) parser support for `INTO`, table/subquery/`VALUES` `USING` sources, `ON`, [`merge_update_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/merge_update_clause.html), [`merge_insert_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/merge_insert_clause.html), `DELETE WHERE`, and [`error_logging_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/error_logging_clause.html)
- [x] [`RETURNING`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html) clause boundary
- [x] [`INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- [x] [Multi-table `INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html)
- [x] [`UPDATE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/UPDATE.html)
- [x] [`DELETE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DELETE.html)
- [x] [`CALL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CALL.html) parser support for positional-argument [`routine_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/routine_clause.html)
- [x] [`LOCK TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html) parser support for multiple table references, [`partition_extension_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/partition_extension_clause.html), lock modes, `NOWAIT`, and `WAIT`
- [x] [`EXPLAIN PLAN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/EXPLAIN-PLAN.html)
- [ ] [`INSERT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html) error logging, direct-path, partition extension, and `DEFAULT VALUES` forms
- [ ] [`UPDATE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/UPDATE.html) partition extension, correlated subquery, object table, and update set clause forms
- [ ] [`DELETE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DELETE.html) partition extension, correlated subquery, and object table forms
- [ ] [`MERGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html) full semantic integration for target/source aliases, DML table expression variants, optimizer hints, `wait_clause`, and `returning_clause`
- [ ] [DML table expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html): subquery, collection, partition/subpartition, remote object, and `ONLY` target forms
- [ ] [DML optimizer hints](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html): statement-level and query-block hints in `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `MERGE`
- [ ] [DML error logging clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html): `LOG ERRORS INTO`, `REJECT LIMIT`, and reusable DML clause parsing
- [ ] [`CALL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/call.html) full support for `object_access_expression`, named or mixed argument notation, `@dblink_name`, and `INTO :host_variable`
- [ ] [`LOCK TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html) full support for remote object references with `@dblink`

## DDL

- [x] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) type names and statement baseline
- [ ] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) advanced table forms: `IF NOT EXISTS`, private temporary tables, blockchain/immutable tables, sharded tables, external tables, object tables, XMLType tables
- [x] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) parser support for `IF NOT EXISTS`, `GLOBAL TEMPORARY`, `PRIVATE TEMPORARY`, and `ON COMMIT`
- [ ] [`CREATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) column and table clauses: identity, default/collation, virtual columns, invisible columns, encryption, compression, in-memory, and row archival
- [x] [`identity_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html), inline visibility, and inline constraint state parser support for `CREATE TABLE` columns
- [x] [`constraint`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html) parser support for table-level `PRIMARY KEY`, `UNIQUE`, `CHECK`, and `FOREIGN KEY` constraint states
- [ ] [`CREATE TABLE AS SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html), `ON COMMIT`, organization, clustering, and table-level storage variants
- [x] [`CREATE TABLE AS SELECT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) parser support for SQLDelight query files
- [x] [`CREATE INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEX.html) parser support for `UNIQUE`, `BITMAP`, `IF NOT EXISTS`, ordinary/function-based indexed columns, visibility, `ONLINE`, `REVERSE`, logging, compression, and `INDEXING FULL/PARTIAL`
- [ ] [`CREATE INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEX.html) index variants: domain, cluster, bitmap join, XMLIndex, multivalue JSON, local/global partitioned indexes, ILM, storage, and advanced index attributes
- [x] [`CREATE VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-VIEW.html) parser support for `OR REPLACE`, `FORCE` / `NO FORCE`, editioning keywords, `BEQUEATH`, `WITH CHECK OPTION`, and `WITH READ ONLY`
- [ ] [`CREATE VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-VIEW.html) view variants: view column alias list semantic validation, object views, XMLType views, inline/out-of-line constraints, and advanced sharing clauses
- [x] [`CREATE SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SEQUENCE.html) parser support for schema-qualified names, `IF NOT EXISTS`, `START WITH`, `INCREMENT BY`, `MINVALUE` / `MAXVALUE`, `CACHE` / `NOCACHE`, `CYCLE` / `NOCYCLE`, `ORDER` / `NOORDER`, `KEEP` / `NOKEEP`, `SCALE` / `NOSCALE`, `SHARD` / `NOSHARD`, and `SESSION` / `GLOBAL`
- [ ] [`CREATE SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SEQUENCE.html) advanced sequence variants: `SHARING`, `NOORDER` combinations with scalable/sharded options, and semantic conflict validation
- [x] [`CREATE MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW.html)
- [x] [`CREATE MATERIALIZED VIEW LOG`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW-LOG.html)
- [ ] [`CREATE MATERIALIZED ZONEMAP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-ZONEMAP.html)
- [x] [`CREATE SYNONYM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SYNONYM.html)
- [x] [`CREATE TRIGGER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TRIGGER.html)
- [x] [`CREATE TYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TYPE.html)
- [ ] [`CREATE TYPE BODY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TYPE-BODY.html)
- [x] [`CREATE PACKAGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PACKAGE.html)
- [ ] [`CREATE PACKAGE BODY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PACKAGE-BODY.html)
- [ ] [`CREATE FUNCTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-FUNCTION.html)
- [x] [`CREATE PROCEDURE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PROCEDURE.html)
- [ ] [`CREATE LIBRARY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-LIBRARY.html)
- [x] [`CREATE DIRECTORY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DIRECTORY.html)
- [x] [`CREATE DATABASE LINK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DATABASE-LINK.html)
- [x] [`CREATE CLUSTER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-CLUSTER.html)
- [x] [`CREATE INDEXTYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEXTYPE.html)
- [ ] [`CREATE ANALYTIC VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ANALYTIC-VIEW.html), [`CREATE ATTRIBUTE DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ATTRIBUTE-DIMENSION.html), [`CREATE HIERARCHY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-HIERARCHY.html), [`CREATE DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DIMENSION.html)
- [ ] [`CREATE PROPERTY GRAPH`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html)
- [ ] [`CREATE JSON RELATIONAL DUALITY VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-json-relational-duality-view.html)
- [ ] [`CREATE VECTOR INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-vector-index.html) and [`CREATE HYBRID VECTOR INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-hybrid-vector-index.html)
- [ ] [`CREATE ICEBERG TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-iceberg-table.html)
- [ ] [`CREATE OPERATOR`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-OPERATOR.html) and [`CREATE OUTLINE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-OUTLINE.html)
- [ ] [`CREATE CONTEXT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-CONTEXT.html), [`CREATE MLE ENV`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-mle-env.html), [`CREATE MLE MODULE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-mle-module.html)
- [ ] [`CREATE PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PROFILE.html), [`CREATE SCHEMA`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SCHEMA.html), [`CREATE FLASHBACK ARCHIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-FLASHBACK-ARCHIVE.html)
- [ ] [`CREATE ASSERTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html), [`CREATE DIRECTIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-directive-validate.html), [`CREATE EDITION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-EDITION.html), [`CREATE INMEMORY JOIN GROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INMEMORY-JOIN-GROUP.html), [`CREATE JAVA`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-JAVA.html), [`CREATE LOGICAL PARTITION TRACKING`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-logical-partition-tracking.html)
- [ ] Administrative create statements: [`CREATE DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DATABASE.html), [`CREATE PLUGGABLE DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PLUGGABLE-DATABASE.html), [`CREATE CONTROLFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-CONTROLFILE.html), [`CREATE TABLESPACE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLESPACE.html), [`CREATE TABLESPACE SET`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLESPACE-SET.html), [`CREATE DISKGROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DISKGROUP.html), [`CREATE SPFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SPFILE.html), [`CREATE PFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PFILE.html), [`CREATE TRUE CACHE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-true-cache.html)
- [ ] Administrative legacy/create object statements: [`CREATE LOCKDOWN PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-LOCKDOWN-PROFILE.html), [`CREATE PMEM FILESTORE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-pmem-filestore.html), [`CREATE RESTORE POINT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-RESTORE-POINT.html), [`CREATE ROLLBACK SEGMENT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLLBACK-SEGMENT.html)
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html)
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) parser support for `ADD column`
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) parser support for single-operation `ADD` table constraint, `MODIFY` column, `DROP COLUMN`, `DROP CONSTRAINT`, and `RENAME COLUMN`
- [x] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) parser support for Oracle multiple-column `ADD` and `MODIFY`
- [ ] [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) parser support for Oracle partition, storage, LOB, and table property clauses
- [x] [`ALTER INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEX.html)
- [x] [`ALTER SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SEQUENCE.html) parser support for schema-qualified names, `IF EXISTS`, `RESTART`, and sequence option clauses shared with `CREATE SEQUENCE`
- [x] [`ALTER VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-VIEW.html)
- [x] [`ALTER MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW.html)
- [ ] [`ALTER MATERIALIZED VIEW LOG`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW-LOG.html) and [`ALTER MATERIALIZED ZONEMAP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-ZONEMAP.html)
- [ ] [`ALTER SYNONYM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYNONYM.html), [`ALTER TRIGGER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TRIGGER.html), [`ALTER TYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TYPE.html)
- [ ] [`ALTER FUNCTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-FUNCTION.html), [`ALTER PROCEDURE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PROCEDURE.html), [`ALTER PACKAGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PACKAGE.html), [`ALTER LIBRARY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-LIBRARY.html), [`ALTER JAVA`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-JAVA.html)
- [ ] [`ALTER ANALYTIC VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ANALYTIC-VIEW.html), [`ALTER ATTRIBUTE DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ATTRIBUTE-DIMENSION.html), [`ALTER HIERARCHY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-HIERARCHY.html), [`ALTER DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DIMENSION.html)
- [ ] [`ALTER CLUSTER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-CLUSTER.html), [`ALTER DATABASE LINK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE-LINK.html), [`ALTER INDEXTYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEXTYPE.html), [`ALTER OPERATOR`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-OPERATOR.html), [`ALTER OUTLINE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-OUTLINE.html)
- [ ] [`ALTER PROPERTY GRAPH`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-property-graph.html), [`ALTER JSON RELATIONAL DUALITY VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-json-relational-duality-view.html)
- [ ] [`ALTER MLE ENV`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-mle-env.html), [`ALTER MLE MODULE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-mle-module.html)
- [ ] [`ALTER ASSERTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-assertion.html), [`ALTER DIRECTIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-directive-validate.html), [`ALTER FLASHBACK ARCHIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-FLASHBACK-ARCHIVE.html), [`ALTER INMEMORY JOIN GROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INMEMORY-JOIN-GROUP.html), [`ALTER LOCKDOWN PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-LOCKDOWN-PROFILE.html)
- [ ] Administrative alter statements: [`ALTER DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE.html), [`ALTER DATABASE DICTIONARY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE-DICTIONARY.html), [`ALTER PLUGGABLE DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PLUGGABLE-DATABASE.html), [`ALTER TABLESPACE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLESPACE.html), [`ALTER TABLESPACE SET`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLESPACE-SET.html), [`ALTER DISKGROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DISKGROUP.html)
- [ ] Administrative legacy alter statements: [`ALTER PMEM FILESTORE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-pmem-filestore.html), [`ALTER PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PROFILE.html), [`ALTER RESOURCE COST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-RESOURCE-COST.html), [`ALTER ROLLBACK SEGMENT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLLBACK-SEGMENT.html)
- [x] [`DROP TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLE.html)
- [x] [`DROP INDEX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INDEX.html)
- [x] [`DROP SEQUENCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-SEQUENCE.html) parser support for schema-qualified names and `IF EXISTS`
- [x] [`DROP VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-VIEW.html)
- [x] [`DROP MATERIALIZED VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-MATERIALIZED-VIEW.html)
- [ ] [`DROP MATERIALIZED VIEW LOG`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-MATERIALIZED-VIEW-LOG.html) and [`DROP MATERIALIZED ZONEMAP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-MATERIALIZED-ZONEMAP.html)
- [ ] [`DROP SYNONYM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-SYNONYM.html), [`DROP TRIGGER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TRIGGER.html), [`DROP TYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TYPE.html), [`DROP TYPE BODY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TYPE-BODY.html)
- [ ] [`DROP FUNCTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-FUNCTION.html), [`DROP PROCEDURE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PROCEDURE.html), [`DROP PACKAGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PACKAGE.html), [`DROP LIBRARY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-LIBRARY.html), [`DROP JAVA`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-JAVA.html)
- [ ] [`DROP ANALYTIC VIEW`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ANALYTIC-VIEW.html), [`DROP ATTRIBUTE DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ATTRIBUTE-DIMENSION.html), [`DROP HIERARCHY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-HIERARCHY.html), [`DROP DIMENSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-DIMENSION.html)
- [ ] [`DROP CLUSTER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-CLUSTER.html), [`DROP DATABASE LINK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-DATABASE-LINK.html), [`DROP DIRECTORY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-DIRECTORY.html), [`DROP INDEXTYPE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INDEXTYPE.html), [`DROP OPERATOR`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-OPERATOR.html), [`DROP OUTLINE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-OUTLINE.html)
- [ ] [`DROP PROPERTY GRAPH`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-property-graph.html), [`DROP ICEBERG TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-iceberg-table.html), [`DROP CONTEXT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-CONTEXT.html), [`DROP MLE ENV`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-mle-env.html), [`DROP MLE MODULE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-mle-module.html)
- [ ] [`DROP ASSERTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-assertion.html), [`DROP DIRECTIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-directive-validate.html), [`DROP EDITION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-EDITION.html), [`DROP FLASHBACK ARCHIVE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-FLASHBACK-ARCHIVE.html), [`DROP INMEMORY JOIN GROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INMEMORY-JOIN-GROUP.html)
- [ ] Administrative drop statements: [`DROP DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-DATABASE.html), [`DROP PLUGGABLE DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PLUGGABLE-DATABASE.html), [`DROP TABLESPACE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLESPACE.html), [`DROP TABLESPACE SET`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLESPACE-SET.html), [`DROP DISKGROUP`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-DISKGROUP.html), [`DROP RESTORE POINT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-RESTORE-POINT.html)
- [ ] Administrative legacy drop statements: [`DROP LOCKDOWN PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-LOCKDOWN-PROFILE.html), [`DROP PMEM FILESTORE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-pmem-filestore.html), [`DROP PROFILE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PROFILE.html), [`DROP ROLLBACK SEGMENT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ROLLBACK-SEGMENT.html)
- [x] [`TRUNCATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html)
- [ ] [`TRUNCATE CLUSTER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-CLUSTER.html)
- [x] [`RENAME`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/RENAME.html)
- [x] [`COMMENT ON`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMENT.html)
- [ ] [`ANALYZE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ANALYZE.html), [`ASSOCIATE STATISTICS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ASSOCIATE-STATISTICS.html), [`DISASSOCIATE STATISTICS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DISASSOCIATE-STATISTICS.html)
- [ ] [`PURGE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/PURGE.html), [`FLASHBACK TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/FLASHBACK-TABLE.html), [`FLASHBACK DATABASE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/FLASHBACK-DATABASE.html)

## Common DDL Clauses

- [ ] [`constraint`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html): inline/out-of-line constraints, `DEFERRABLE`, `RELY`, `NOVALIDATE`, `USING INDEX`
- [ ] [`physical_attributes_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/physical_attributes_clause.html), [`storage_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/storage_clause.html), [`logging_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/logging_clause.html), [`parallel_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/parallel_clause.html)
- [ ] [`allocate_extent_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/allocate_extent_clause.html), [`deallocate_unused_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/deallocate_unused_clause.html), [`file_specification`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/file_specification.html), [`size_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/size_clause.html)
- [ ] [`annotations_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/annotations_clause.html)
- [ ] [Partitioning clauses](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html): range, list, hash, composite, interval, reference, system, automatic list, and partition templates
- [ ] [LOB storage and XMLType storage clauses](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html): SecureFiles/BasicFiles, inline/out-of-line storage, compression, deduplication, and encryption
- [ ] [Object-relational clauses](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html): object identifiers, object properties, nested table storage, varray storage, and `REF` constraints

## Transactions And Session Statements

- [x] [`ALTER SESSION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SESSION.html)
- [x] [`ALTER SYSTEM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYSTEM.html)
- [x] [`COMMIT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMIT.html)
- [x] [`ROLLBACK`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ROLLBACK.html)
- [x] [`SAVEPOINT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SAVEPOINT.html)
- [x] [`SET TRANSACTION`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-TRANSACTION.html)
- [x] [`SET CONSTRAINTS`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-CONSTRAINTS.html)
- [ ] [`SET ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-ROLE.html)
- [ ] [`SET USE DATA GRANTS ONLY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/set-use-data-grants-only.html)
- [ ] [`ADMINISTER KEY MANAGEMENT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ADMINISTER-KEY-MANAGEMENT.html)

## Security And Privileges

- [x] [`GRANT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GRANT.html)
- [x] [`REVOKE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/REVOKE.html)
- [x] [`CREATE USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-USER.html)
- [x] [`ALTER USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-USER.html)
- [x] [`DROP USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-USER.html)
- [x] [`CREATE ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLE.html)
- [x] [`ALTER ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLE.html)
- [x] [`DROP ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ROLE.html)
- [ ] [`CREATE AUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html), [`ALTER AUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html), [`DROP AUDIT POLICY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-AUDIT-POLICY-Unified-Auditing.html)
- [ ] [`AUDIT` traditional](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Traditional-Auditing.html), [`AUDIT` unified](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Unified-Auditing.html), [`NOAUDIT` traditional](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Traditional-Auditing.html), [`NOAUDIT` unified](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Unified-Auditing.html)
- [ ] Data security statements: [`CREATE DATA ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-data-role.html), [`DROP DATA ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-data-role.html), [`GRANT DATA ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/grant-data-role.html), [`REVOKE DATA ROLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/revoke-data-role.html), [`CREATE DATA GRANT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-data-grant.html), [`DROP DATA GRANT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-data-grant.html)
- [ ] End-user security statements: [`CREATE APPLICATION IDENTITY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-application-identity.html), [`DROP APPLICATION IDENTITY`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-application-identity.html), [`CREATE END USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-end-user.html), [`ALTER END USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-end-user.html), [`DROP END USER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-end-user.html), [`CREATE END USER CONTEXT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-end-user-context.html), [`DROP END USER CONTEXT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-end-user-context.html), [`UPDATE END USER CONTEXT`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/update-end-user-context.html)

## Data Types

- [x] [Character types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `CHAR`, `NCHAR`, `VARCHAR2`, `NVARCHAR2`
- [x] [Numeric types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `NUMBER`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- [x] [Date/time types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `DATE`, `TIMESTAMP`, `INTERVAL`
- [x] [LOB and raw types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `BLOB`, `CLOB`, `NCLOB`, `BFILE`, `RAW`, `LONG RAW`
- [x] [Row id types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `ROWID`, `UROWID`
- [x] [JSON/XML/spatial/vector names](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `JSON`, `XMLTYPE`, `SDO_GEOMETRY`, `VECTOR`
- [x] [Object/reference type names](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `OBJECT`, `REF`
- [x] [Collection types and nested tables](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [x] Data use case domains: [`CREATE DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html), [`ALTER DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-domain.html), [`DROP DOMAIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/drop-domain.html)
- [ ] [Boolean data type](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [ ] [Extended data types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [ ] [ANSI, DB2, and SQL/DS data type aliases](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html)
- [x] [Any types and XML URI types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `ANYTYPE`, `ANYDATA`, `ANYDATASET`, `URIType`, `DBURIType`, `XDBURIType`, `HTTPURIType`
- [x] [Spatial and GeoRaster type names](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html): `SDO_TOPO_GEOMETRY`, `SDO_GEORASTER`
- [x] SQLDelight type resolver mapping for Oracle scalar data types, including `NUMBER` precision/scale, ANSI character/numeric aliases, `BINARY_INTEGER`, `PLS_INTEGER`, `DATE`, `TIMESTAMP`, text, binary, JSON/XML, spatial, collection, URI, and vector names
- [x] SQLDelight parser `type_name` hook for Oracle scalar, multi-word, and parameterized data type names
- [x] SQLDelight parser-backed `.sq` tests with exact success/failure results for representative Oracle type names in `CREATE TABLE`
- [x] SQLDelight function resolver mapping for deterministic Oracle functions such as `SYSDATE`, `SYSTIMESTAMP`, `TO_CHAR`, `TO_NUMBER`, `HEXTORAW`, JSON/XML constructors, UUID helpers, vector constructors and distance functions, domain predicates, analytic rank functions, and numeric math functions
- [x] SQLDelight argument-dependent function resolver mapping for [`COALESCE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COALESCE.html), [`NVL`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL.html), [`NVL2`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NVL2.html), [`GREATEST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GREATEST.html), [`LEAST`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LEAST.html), [`MAX`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MAX.html), [`MIN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MIN.html), and [`SUM`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SUM.html)
- [x] sqldelight-check source scanner data type and common function patterns synchronized with SQLDelight Oracle type and function resolver coverage

## Operators, Expressions, And Conditions

- [ ] [Pseudocolumns](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Pseudocolumns.html): hierarchical, sequence, version query, `ROWID`, `ROWNUM`, `ORA_ROWSCN`
- [ ] [Additional pseudocolumns](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Pseudocolumns.html): `COLUMN_VALUE`, `OBJECT_ID`, `OBJECT_VALUE`, `ORA_SHARDSPACE_NAME`, and `XMLDATA`
- [ ] [Operators](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Operators.html): arithmetic, `COLLATE`, concatenation, hierarchical, set, multiset, user-defined, data quality, `JSON_ID`
- [ ] [`GRAPH_TABLE` operator](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph_table-operator.html): graph references, path patterns, element patterns, graph table shape, and graph value expressions
- [ ] [Expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Expressions.html): `CASE`, `CURSOR`, datetime/interval, JSON object access, object access, placeholders, scalar subqueries, type constructors, boolean expressions
- [ ] [Analytic view and model expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Expressions.html): analytic view expressions, model expressions, cell references, and iteration expressions
- [ ] [Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Conditions.html): comparison, floating-point, logical, multiset, pattern matching, null, XML, SQL/JSON, compound, `BETWEEN`, `EXISTS`, `IN`, `IS OF`, boolean test
- [ ] [Regular expression syntax](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-Regular-Expression-Support.html): POSIX, Unicode multilingual enhancements, and Perl-influenced extensions used by `REGEXP_*`

## Functions

- [ ] [Aggregate functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Aggregate-Functions.html): ordinary aggregates, `DISTINCT`/`ALL`, approximate aggregates, bit/boolean aggregates, statistical aggregates, and `KEEP`
- [ ] [Analytic functions and `analytic_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Analytic-Functions.html): `OVER`, `PARTITION BY`, `ORDER BY`, windowing, ranking, distribution, reporting, and lag/lead functions
- [ ] [Single-row function categories](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html): numeric, calendar, character, character set, collation, datetime, comparison, conversion, LOB, collection, hierarchical, encoding/decoding, null-related, environment and identifier
- [ ] [Model, object reference, OLAP, and data cartridge functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Functions.html): `CV`, `ITERATION_NUMBER`, `PRESENT*`, `PREVIOUS`, `REF`, `DEREF`, `MAKE_REF`, and extensible index functions
- [ ] [SQL/JSON functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-C13171B3-C070-4137-AC71-7A30BD26F380): `JSON_ARRAY`, `JSON_OBJECT`, `JSON_QUERY`, `JSON_VALUE`, `JSON_SERIALIZE`, `JSON_TRANSFORM`, `JSON_SCALAR`
- [ ] [XML functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-C64CC0DE-0D7C-42C8-B078-92A2984AD953): `XMLAGG`, `XMLCAST`, `XMLELEMENT`, `XMLEXISTS`, `XMLQUERY`, `XMLSERIALIZE`, `XMLTABLE`
- [ ] [Vector functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-C0C477F1-8210-4CA9-A5FA-0A340C409892): `VECTOR`, `TO_VECTOR`, `VECTOR_DISTANCE`, `VECTOR_EMBEDDING`, `VECTOR_SERIALIZE`
- [ ] [Domain functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-AEF8F898-493F-4BE8-86E6-06241BB78AB0), [UUID functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-97F3185F-B39A-492A-AD01-8CBCD4713AC9), [Oracle Machine Learning functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html#GUID-E64F8D20-C7E2-482A-914F-2781D0AA4E64)

## SQL Standards And Compatibility

- [ ] [Oracle and Standard SQL](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-and-Standard-SQL.html): Oracle extensions, optional SQL/Foundation features, SQL/XML, SQL/JSON, SQL/PGQ, and older standards compatibility
- [ ] [Older Oracle SQL compatibility](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-Compliance-with-Older-Standards.html): legacy outer joins, older data type aliases, and compatibility syntax that may appear in existing schemas

## Rules

- [x] `oracle:no-empty-string-comparison` for Oracle [null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html)
- [x] Prefer Oracle [`identity_clause`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html) columns over sequence-trigger pairs
- [x] Flag nullable [`NOT IN`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IN-Condition.html) predicates where [Oracle null semantics](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Nulls.html) are likely unintended
- [x] Flag unsafe [`ALTER TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html) / [`TRUNCATE TABLE`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html) migration DDL that rewrites, locks, or destructively changes large tables
- [x] Require explicit precision for [`NUMBER`](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html) where generated Kotlin type would be ambiguous

## Database Verification

- [x] Gated Testcontainers smoke test for connection, DDL, and DML execution
- [x] Testcontainers smoke test executed against local Docker with `gvenzl/oracle-free:23-slim-faststart` and `ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1`
- [x] Gated Testcontainers DDL round-trip test for representative supported type names
- [x] Gated Testcontainers DML round-trip test for `INSERT`, `UPDATE`, `DELETE`, `MERGE`
