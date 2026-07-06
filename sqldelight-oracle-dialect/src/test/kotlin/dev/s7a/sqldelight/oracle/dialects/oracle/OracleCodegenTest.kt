package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.core.annotators.OptimisticLockCompilerAnnotator
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import com.intellij.lang.LanguageParserDefinitions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

class OracleCodegenTest :
    FunSpec({
        test("generates Oracle assignment bind parameters in SQLDelight query files exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100),
                      PRIMARY KEY (id)
                    );

                    insertPositional:
                    INSERT INTO sample (id, label)
                    VALUES (?, ?);

                    insertNamed:
                    INSERT INTO sample (id, label, note)
                    VALUES (:id, :label, :note);

                    updateMixed:
                    UPDATE sample
                    SET label = :label,
                        note = ?
                    WHERE id = :id;

                    insertFromSelect:
                    INSERT INTO sample (id, label, note)
                    SELECT :id, label, :note
                    FROM sample
                    WHERE id = :source_id;
                    """.trimIndent(),
                )

            generated.fileNames shouldContainAll
                listOf(
                    "com/example/Sample.kt",
                    "com/example/TestQueries.kt",
                )
            generated.contentsByFile.getValue("com/example/TestQueries.kt") shouldBe
                """
                package com.example

                import app.cash.sqldelight.TransacterImpl
                import app.cash.sqldelight.db.QueryResult
                import app.cash.sqldelight.db.SqlDriver
                import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
                import kotlin.Long
                import kotlin.String

                public class TestQueries(
                  driver: SqlDriver,
                  private val sampleAdapter: Sample.Adapter,
                ) : TransacterImpl(driver) {
                  /**
                   * @return The number of rows updated.
                   */
                  public fun insertPositional(id: Long, label: Label): QueryResult<Long> {
                    val result = driver.execute(1_307_207_658, ""${'"'}
                        |INSERT INTO sample (id, label)
                        |VALUES (?, ?)
                        ""${'"'}.trimMargin(), 2) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindLong(parameterIndex++, id)
                          bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))
                        }
                    notifyQueries(1_307_207_658) { emit ->
                      emit("sample")
                    }
                    return result
                  }

                  /**
                   * @return The number of rows updated.
                   */
                  public fun insertNamed(
                    id: Long,
                    label: Label,
                    note: String?,
                  ): QueryResult<Long> {
                    val result = driver.execute(-615_661_405, ""${'"'}
                        |INSERT INTO sample (id, label, note)
                        |VALUES (?, ?, ?)
                        ""${'"'}.trimMargin(), 3) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindLong(parameterIndex++, id)
                          bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))
                          bindString(parameterIndex++, note)
                        }
                    notifyQueries(-615_661_405) { emit ->
                      emit("sample")
                    }
                    return result
                  }

                  /**
                   * @return The number of rows updated.
                   */
                  public fun updateMixed(
                    label: Label,
                    `value`: String?,
                    id: Long,
                  ): QueryResult<Long> {
                    val result = driver.execute(-1_086_633_643, ""${'"'}
                        |UPDATE sample
                        |SET label = ?,
                        |    note = ?
                        |WHERE id = ?
                        ""${'"'}.trimMargin(), 3) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))
                          bindString(parameterIndex++, value)
                          bindLong(parameterIndex++, id)
                        }
                    notifyQueries(-1_086_633_643) { emit ->
                      emit("sample")
                    }
                    return result
                  }

                  /**
                   * @return The number of rows updated.
                   */
                  public fun insertFromSelect(
                    id: Long,
                    note: String?,
                    source_id: Long,
                  ): QueryResult<Long> {
                    val result = driver.execute(2_070_133_276, ""${'"'}
                        |INSERT INTO sample (id, label, note)
                        |SELECT ?, label, ?
                        |FROM sample
                        |WHERE id = ?
                        ""${'"'}.trimMargin(), 3) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindLong(parameterIndex++, id)
                          bindString(parameterIndex++, note)
                          bindLong(parameterIndex++, source_id)
                        }
                    notifyQueries(2_070_133_276) { emit ->
                      emit("sample")
                    }
                    return result
                  }
                }
                """.trimIndent() + "\n"
        }

        test("generates Oracle predicate bind parameters as regression contrasts exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100),
                      PRIMARY KEY (id)
                    );

                    selectByPredicate:
                    SELECT id, label, note
                    FROM sample
                    WHERE id = ?
                      AND label = :label;

                    deleteByPredicate:
                    DELETE FROM sample
                    WHERE id = ?
                      AND label = :label;
                    """.trimIndent(),
                )

            generated.fileNames shouldContain "com/example/TestQueries.kt"
            generated.contentsByFile.getValue("com/example/TestQueries.kt") shouldBe
                """
                package com.example

                import app.cash.sqldelight.Query
                import app.cash.sqldelight.TransacterImpl
                import app.cash.sqldelight.db.QueryResult
                import app.cash.sqldelight.db.SqlCursor
                import app.cash.sqldelight.db.SqlDriver
                import app.cash.sqldelight.driver.jdbc.JdbcCursor
                import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
                import kotlin.Any
                import kotlin.Long
                import kotlin.String

                public class TestQueries(
                  driver: SqlDriver,
                  private val sampleAdapter: Sample.Adapter,
                ) : TransacterImpl(driver) {
                  public fun <T : Any> selectByPredicate(
                    id: Long,
                    label: Label,
                    mapper: (
                      id: Long,
                      label: Label,
                      note: String?,
                    ) -> T,
                  ): Query<T> = SelectByPredicateQuery(id, label) { cursor ->
                    check(cursor is JdbcCursor)
                    mapper(
                      cursor.getLong(0)!!,
                      sampleAdapter.labelAdapter.decode(cursor.getString(1)!!),
                      cursor.getString(2)
                    )
                  }

                  public fun selectByPredicate(id: Long, label: Label): Query<Sample> = selectByPredicate(id, label, ::Sample)

                  /**
                   * @return The number of rows updated.
                   */
                  public fun deleteByPredicate(id: Long, label: Label): QueryResult<Long> {
                    val result = driver.execute(-2_011_516_200, ""${'"'}
                        |DELETE FROM sample
                        |WHERE id = ?
                        |  AND label = ?
                        ""${'"'}.trimMargin(), 2) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindLong(parameterIndex++, id)
                          bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))
                        }
                    notifyQueries(-2_011_516_200) { emit ->
                      emit("sample")
                    }
                    return result
                  }

                  private inner class SelectByPredicateQuery<out T : Any>(
                    public val id: Long,
                    public val label: Label,
                    mapper: (SqlCursor) -> T,
                  ) : Query<T>(mapper) {
                    override fun addListener(listener: Query.Listener) {
                      driver.addListener("sample", listener = listener)
                    }

                    override fun removeListener(listener: Query.Listener) {
                      driver.removeListener("sample", listener = listener)
                    }

                    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-557_582_617, ""${'"'}
                    |SELECT id, label, note
                    |FROM sample
                    |WHERE id = ?
                    |  AND label = ?
                    ""${'"'}.trimMargin(), mapper, 2) {
                      check(this is JdbcPreparedStatement)
                      var parameterIndex = 0
                      bindLong(parameterIndex++, id)
                      bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))
                    }

                    override fun toString(): String = "Test.sq:selectByPredicate"
                  }
                }
                """.trimIndent() + "\n"
        }

        test("generates Oracle text pattern bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.DisplayName;

                    CREATE TABLE users (
                      id NUMBER(10, 0) NOT NULL,
                      display_name NVARCHAR2(100) AS DisplayName NOT NULL,
                      nickname NVARCHAR2(100) AS DisplayName,
                      PRIMARY KEY (id)
                    );

                    selectByUnicodeLike:
                    SELECT id, display_name, nickname
                    FROM users
                    WHERE display_name LIKEC :pattern ESCAPE :escape_char;

                    selectByRegexp:
                    SELECT id, display_name, nickname
                    FROM users
                    WHERE REGEXP_LIKE(display_name, :regex_pattern, :match_param);
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectByUnicodeLike(") shouldBe true
            queries.contains("pattern: String,") shouldBe true
            queries.contains("escape_char: String,") shouldBe true
            queries.contains("public fun <T : Any> selectByRegexp(") shouldBe true
            queries.contains("regex_pattern: String,") shouldBe true
            queries.contains("match_param: String?,") shouldBe true
            queries.contains("usersAdapter.display_nameAdapter.decode(cursor.getString(1)!!)") shouldBe true
            queries.contains("cursor.getString(2)?.let { usersAdapter.nicknameAdapter.decode(it) }") shouldBe true
            queries.contains("bindString(parameterIndex++, pattern)") shouldBe true
            queries.contains("bindString(parameterIndex++, escape_char)") shouldBe true
            queries.contains("bindString(parameterIndex++, regex_pattern)") shouldBe true
            queries.contains("bindString(parameterIndex++, match_param)") shouldBe true
            queries.contains("usersAdapter.display_nameAdapter.encode(pattern)") shouldBe false
            queries.contains("usersAdapter.display_nameAdapter.encode(regex_pattern)") shouldBe false
        }

        test("generates Oracle row-value predicate bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100),
                      PRIMARY KEY (id)
                    );

                    selectByTuple:
                    SELECT id, label, note
                    FROM sample
                    WHERE (id, label) = (:id, :label);

                    selectByTupleIn:
                    SELECT id, label, note
                    FROM sample
                    WHERE (id, label) IN ((:tuple_id, :tuple_label));
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectByTuple(") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("label: Label,") shouldBe true
            queries.contains("public fun <T : Any> selectByTupleIn(") shouldBe true
            queries.contains("tuple_id: Long,") shouldBe true
            queries.contains("tuple_label: Label,") shouldBe true
            queries.contains("|WHERE (id, label) = (?, ?)") shouldBe true
            queries.contains("|WHERE (id, label) IN ((?, ?))") shouldBe true
            queries.contains("bindLong(parameterIndex++, id)") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))") shouldBe true
            queries.contains("bindLong(parameterIndex++, tuple_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(tuple_label))") shouldBe true
        }

        test("generates Oracle pivot bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Region;

                    CREATE TABLE orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      region NVARCHAR2(16) AS Region NOT NULL,
                      fiscal_year NUMBER(10, 0) NOT NULL,
                      amount NUMBER(10, 2)
                    );

                    selectPivotByRegion:
                    SELECT pivoted.west_order_count
                    FROM orders PIVOT (
                      COUNT(*) AS order_count
                      FOR region IN (:region AS west)
                    ) pivoted;

                    selectCompositePivot:
                    SELECT pivoted.selected_order_count
                    FROM orders PIVOT (
                      COUNT(*) AS order_count
                      FOR (region, fiscal_year) IN ((:region, :year) AS selected)
                    ) pivoted;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun selectPivotByRegion(region: Region): Query<Long>") shouldBe true
            queries.contains("region: Region,") shouldBe true
            queries.contains("public fun selectCompositePivot(region: Region, year: Long): Query<Long>") shouldBe true
            queries.contains("year: Long,") shouldBe true
            queries.contains("|  FOR region IN (? AS west)") shouldBe true
            queries.contains("|  FOR (region, fiscal_year) IN ((?, ?) AS selected)") shouldBe true
            queries.contains("bindString(parameterIndex++, ordersAdapter.regionAdapter.encode(region))") shouldBe true
            queries.contains("bindLong(parameterIndex++, year)") shouldBe true
        }

        test("generates Oracle flashback bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      order_total NUMBER(10, 2),
                      created_at TIMESTAMP NOT NULL,
                      PRIMARY KEY (order_id)
                    );

                    selectAsOfScn:
                    SELECT order_id
                    FROM orders AS OF SCN :scn
                    WHERE order_id = :order_id;

                    selectAsOfTimestamp:
                    SELECT order_id
                    FROM orders AS OF TIMESTAMP :as_of_timestamp
                    WHERE created_at <= :created_before;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun selectAsOfScn(scn: Long, order_id: Long): Query<Long>") shouldBe true
            queries.contains("public fun selectAsOfTimestamp(as_of_timestamp: LocalDateTime, created_before: LocalDateTime): Query<Long>") shouldBe true
            queries.contains("|FROM orders AS OF SCN ?") shouldBe true
            queries.contains("|FROM orders AS OF TIMESTAMP ?") shouldBe true
            queries.contains("bindLong(parameterIndex++, scn)") shouldBe true
            queries.contains("bindLong(parameterIndex++, order_id)") shouldBe true
            queries.contains("bindObject(parameterIndex++, as_of_timestamp)") shouldBe true
            queries.contains("bindObject(parameterIndex++, created_before)") shouldBe true
        }

        test("generates Oracle SQL JSON passing bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE documents (
                      id NUMBER(10, 0) NOT NULL,
                      payload JSON NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectJsonItems:
                    SELECT d.id, jt.line_number, jt.item_id, jt.item_name
                    FROM documents d,
                      JSON_TABLE(
                        d.payload,
                        '${'$'}.items[*]?(@.category == ${'$'}category && @.price >= ${'$'}minPrice)'
                        PASSING
                          CAST(:category AS VARCHAR2(30)) AS category,
                          CAST(:min_price AS NUMBER(10, 2)) AS minPrice
                        COLUMNS (
                          line_number FOR ORDINALITY,
                          item_id NUMBER(10, 0) PATH '${'$'}.id',
                          item_name VARCHAR2(100) PATH '${'$'}.name'
                        )
                      ) jt
                    WHERE JSON_EXISTS(
                      d.payload,
                      '${'$'}?(@.tenantId == ${'$'}tenantId)'
                      PASSING CAST(:tenant_id AS NUMBER(10, 0)) AS tenantId
                    );
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectJsonItems(") shouldBe true
            queries.contains("category: String,") shouldBe true
            queries.contains("min_price: BigDecimal,") shouldBe true
            queries.contains("tenant_id: Long,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("line_number: Long,") shouldBe true
            queries.contains("item_id: Long?,") shouldBe true
            queries.contains("item_name: String?,") shouldBe true
            queries.contains("|    PASSING") shouldBe true
            queries.contains("|      CAST(? AS VARCHAR2(30)) AS category,") shouldBe true
            queries.contains("|      CAST(? AS NUMBER(10, 2)) AS minPrice") shouldBe true
            queries.contains("|  PASSING CAST(? AS NUMBER(10, 0)) AS tenantId") shouldBe true
            queries.contains("bindString(parameterIndex++, category)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_price)") shouldBe true
            queries.contains("bindLong(parameterIndex++, tenant_id)") shouldBe true
        }

        test("generates Oracle SQL XML passing bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE departments (
                      id NUMBER(10, 0) NOT NULL,
                      warehouse_spec XMLTYPE,
                      PRIMARY KEY (id)
                    );

                    selectWarehouse:
                    SELECT d.id, x.line_number, x.area
                    FROM departments d,
                      XMLTABLE(
                        '/Warehouse'
                        PASSING BY VALUE XMLTYPE(CAST(:warehouse_xml AS CLOB)) AS "doc"
                        COLUMNS
                          line_number FOR ORDINALITY,
                          area NUMBER(10, 2) PATH 'Area'
                      ) x
                    WHERE d.id = :department_id
                      AND XMLEXISTS(
                        '/Warehouse[Area >= ${'$'}minArea]'
                        PASSING d.warehouse_spec, CAST(:min_area AS NUMBER(10, 2)) AS minArea
                      );
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectWarehouse(") shouldBe true
            queries.contains("warehouse_xml: String,") shouldBe true
            queries.contains("department_id: Long,") shouldBe true
            queries.contains("min_area: BigDecimal,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("line_number: Long,") shouldBe true
            queries.contains("area: BigDecimal?,") shouldBe true
            queries.contains("|    PASSING BY VALUE XMLTYPE(CAST(? AS CLOB)) AS \"doc\"") shouldBe true
            queries.contains("|    PASSING d.warehouse_spec, CAST(? AS NUMBER(10, 2)) AS minArea") shouldBe true
            queries.contains("bindString(parameterIndex++, warehouse_xml)") shouldBe true
            queries.contains("bindLong(parameterIndex++, department_id)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_area)") shouldBe true
        }

        test("generates Oracle DML returning input bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      order_total NUMBER(10, 2),
                      order_name VARCHAR2(64),
                      PRIMARY KEY (order_id)
                    );

                    insertReturning:
                    INSERT INTO orders (order_id, order_total, order_name)
                    VALUES (:order_id, :order_total, :order_name)
                    RETURNING order_id, order_name INTO :returned_id, :returned_name;

                    updateReturning:
                    UPDATE orders
                    SET order_total = :order_total
                    WHERE order_id = :order_id
                    RETURNING OLD order_total, NEW order_total INTO :old_total, :new_total;

                    deleteReturning:
                    DELETE FROM orders
                    WHERE order_id = :order_id
                    RETURNING order_id, order_name BULK COLLECT INTO :deleted_ids, :deleted_names;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun insertReturning(") shouldBe true
            queries.contains("order_id: Long,") shouldBe true
            queries.contains("order_total: BigDecimal?,") shouldBe true
            queries.contains("order_name: String?,") shouldBe true
            queries.contains("): QueryResult<Long> {") shouldBe true
            queries.contains("|VALUES (?, ?, ?)") shouldBe true
            queries.contains("|RETURNING order_id, order_name INTO :returned_id, :returned_name") shouldBe true
            queries.contains(", 3) {") shouldBe true
            queries.contains("bindLong(parameterIndex++, order_id)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, order_total)") shouldBe true
            queries.contains("bindString(parameterIndex++, order_name)") shouldBe true
            queries.contains("returned_id") shouldBe true
            queries.contains("bindString(parameterIndex++, returned_id)") shouldBe false
            queries.contains("public fun updateReturning(order_total: BigDecimal?, order_id: Long): QueryResult<Long>") shouldBe true
            queries.contains("RETURNING OLD order_total, NEW order_total INTO :old_total, :new_total") shouldBe true
            queries.contains(", 2) {") shouldBe true
            queries.contains("public fun deleteReturning(order_id: Long): QueryResult<Long>") shouldBe true
            queries.contains("RETURNING order_id, order_name BULK COLLECT INTO :deleted_ids, :deleted_names") shouldBe true
            queries.contains(", 1) {") shouldBe true
        }

        test("generates Oracle merge bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100),
                      PRIMARY KEY (id)
                    );

                    mergeUpsert:
                    MERGE INTO sample target
                    USING sample source
                    ON (target.id = :id AND source.id = :source_id)
                    WHEN MATCHED THEN
                      UPDATE SET target.label = :label,
                                 target.note = :note
                    WHEN NOT MATCHED THEN
                      INSERT (id, label, note)
                      VALUES (:insert_id, :insert_label, :insert_note);
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun mergeUpsert(") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("source_id: Long,") shouldBe true
            queries.contains("label: Label,") shouldBe true
            queries.contains("note: String?,") shouldBe true
            queries.contains("insert_id: Long,") shouldBe true
            queries.contains("insert_label: Label,") shouldBe true
            queries.contains("insert_note: String?,") shouldBe true
            queries.contains("|ON (target.id = ? AND source.id = ?)") shouldBe true
            queries.contains("|  UPDATE SET target.label = ?,") shouldBe true
            queries.contains("|             target.note = ?") shouldBe true
            queries.contains("|  VALUES (?, ?, ?)") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(insert_label))") shouldBe true
        }

        test("generates Oracle multi-table insert bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100)
                    );

                    insertAll:
                    INSERT ALL
                      INTO sample (id, label, note)
                      VALUES (:sample_id, :sample_label, :sample_note)
                      INTO sample (id, label, note)
                      VALUES (:audit_id, :audit_label, :audit_note)
                    SELECT 1 FROM dual;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun insertAll(") shouldBe true
            queries.contains("sample_id: Long,") shouldBe true
            queries.contains("sample_label: Label,") shouldBe true
            queries.contains("sample_note: String?,") shouldBe true
            queries.contains("audit_id: Long,") shouldBe true
            queries.contains("audit_label: Label,") shouldBe true
            queries.contains("audit_note: String?,") shouldBe true
            queries.contains("|  VALUES (?, ?, ?)") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(sample_label))") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(audit_label))") shouldBe true
        }

        test("generates Oracle insert set bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100)
                    );

                    insertSet:
                    INSERT INTO sample
                    SET id = :id,
                        label = :label,
                        note = :note;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun insertSet(") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("label: Label,") shouldBe true
            queries.contains("note: String?,") shouldBe true
            queries.contains("|SET id = ?,") shouldBe true
            queries.contains("|    label = ?,") shouldBe true
            queries.contains("|    note = ?") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))") shouldBe true
        }

        test("generates Oracle row-value update bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100)
                    );

                    updateTuple:
                    UPDATE sample
                    SET (id, label, note) = (:id, :label, :note)
                    WHERE id = :old_id;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun updateTuple(") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("label: Label,") shouldBe true
            queries.contains("note: String?,") shouldBe true
            queries.contains("old_id: Long,") shouldBe true
            queries.contains("|SET (id, label, note) = (?, ?, ?)") shouldBe true
            queries.contains("|WHERE id = ?") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))") shouldBe true
        }

        test("generates Oracle CTE column alias custom types exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100)
                    );

                    selectFromAliasedCte:
                    WITH aliased_sample (sample_id, sample_label, sample_note) AS (
                      SELECT id, label, note
                      FROM sample
                    )
                    SELECT sample_id, sample_label, sample_note
                    FROM aliased_sample
                    WHERE sample_label = :label;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectFromAliasedCte(") shouldBe true
            queries.contains("label: Label,") shouldBe true
            queries.contains("sample_id: Long,") shouldBe true
            queries.contains("sample_label: Label,") shouldBe true
            queries.contains("sample_note: String?,") shouldBe true
            queries.contains("sampleAdapter.labelAdapter.decode(cursor.getString(1)!!)") shouldBe true
            queries.contains("bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(label))") shouldBe true
        }

        test("generates SQLDelight value type row and variable arguments exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE sample (
                      id NUMBER(10, 0) AS VALUE NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      note VARCHAR2(100),
                      PRIMARY KEY (id)
                    );

                    insertRow:
                    INSERT INTO sample
                    VALUES ?;

                    selectByIds:
                    SELECT id, label, note
                    FROM sample
                    WHERE id IN ?;
                    """.trimIndent(),
                )

            generated.fileNames shouldContainAll
                listOf(
                    "com/example/Sample.kt",
                    "com/example/TestQueries.kt",
                )
            generated.contentsByFile.getValue("com/example/Sample.kt") shouldBe
                """
                package com.example

                import app.cash.sqldelight.ColumnAdapter
                import kotlin.Long
                import kotlin.String
                import kotlin.jvm.JvmInline

                public data class Sample(
                  public val id: Id,
                  public val label: Label,
                  public val note: String?,
                ) {
                  public class Adapter(
                    public val labelAdapter: ColumnAdapter<Label, String>,
                  )

                  @JvmInline
                  public value class Id(
                    public val id: Long,
                  )
                }
                """.trimIndent() + "\n"
            generated.contentsByFile.getValue("com/example/TestQueries.kt") shouldBe
                """
                package com.example

                import app.cash.sqldelight.Query
                import app.cash.sqldelight.TransacterImpl
                import app.cash.sqldelight.db.QueryResult
                import app.cash.sqldelight.db.SqlCursor
                import app.cash.sqldelight.db.SqlDriver
                import app.cash.sqldelight.driver.jdbc.JdbcCursor
                import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
                import kotlin.Any
                import kotlin.Long
                import kotlin.String
                import kotlin.collections.Collection

                public class TestQueries(
                  driver: SqlDriver,
                  private val sampleAdapter: Sample.Adapter,
                ) : TransacterImpl(driver) {
                  public fun <T : Any> selectByIds(id: Collection<Sample.Id>, mapper: (
                    id: Sample.Id,
                    label: Label,
                    note: String?,
                  ) -> T): Query<T> = SelectByIdsQuery(id) { cursor ->
                    check(cursor is JdbcCursor)
                    mapper(
                      Sample.Id(cursor.getLong(0)!!),
                      sampleAdapter.labelAdapter.decode(cursor.getString(1)!!),
                      cursor.getString(2)
                    )
                  }

                  public fun selectByIds(id: Collection<Sample.Id>): Query<Sample> = selectByIds(id, ::Sample)

                  /**
                   * @return The number of rows updated.
                   */
                  public fun insertRow(sample: Sample): QueryResult<Long> {
                    val result = driver.execute(745_731_524, ""${'"'}
                        |INSERT INTO sample
                        |VALUES ?
                        ""${'"'}.trimMargin(), 3) {
                          check(this is JdbcPreparedStatement)
                          var parameterIndex = 0
                          bindLong(parameterIndex++, sample.id.id)
                          bindString(parameterIndex++, sampleAdapter.labelAdapter.encode(sample.label))
                          bindString(parameterIndex++, sample.note)
                        }
                    notifyQueries(745_731_524) { emit ->
                      emit("sample")
                    }
                    return result
                  }

                  private inner class SelectByIdsQuery<out T : Any>(
                    public val id: Collection<Sample.Id>,
                    mapper: (SqlCursor) -> T,
                  ) : Query<T>(mapper) {
                    override fun addListener(listener: Query.Listener) {
                      driver.addListener("sample", listener = listener)
                    }

                    override fun removeListener(listener: Query.Listener) {
                      driver.removeListener("sample", listener = listener)
                    }

                    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
                      val idIndexes = createArguments(count = id.size)
                      return driver.executeQuery(null, ""${'"'}
                          |SELECT id, label, note
                          |FROM sample
                          |WHERE id IN ${'$'}idIndexes
                          ""${'"'}.trimMargin(), mapper, id.size) {
                            check(this is JdbcPreparedStatement)
                            var parameterIndex = 0
                            id.forEach { id_ ->
                              bindLong(parameterIndex++, id_.id)
                            }
                          }
                    }

                    override fun toString(): String = "Test.sq:selectByIds"
                  }
                }
                """.trimIndent() + "\n"
        }
    })

private data class CodegenResult(
    val fileNames: List<String>,
    val contentsByFile: Map<String, String>,
)

private fun generateOracleSqlDelight(
    sql: String,
    fileName: String = "Test.sq",
): CodegenResult {
    val root = Files.createTempDirectory("sqldelight-oracle-codegen-test").toFile()
    val sourceDirectory = File(root, "com/example").apply { mkdirs() }
    File(sourceDirectory, fileName).writeText(sql)

    val compilationUnit = OracleCodegenTestCompilationUnit(File(root, "output"))
    val environment =
        SqlDelightEnvironment(
            sourceFolders = listOf(root),
            dependencyFolders = emptyList(),
            properties =
                OracleCodegenTestDatabaseProperties(
                    rootDirectory = root,
                    compilationUnit = compilationUnit,
                ),
            dialect = OracleDialect(),
            verifyMigrations = true,
            moduleName = "oracle-codegen-test",
            compilationUnit = compilationUnit,
        )

    LanguageParserDefinitions.INSTANCE.forLanguage(SqlDelightLanguage).createParser(environment.project)
    LanguageParserDefinitions.INSTANCE.forLanguage(MigrationLanguage).createParser(environment.project)

    val annotationErrors = mutableListOf<String>()
    environment.annotate(listOf(OptimisticLockCompilerAnnotator())) { element, message ->
        annotationErrors += "${element.containingFile.name}: $message"
    }
    annotationErrors shouldBe emptyList()

    val compilerErrors = mutableListOf<String>()
    val status = environment.generateSqlDelightFiles { message -> compilerErrors += message }
    status::class.simpleName shouldBe "Success"

    val generatedFiles =
        compilationUnit.outputDirectoryFile
            .walkTopDown()
            .filter { file -> file.isFile }
            .associate { file ->
                file.relativeTo(compilationUnit.outputDirectoryFile).invariantSeparatorsPath to file.readText()
            }
    val files = generatedFiles.toSortedMap()

    return CodegenResult(
        fileNames = files.keys.toList(),
        contentsByFile = files,
    )
}

private data class OracleCodegenTestCompilationUnit(
    override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit {
    override val name: String = "test"
    override val sourceFolders: Set<SqlDelightSourceFolder> = emptySet()
}

private data class OracleCodegenTestDatabaseProperties(
    override val rootDirectory: File,
    private val compilationUnit: SqlDelightCompilationUnit,
) : SqlDelightDatabaseProperties {
    override val packageName: String = "com.example"
    override val className: String = "TestDatabase"
    override val dependencies: List<SqlDelightDatabaseName> = emptyList()
    override val compilationUnits: List<SqlDelightCompilationUnit> = listOf(compilationUnit)
    override val deriveSchemaFromMigrations: Boolean = false
    override val generateAsync: Boolean = false
    override val expandSelectStar: Boolean = true
    override val treatNullAsUnknownForEquality: Boolean = false
}
