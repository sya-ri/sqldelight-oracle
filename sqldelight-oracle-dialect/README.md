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

## Supported Oracle Syntax

Baseline: [Oracle AI Database 26ai SQL Language Reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/index.html).

This dialect tracks Oracle SQL syntax that can be represented in SQLDelight `.sq` and `.sqm` files. The list below is for the SQLDelight dialect only: parser, PSI/mixins, type resolution, and SQLDelight-facing column resolution.

### Supported

Lexical and names:

- [Lexical conventions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Lexical-Conventions.html), [database object names and qualifiers](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Database-Object-Names-and-Qualifiers.html), [schema object references](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html), quoted identifiers, schema qualifiers, database links, partition/subpartition qualifiers, and Oracle comments/hints from [Comments](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html).
- [Literals](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html), including ordinary strings, national strings, `q'...'` / `nq'...'` quoted literals, signed numbers, binary float/double suffixes, datetime literals, and interval literals.

Queries and table expressions:

- [SELECT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html), including `WITH`, query-name column aliases, `SEARCH`, `CYCLE`, `OFFSET`, `FETCH`, `WITH TIES`, `QUALIFY`, flashback/version query clauses, partition extensions, `SAMPLE`, external table references, and named windows.
- [query_block](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/query_block.html), [hierarchical_query_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/hierarchical_query_clause.html), [row_limiting_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/row_limiting_clause.html), [group_by_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/group_by_clause.html), [order_by_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/order_by_clause.html), and [for_update_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/for_update_clause.html).
- [JSON_TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/JSON_TABLE.html), including `FORMAT JSON`, `PASSING`, row-level `ON ERROR` / `ON EMPTY`, `COLUMNS`, ordinality, value, exists, query-style `FORMAT JSON`, nested path columns, and SQLDelight column resolution for output columns.
- [XMLTABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/XMLTABLE.html), including SQLDelight column resolution for `COLUMNS` output columns.
- [Set operators](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/The-UNION-ALL-INTERSECT-MINUS-Operators.html), including `UNION`, `INTERSECT`, `MINUS`, `EXCEPT`, and their `ALL` forms.
- [Joins](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Joins.html), including ANSI joins, `CROSS APPLY`, `OUTER APPLY`, partitioned outer joins, and legacy `(+)` predicate syntax.
- [query_table_expression](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/query_table_expression.html) and [table_reference](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/table_reference.html), including direct and remote `ONLY (...)`, table references over `@dblink`, subquery restriction clauses, analytic-view/hierarchy references, `CONTAINERS`, `SHARDS`, flashback table references, `PIVOT`, `UNPIVOT`, `MATCH_RECOGNIZE`, `VALUES` table references, collection-table expressions, `JOIN TO ONE`, and row-widened table expressions.
- [row_pattern_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/row_pattern_clause.html), including measures, rows-per-match forms, skip forms, subset definitions, alternation, permutation, exclusion groups, bounded/reluctant quantifiers, SQLDelight `MEASURES` column resolution, and `ALL ROWS PER MATCH` source-column resolution.
- [GRAPHQL table function](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graphql-table-function.html), [GRAPH_TABLE operator](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph_table-operator.html), and SQL/PGQ/property-graph parser boundaries.

DML and procedural SQL statements:

- [MERGE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/MERGE.html), [INSERT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html), [UPDATE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/UPDATE.html), and [DELETE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DELETE.html), including named table targets, `ONLY`, `@dblink`, partition extensions, `RETURN` / `RETURNING`, `OLD` / `NEW` returning expressions, `WHERE CURRENT OF`, DML error logging, wait clauses, `UPDATE ... FROM`, `INSERT ALL` / `INSERT FIRST`, `BY NAME` / `BY POSITION`, `DEFAULT VALUES`, and SQLDelight target/source alias resolution where supported.
- [CALL](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CALL.html), [LOCK TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LOCK-TABLE.html), and [EXPLAIN PLAN](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/EXPLAIN-PLAN.html).

DDL and administrative SQL:

- [CREATE TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html), including relational/object/XMLType/JSON collection tables, identity and virtual columns, domains, immutable/blockchain tables, compression, storage, segment attributes, LOB/XML/JSON storage, external tables, partitioning, attribute clustering, temporal validity, supplemental logging, CTAS, and representative physical/table properties.
- [CREATE INDEX](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEX.html), [CREATE VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-VIEW.html), [CREATE SEQUENCE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SEQUENCE.html), [CREATE MATERIALIZED VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW.html), [CREATE MATERIALIZED VIEW LOG](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW-LOG.html), [CREATE MATERIALIZED ZONEMAP](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-ZONEMAP.html), [CREATE SYNONYM](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SYNONYM.html), [CREATE TRIGGER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TRIGGER.html), [CREATE TYPE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TYPE.html), [CREATE FUNCTION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-FUNCTION.html), [CREATE PROCEDURE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PROCEDURE.html), [CREATE PACKAGE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PACKAGE.html), [CREATE JAVA](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-JAVA.html), [CREATE LIBRARY](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-LIBRARY.html), [CREATE ANALYTIC VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ANALYTIC-VIEW.html), [CREATE ATTRIBUTE DIMENSION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ATTRIBUTE-DIMENSION.html), [CREATE HIERARCHY](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-HIERARCHY.html), [CREATE DIMENSION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DIMENSION.html), [CREATE PROPERTY GRAPH](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html), [CREATE JSON RELATIONAL DUALITY VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-json-relational-duality-view.html), [CREATE MLE ENV](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-mle-env.html), [CREATE MLE MODULE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-mle-module.html), users, roles, contexts, directories, clusters, operators, indextypes, lockdown profiles, profiles, editions, restore points, tablespaces, disk groups, and administrative database/PDB statements.
- [ALTER TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html), [ALTER INDEX](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEX.html), [ALTER SEQUENCE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SEQUENCE.html), [ALTER VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-VIEW.html), [ALTER MATERIALIZED VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW.html), [ALTER MATERIALIZED VIEW LOG](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-VIEW-LOG.html), [ALTER MATERIALIZED ZONEMAP](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-ZONEMAP.html), [ALTER SYNONYM](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYNONYM.html), [ALTER TRIGGER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TRIGGER.html), [ALTER TYPE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TYPE.html), [ALTER FUNCTION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-FUNCTION.html), [ALTER PROCEDURE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PROCEDURE.html), [ALTER PACKAGE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PACKAGE.html), [ALTER DATABASE LINK](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE-LINK.html), [ALTER PROPERTY GRAPH](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-property-graph.html), [ALTER JSON RELATIONAL DUALITY VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-json-relational-duality-view.html), MLE/assertion/directive/flashback/in-memory/database/PDB/tablespace/diskgroup/profile/resource-cost/rollback-segment forms.
- [DROP TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLE.html), [DROP INDEX](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-INDEX.html), [DROP SEQUENCE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-SEQUENCE.html), [DROP VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-VIEW.html), [DROP MATERIALIZED VIEW](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-MATERIALIZED-VIEW.html), [DROP SYNONYM](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-SYNONYM.html), [DROP TRIGGER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TRIGGER.html), [DROP TYPE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TYPE.html), [DROP FUNCTION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-FUNCTION.html), [DROP PROCEDURE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PROCEDURE.html), [DROP PACKAGE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-PACKAGE.html), [TRUNCATE TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-TABLE.html), [TRUNCATE CLUSTER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-CLUSTER.html), [RENAME](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/RENAME.html), [COMMENT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMENT.html), [ANALYZE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ANALYZE.html), [ASSOCIATE STATISTICS](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ASSOCIATE-STATISTICS.html), [DISASSOCIATE STATISTICS](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DISASSOCIATE-STATISTICS.html), [PURGE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/PURGE.html), [FLASHBACK TABLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/FLASHBACK-TABLE.html), and [FLASHBACK DATABASE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/FLASHBACK-DATABASE.html) statement families.
- Common DDL clauses such as [constraint](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html), [physical_attributes_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/physical_attributes_clause.html), [storage_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/storage_clause.html), [logging_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/logging_clause.html), [parallel_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/parallel_clause.html), [file_specification](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/file_specification.html), [annotations_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/annotations_clause.html), and partitioning/storage variants.

Transactions, security, and data types:

- [ALTER SESSION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SESSION.html), [ALTER SYSTEM](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYSTEM.html), [COMMIT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMMIT.html), [ROLLBACK](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ROLLBACK.html), [SAVEPOINT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SAVEPOINT.html), [SET TRANSACTION](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-TRANSACTION.html), [SET CONSTRAINTS](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-CONSTRAINTS.html), [SET ROLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SET-ROLE.html), `SET USE DATA GRANTS ONLY`, and `ADMINISTER KEY MANAGEMENT`.
- [GRANT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/GRANT.html), [REVOKE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/REVOKE.html), [CREATE USER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-USER.html), [ALTER USER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-USER.html), [DROP USER](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-USER.html), [CREATE ROLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLE.html), [ALTER ROLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLE.html), [DROP ROLE](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-ROLE.html), [AUDIT unified](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Unified-Auditing.html), [NOAUDIT unified](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Unified-Auditing.html), traditional auditing, data security statements, and end-user security statements.
- Oracle scalar type names from [Data Types](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Types.html), data use-case domains, ANSI/DB2/SQL-DS aliases, JSON/XML/spatial/collection/URI/vector names, parser-backed `type_name` support, and SQLDelight type mapping.

Expressions, conditions, operators, and functions:

- [Pseudocolumns](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Pseudocolumns.html), [operators](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/About-SQL-Operators.html), [CURSOR expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CURSOR-Expressions.html), [CASE expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CASE-Expressions.html), [Scalar Subquery Expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Scalar-Subquery-Expressions.html), [CAST](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CAST.html), [TREAT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TREAT.html), [Function Expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Function-Expressions.html), [Simple Expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Simple-Expressions.html), [Type Constructor Expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Type-Constructor-Expressions.html), and [BOOLEAN expressions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/boolean-expressions.html).
- Conditions including [comparison](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comparison-Conditions.html), [Null conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Null-Conditions.html), [BETWEEN](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/BETWEEN-Condition.html), [EXISTS](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/EXISTS-Condition.html), [IN](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IN-Condition.html), [Floating-Point Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Floating-Point-Conditions.html), [IS OF type](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/IS-OF-type-Condition.html), [Multiset Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Multiset-Conditions.html), [SQL/JSON conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SQL-JSON-Conditions.html), [Model Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Model-Conditions.html), [XML Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/XML-Conditions.html), and [Pattern-matching Conditions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Pattern-matching-Conditions.html).
- [Aggregate functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Aggregate-Functions.html), [LISTAGG](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LISTAGG.html), [Analytic Functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Analytic-Functions.html), [Single-Row Functions](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Single-Row-Functions.html), SQL/JSON functions, XML functions, vector functions/operators, OML functions, domain/UUID functions, [EXTRACT](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/EXTRACT-datetime.html), [TO_LOB](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TO_LOB.html), object-reference functions, environment functions, and argument-dependent return handling for selected functions.

Compatibility:

- [Oracle and Standard SQL](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-and-Standard-SQL.html) surfaces relevant to `.sq` parsing.
- [Older Oracle SQL compatibility](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-Compliance-with-Older-Standards.html), including legacy outer joins, deprecated collection unnesting, traditional auditing, legacy type names, legacy table compression/storage forms, materialized view terminology, and administrative legacy forms.

### Not Supported Or Deferred

The following are intentionally not treated as `0.1.0` blockers:

- [MODEL query clauses](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/model_clause.html), model cell references, and JSON array-step bracket access. These conflict with SQLDelight core bracket/index parsing today.
- Non-named DML targets such as subquery targets with [subquery_restriction_clause](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/img_text/subquery_restriction_clause.html), table collection targets, non-named object table targets, and object view targets. SQLDelight mutator paths currently assume named `SqlTableName` / `SqlQualifiedTableName` targets.
- PL/SQL trigger body parsing beyond SQL statement boundaries.
- Full embedded GraphQL syntax validation inside `GRAPHQL('...')`.
- Full regular expression validation for [Oracle regular expression syntax](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Oracle-Regular-Expression-Support.html).
- Full semantic validation that requires database metadata or schema-aware object models: object kind restrictions, privileges, release-specific limits, remote object validation, object/REF attribute validation, analytic-view/hierarchy object validation, generated-column inference beyond explicit `PIVOT` / `UNPIVOT` aliases, recursive CTE integration, expression-context restrictions, function overload/placement validation, and runtime behavior controlled by optimizer hints.

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
dev.s7a.sqldelight.oracle.dialects.oracle.OracleDialect
```
