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

        test("generates Oracle function argument bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Label;

                    CREATE TABLE function_bind_sample (
                      id NUMBER(10, 0) NOT NULL,
                      label NVARCHAR2(50) AS Label NOT NULL,
                      nullable_label NVARCHAR2(50) AS Label,
                      amount NUMBER(10, 2),
                      created_at TIMESTAMP,
                      payload JSON,
                      PRIMARY KEY (id)
                    );

                    selectFunctionBinds:
                    SELECT COALESCE(nullable_label, :fallback) AS coalesced,
                           NVL(nullable_label, ?) AS defaulted,
                           NVL2(nullable_label, :present, label) AS selected,
                           LEAST(label, :ceiling) AS least_label,
                           UPPER(:upper_value) AS upper_value,
                           UPPER(CAST(:cast_upper_value AS VARCHAR2(50))) AS cast_upper_value,
                           TO_DATE(:date_value, 'YYYY-MM-DD') AS converted_date,
                           JSON_DATAGUIDE(payload, :json_format, :json_pretty) AS json_dataguide,
                           NTH_VALUE(amount, :position) OVER (ORDER BY id) AS nth_amount
                    FROM function_bind_sample
                    WHERE label = COALESCE(:predicate_fallback, nullable_label);
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("fallback: Label?,") shouldBe true
            queries.contains("nullable_label: Label?,") shouldBe true
            queries.contains("present: Label,") shouldBe true
            queries.contains("ceiling: Label,") shouldBe true
            queries.contains("upper_value: String,") shouldBe true
            queries.contains("cast_upper_value: String,") shouldBe true
            queries.contains("date_value: String,") shouldBe true
            queries.contains("json_format: String,") shouldBe true
            queries.contains("json_pretty: String,") shouldBe true
            queries.contains("position: Long,") shouldBe true
            queries.contains("predicate_fallback: Label?,") shouldBe true
            queries.contains("function_bind_sampleAdapter.nullable_labelAdapter.encode(it)") shouldBe true
            queries.contains("function_bind_sampleAdapter.labelAdapter.encode(present)") shouldBe true
            queries.contains("function_bind_sampleAdapter.labelAdapter.encode(ceiling)") shouldBe true
            queries.contains("predicate_fallback?.let { function_bind_sampleAdapter.nullable_labelAdapter.encode(it) }") shouldBe true
            queries.contains("bindString(parameterIndex++, upper_value)") shouldBe true
            queries.contains("bindString(parameterIndex++, cast_upper_value)") shouldBe true
            queries.contains("bindString(parameterIndex++, date_value)") shouldBe true
            queries.contains("bindString(parameterIndex++, json_format)") shouldBe true
            queries.contains("bindString(parameterIndex++, json_pretty)") shouldBe true
            queries.contains("bindLong(parameterIndex++, position)") shouldBe true
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

        test("generates Oracle collate bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.DisplayName;

                    CREATE TABLE contacts (
                      id NUMBER(10, 0) NOT NULL,
                      display_name NVARCHAR2(80) AS DisplayName NOT NULL,
                      nickname VARCHAR2(80),
                      PRIMARY KEY (id)
                    );

                    selectCaseInsensitiveContacts:
                    SELECT display_name COLLATE BINARY_CI AS normalized_name,
                           nickname COLLATE BINARY_CI AS normalized_nickname
                    FROM contacts
                    WHERE display_name COLLATE BINARY_CI = :display_name
                      AND nickname COLLATE USING_NLS_COMP LIKE :nickname_pattern
                    ORDER BY display_name COLLATE GENERIC_M;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectCaseInsensitiveContacts(") shouldBe true
            queries.contains("display_name: DisplayName,") shouldBe true
            queries.contains("nickname_pattern: String,") shouldBe true
            queries.contains("normalized_name: DisplayName,") shouldBe true
            queries.contains("normalized_nickname: String?) -> T,") shouldBe true
            queries.contains("contactsAdapter.display_nameAdapter.decode(cursor.getString(0)!!)") shouldBe true
            queries.contains("|SELECT display_name COLLATE BINARY_CI AS normalized_name,") shouldBe true
            queries.contains("|       nickname COLLATE BINARY_CI AS normalized_nickname") shouldBe true
            queries.contains("|WHERE display_name COLLATE BINARY_CI = ?") shouldBe true
            queries.contains("|  AND nickname COLLATE USING_NLS_COMP LIKE ?") shouldBe true
            queries.contains("|ORDER BY display_name COLLATE GENERIC_M") shouldBe true
            queries.contains("bindString(parameterIndex++, contactsAdapter.display_nameAdapter.encode(display_name))") shouldBe true
            queries.contains("bindString(parameterIndex++, nickname_pattern)") shouldBe true
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

        test("generates Oracle unpivot bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import com.example.Region;

                    CREATE TABLE order_metrics (
                      id NUMBER(10, 0) NOT NULL,
                      region NVARCHAR2(16) AS Region NOT NULL,
                      booked_total NUMBER(10, 2),
                      shipped_total NUMBER(10, 2)
                    );

                    selectUnpivotMetrics:
                    SELECT u.region, u.metric_name, u.metric_value
                    FROM order_metrics UNPIVOT INCLUDE NULLS (
                      metric_value
                      FOR metric_name IN (booked_total AS 'BOOKED', shipped_total AS 'SHIPPED')
                    ) u
                    WHERE u.region = :region
                      AND u.metric_value >= :min_value;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectUnpivotMetrics(") shouldBe true
            queries.contains("region: Region,") shouldBe true
            queries.contains("min_value: BigDecimal?,") shouldBe true
            queries.contains("metric_name: String,") shouldBe true
            queries.contains("metric_value: BigDecimal?,") shouldBe true
            queries.contains("order_metricsAdapter.regionAdapter.decode(cursor.getString(0)!!)") shouldBe true
            queries.contains("cursor.getString(1)!!") shouldBe true
            queries.contains("cursor.getBigDecimal(2)") shouldBe true
            queries.contains("|FROM order_metrics UNPIVOT INCLUDE NULLS (") shouldBe true
            queries.contains("|  metric_value") shouldBe true
            queries.contains("|  FOR metric_name IN (booked_total AS 'BOOKED', shipped_total AS 'SHIPPED')") shouldBe true
            queries.contains("|) u") shouldBe true
            queries.contains("|WHERE u.region = ?") shouldBe true
            queries.contains("|  AND u.metric_value >= ?") shouldBe true
            queries.contains("bindString(parameterIndex++, order_metricsAdapter.regionAdapter.encode(region))") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_value)") shouldBe true
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
            queries.contains(
                "public fun selectAsOfTimestamp(as_of_timestamp: LocalDateTime, created_before: LocalDateTime): Query<Long>",
            ) shouldBe
                true
            queries.contains("|FROM orders AS OF SCN ?") shouldBe true
            queries.contains("|FROM orders AS OF TIMESTAMP ?") shouldBe true
            queries.contains("bindLong(parameterIndex++, scn)") shouldBe true
            queries.contains("bindLong(parameterIndex++, order_id)") shouldBe true
            queries.contains("bindObject(parameterIndex++, as_of_timestamp)") shouldBe true
            queries.contains("bindObject(parameterIndex++, created_before)") shouldBe true
        }

        test("generates Oracle listagg within group bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE employees (
                      id NUMBER(10, 0) NOT NULL,
                      department_id NUMBER(10, 0) NOT NULL,
                      last_name VARCHAR2(100) NOT NULL,
                      hire_date DATE NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectDepartmentNames:
                    SELECT department_id,
                           LISTAGG(
                             last_name,
                             CAST(:separator AS VARCHAR2(10))
                             ON OVERFLOW TRUNCATE CAST(:overflow_marker AS VARCHAR2(10)) WITH COUNT
                           ) WITHIN GROUP (ORDER BY hire_date, last_name) AS employee_names
                    FROM employees
                    WHERE last_name LIKE :name_pattern
                    GROUP BY department_id;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectDepartmentNames(") shouldBe true
            queries.contains("separator: String,") shouldBe true
            queries.contains("overflow_marker: String,") shouldBe true
            queries.contains("name_pattern: String,") shouldBe true
            queries.contains("department_id: Long,") shouldBe true
            queries.contains("employee_names: String?) -> T,") shouldBe true
            queries.contains("|       LISTAGG(") shouldBe true
            queries.contains("|         CAST(? AS VARCHAR2(10))") shouldBe true
            queries.contains("|         ON OVERFLOW TRUNCATE CAST(? AS VARCHAR2(10)) WITH COUNT") shouldBe true
            queries.contains("|       ) WITHIN GROUP (ORDER BY hire_date, last_name) AS employee_names") shouldBe true
            queries.contains("|WHERE last_name LIKE ?") shouldBe true
            queries.contains("bindString(parameterIndex++, separator)") shouldBe true
            queries.contains("bindString(parameterIndex++, overflow_marker)") shouldBe true
            queries.contains("bindString(parameterIndex++, name_pattern)") shouldBe true
        }

        test("generates Oracle ordered percentile aggregate bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE sales (
                      id NUMBER(10, 0) NOT NULL,
                      region VARCHAR2(20) NOT NULL,
                      amount NUMBER(10, 2) NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectPercentileMetrics:
                    SELECT region,
                           PERCENTILE_CONT(CAST(:continuous_percentile AS NUMBER(3, 2)))
                             WITHIN GROUP (ORDER BY amount DESC) AS continuous_amount,
                           APPROX_PERCENTILE(amount DETERMINISTIC, CAST(:approx_percentile AS NUMBER(3, 2)))
                             WITHIN GROUP (ORDER BY amount DESC) AS approximate_amount,
                           RANK(CAST(:hypothetical_amount AS NUMBER(10, 2)))
                             WITHIN GROUP (ORDER BY amount DESC) AS hypothetical_rank
                    FROM sales
                    WHERE amount >= :minimum_amount
                    GROUP BY region;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectPercentileMetrics(") shouldBe true
            queries.contains("continuous_percentile: BigDecimal,") shouldBe true
            queries.contains("approx_percentile: BigDecimal,") shouldBe true
            queries.contains("hypothetical_amount: BigDecimal,") shouldBe true
            queries.contains("minimum_amount: BigDecimal,") shouldBe true
            queries.contains("region: String,") shouldBe true
            queries.contains("continuous_amount: BigDecimal?,") shouldBe true
            queries.contains("approximate_amount: BigDecimal?,") shouldBe true
            queries.contains("hypothetical_rank: Long,") shouldBe true
            queries.contains("|       PERCENTILE_CONT(CAST(? AS NUMBER(3, 2)))") shouldBe true
            queries.contains("|       APPROX_PERCENTILE(amount DETERMINISTIC, CAST(? AS NUMBER(3, 2)))") shouldBe true
            queries.contains("|       RANK(CAST(? AS NUMBER(10, 2)))") shouldBe true
            queries.contains("|WHERE amount >= ?") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, continuous_percentile)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, approx_percentile)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, hypothetical_amount)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, minimum_amount)") shouldBe true
        }

        test("generates Oracle ANY_VALUE expression operands exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import com.example.Amount;

                    CREATE TABLE any_value_bind (
                      amount NUMBER(10, 2) NOT NULL,
                      nullable_amount NUMBER(10, 2),
                      adapted_amount NUMBER(10, 2) AS Amount NOT NULL
                    );

                    anyValueExpressions:
                    SELECT ANY_VALUE(CAST(? AS NUMBER)) AS positional_value,
                      ANY_VALUE(CAST(:amount_bind AS NUMBER(10, 2))) AS named_value,
                      ANY_VALUE(CAST(1 AS NUMBER)) AS cast_literal_value,
                      ANY_VALUE(amount + 1) AS scalar_value,
                      ANY_VALUE(nullable_amount) AS nullable_value,
                      ANY_VALUE(adapted_amount) AS adapted_value
                    FROM any_value_bind;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> anyValueExpressions(") shouldBe true
            queries.contains("`value`: BigDecimal,") shouldBe true
            queries.contains("amount_bind: BigDecimal,") shouldBe true
            queries.contains("positional_value: BigDecimal?,") shouldBe true
            queries.contains("named_value: BigDecimal?,") shouldBe true
            queries.contains("cast_literal_value: BigDecimal?,") shouldBe true
            queries.contains("scalar_value: BigDecimal?,") shouldBe true
            queries.contains("nullable_value: BigDecimal?,") shouldBe true
            queries.contains("adapted_value: Amount?,") shouldBe true
            queries.contains("|SELECT ANY_VALUE(CAST(? AS NUMBER)) AS positional_value,") shouldBe true
            queries.contains("|  ANY_VALUE(CAST(? AS NUMBER(10, 2))) AS named_value,") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, value)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, amount_bind)") shouldBe true
            queries.contains("any_value_bindAdapter.adapted_amountAdapter.decode") shouldBe true
        }

        test("generates Oracle 26ai datetime bitmap and every result types exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE audit_functions (
                      id NUMBER(10) NOT NULL,
                      created_at DATE NOT NULL,
                      bitmap_value BLOB
                    );

                    dateDifference:
                    SELECT DATEDIFF('DAY', created_at, created_at) AS value
                    FROM audit_functions
                    WHERE id > :minimum_id;

                    bitmapSummary:
                    SELECT BITMAP_CONSTRUCT_AGG(id) AS detail,
                           BITMAP_COUNT(bitmap_value) AS cardinality,
                           BITMAP_OR_AGG(bitmap_value) AS combined
                    FROM audit_functions;

                    everyAbove:
                    SELECT EVERY(id > :minimum_id) AS value
                    FROM audit_functions;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun dateDifference(minimum_id: Long): Query<BigDecimal>") shouldBe true
            queries.contains("detail: ByteArray?") shouldBe true
            queries.contains("cardinality: BigDecimal?") shouldBe true
            queries.contains("combined: ByteArray?") shouldBe true
            queries.contains("mapper: (value_: Boolean?) -> T") shouldBe true
            queries.contains("bindLong(parameterIndex++, minimum_id)") shouldBe true
            queries.contains("cursor.getBytes(0)") shouldBe true
            queries.contains("cursor.getBigDecimal(1)") shouldBe true
            queries.contains("cursor.getBoolean(0)") shouldBe true
        }

        test("generates Oracle statistical aggregate results and bind order exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE stats_sample (
                      group_id NUMBER(10) NOT NULL,
                      sample_value NUMBER(10, 2),
                      sample_value_2 NUMBER(10, 2)
                    );

                    tTests:
                    SELECT STATS_T_TEST_ONE(sample_value, 0, 'TWO_SIDED_SIG') AS one_sample,
                           STATS_T_TEST_PAIRED(sample_value, sample_value_2, 'TWO_SIDED_SIG') AS paired,
                           STATS_T_TEST_INDEP(group_id, sample_value, 'TWO_SIDED_SIG') AS independent,
                           STATS_T_TEST_INDEPU(group_id, sample_value, 'TWO_SIDED_SIG') AS unequal_variance
                    FROM stats_sample
                    WHERE group_id > :minimum_group;

                    ksTest:
                    SELECT STATS_KS_TEST(group_id, sample_value, 'TWO_SIDED_SIG') AS value
                    FROM stats_sample
                    WHERE group_id > ?;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("one_sample: BigDecimal?") shouldBe true
            queries.contains("paired: BigDecimal?") shouldBe true
            queries.contains("independent: BigDecimal?") shouldBe true
            queries.contains("unequal_variance: BigDecimal?") shouldBe true
            queries.contains("mapper: (value_: BigDecimal?) -> T") shouldBe true
            queries.contains("bindLong(parameterIndex++, minimum_group)") shouldBe true
            queries.contains("cursor.getBigDecimal(0)") shouldBe true
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

        test("generates Oracle end user context JSON path queries exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    username:
                    SELECT ORA_END_USER_CONTEXT.username AS value
                    FROM dual;

                    tokenIssuer:
                    SELECT ORA_END_USER_CONTEXT.USER.TOKEN.iss AS value
                    FROM dual
                    WHERE ORA_END_USER_CONTEXT.USER.TOKEN.iss IS NOT NULL;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> username(mapper: (value_: String?) -> T)") shouldBe true
            queries.contains("public fun <T : Any> tokenIssuer(mapper: (value_: String?) -> T)") shouldBe true
            queries.contains("|SELECT ORA_END_USER_CONTEXT.username AS value") shouldBe true
            queries.contains("|SELECT ORA_END_USER_CONTEXT.USER.TOKEN.iss AS value") shouldBe true
            queries.contains("cursor.getString(0)") shouldBe true
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

        test("generates Oracle XMLELEMENT EVALNAME bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE xml_bind (
                      label VARCHAR2(100) NOT NULL,
                      element_name VARCHAR2(100) NOT NULL
                    );

                    xmlElementNames:
                    SELECT XMLELEMENT(EVALNAME ?, label) AS positional_value,
                      XMLELEMENT(EVALNAME :element_name, XMLATTRIBUTES(label AS "label"), label) AS named_value,
                      XMLELEMENT(EVALNAME CAST(:cast_name AS VARCHAR2(30)), label) AS cast_value,
                      XMLELEMENT(EVALNAME element_name, label) AS column_value,
                      XMLELEMENT(NAME "fixed", label) AS fixed_value
                    FROM xml_bind;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> xmlElementNames(") shouldBe true
            queries.contains("`value`: String,") shouldBe true
            queries.contains("element_name: String,") shouldBe true
            queries.contains("cast_name: String,") shouldBe true
            queries.contains("positional_value: String,") shouldBe true
            queries.contains("named_value: String,") shouldBe true
            queries.contains("cast_value: String,") shouldBe true
            queries.contains("|SELECT XMLELEMENT(EVALNAME ?, label) AS positional_value,") shouldBe true
            queries.contains("|  XMLELEMENT(EVALNAME ?, XMLATTRIBUTES(label AS \"label\"), label) AS named_value,") shouldBe true
            queries.contains("|  XMLELEMENT(EVALNAME CAST(? AS VARCHAR2(30)), label) AS cast_value,") shouldBe true
            queries.contains("bindString(parameterIndex++, value)") shouldBe true
            queries.contains("bindString(parameterIndex++, element_name)") shouldBe true
            queries.contains("bindString(parameterIndex++, cast_name)") shouldBe true
        }

        test("generates Oracle vector function bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE documents (
                      id NUMBER(10, 0) NOT NULL,
                      body CLOB,
                      embedding VECTOR(3, FLOAT32),
                      PRIMARY KEY (id)
                    );

                    selectNearestDocuments:
                    SELECT id,
                           embedding,
                           FROM_VECTOR(embedding) AS serialized_embedding,
                           VECTOR_DIMS(embedding) AS embedding_dimensions,
                           VECTOR_DIMENSION_COUNT(embedding) AS embedding_dimension_count,
                           VECTOR_DISTANCE(embedding, TO_VECTOR(:query_vector, 3, FLOAT32), COSINE) AS cosine_distance,
                           embedding <-> TO_VECTOR('[1,2,3]', 3, FLOAT32) AS euclidean_distance
                    FROM documents
                    WHERE id > :minimum_id
                    ORDER BY embedding <=> TO_VECTOR('[3,2,1]', 3, FLOAT32);
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectNearestDocuments(") shouldBe true
            queries.contains("query_vector: String,") shouldBe true
            queries.contains("minimum_id: Long,") shouldBe true
            queries.contains("embedding: String?,") shouldBe true
            queries.contains("serialized_embedding: String?,") shouldBe true
            queries.contains("embedding_dimensions: String?,") shouldBe true
            queries.contains("embedding_dimension_count: Long?,") shouldBe true
            queries.contains("cosine_distance: Double?,") shouldBe true
            queries.contains("euclidean_distance: Double?,") shouldBe true
            queries.contains("|       VECTOR_DISTANCE(embedding, TO_VECTOR(?, 3, FLOAT32), COSINE) AS cosine_distance,") shouldBe true
            queries.contains("|       embedding <-> TO_VECTOR('[1,2,3]', 3, FLOAT32) AS euclidean_distance") shouldBe true
            queries.contains("|WHERE id > ?") shouldBe true
            queries.contains("|ORDER BY embedding <=> TO_VECTOR('[3,2,1]', 3, FLOAT32)") shouldBe true
            queries.contains("bindString(parameterIndex++, query_vector)") shouldBe true
            queries.contains("bindLong(parameterIndex++, minimum_id)") shouldBe true
        }

        test("generates Oracle 26ai conversion bind parameters and result types exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE conversion_input (
                      id NUMBER(10) NOT NULL
                    );

                    toBooleanPositional:
                    SELECT TO_BOOLEAN(?) AS value FROM conversion_input;

                    toBooleanNamed:
                    SELECT TO_BOOLEAN(:flag) AS value FROM conversion_input;

                    toUtcPositional:
                    SELECT TO_UTC_TIMESTAMP_TZ(?) AS value FROM conversion_input;

                    toUtcNamed:
                    SELECT TO_UTC_TIMESTAMP_TZ(:iso) AS value FROM conversion_input;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun toBooleanPositional(value_: String)") shouldBe true
            queries.contains("public fun toBooleanNamed(flag: String)") shouldBe true
            queries.contains("public fun toUtcPositional(value_: String)") shouldBe true
            queries.contains("public fun toUtcNamed(iso: String)") shouldBe true
            queries.contains("bindString(parameterIndex++, value)") shouldBe true
            queries.contains("bindString(parameterIndex++, flag)") shouldBe true
            queries.contains("bindString(parameterIndex++, iso)") shouldBe true
            queries.contains("mapper: (value_: Boolean?) -> T") shouldBe true
            queries.contains("mapper: (value_: OffsetDateTime?) -> T") shouldBe true
            queries.contains("cursor.getBoolean(0)") shouldBe true
            queries.contains("cursor.getObject<OffsetDateTime>(0)") shouldBe true
        }

        test("generates Oracle GraphQL passing bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE departments (
                      id NUMBER(10, 0) NOT NULL,
                      status VARCHAR2(20) NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectGraphqlDepartments:
                    SELECT d.id
                    FROM departments d,
                      GRAPHQL('
                        departments(department_id: ${'$'}department_id, status: ${'$'}status) {
                          _id: department_id
                        }
                      ' PASSING
                        CAST(:department_id AS NUMBER(10, 0)) AS department_id,
                        CAST(:status AS VARCHAR2(20)) AS status
                      ) department_documents
                    WHERE d.id = :filter_id;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun selectGraphqlDepartments(") shouldBe true
            queries.contains("department_id: Long,") shouldBe true
            queries.contains("status: String,") shouldBe true
            queries.contains("filter_id: Long,") shouldBe true
            queries.contains("|  ' PASSING") shouldBe true
            queries.contains("|    CAST(? AS NUMBER(10, 0)) AS department_id,") shouldBe true
            queries.contains("|    CAST(? AS VARCHAR2(20)) AS status") shouldBe true
            queries.contains("|WHERE d.id = ?") shouldBe true
            queries.contains("bindLong(parameterIndex++, department_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, status)") shouldBe true
            queries.contains("bindLong(parameterIndex++, filter_id)") shouldBe true
        }

        test("generates Oracle CALL bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import java.time.LocalDateTime;

                    callAdjustSalary:
                    CALL hr.employee_api.adjust_salary(
                      employee_id => CAST(:employee_id AS NUMBER(10, 0)),
                      delta => CAST(:delta AS NUMBER(10, 2)),
                      effective_at => CAST(:effective_at AS TIMESTAMP)
                    );

                    callRefreshCache:
                    CALL hr.remote_api.refresh_cache@reporting.us.example(
                      tenant_id => CAST(:tenant_id AS NUMBER(10, 0)),
                      status => CAST(:status AS VARCHAR2(20))
                    );

                    callCurrentSalaryInto:
                    CALL hr.employee_api.current_salary(
                      employee_id => CAST(:employee_id AS NUMBER(10, 0))
                    ) INTO :salary_out;

                    callObjectMethodInto:
                    CALL warehouse_typ(
                      CAST(:warehouse_id AS NUMBER(10, 0)),
                      CAST(:warehouse_name AS VARCHAR2(128)),
                      CAST(:warehouse_area AS NUMBER(10, 0))
                    ).ret_name() INTO :warehouse_name_out;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun callAdjustSalary(") shouldBe true
            queries.contains("employee_id: Long,") shouldBe true
            queries.contains("delta: BigDecimal,") shouldBe true
            queries.contains("effective_at: LocalDateTime,") shouldBe true
            queries.contains("|  employee_id => CAST(? AS NUMBER(10, 0)),") shouldBe true
            queries.contains("|  delta => CAST(? AS NUMBER(10, 2)),") shouldBe true
            queries.contains("|  effective_at => CAST(? AS TIMESTAMP)") shouldBe true
            queries.contains("bindLong(parameterIndex++, employee_id)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, delta)") shouldBe true
            queries.contains("bindObject(parameterIndex++, effective_at)") shouldBe true
            queries.contains("public fun callRefreshCache(tenant_id: Long, status: String): QueryResult<Long>") shouldBe true
            queries.contains("|CALL hr.remote_api.refresh_cache@reporting.us.example(") shouldBe true
            queries.contains("|  tenant_id => CAST(? AS NUMBER(10, 0)),") shouldBe true
            queries.contains("|  status => CAST(? AS VARCHAR2(20))") shouldBe true
            queries.contains("bindLong(parameterIndex++, tenant_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, status)") shouldBe true
            queries.contains("public fun callCurrentSalaryInto(employee_id: Long): QueryResult<Long>") shouldBe true
            queries.contains("|CALL hr.employee_api.current_salary(") shouldBe true
            queries.contains("|  employee_id => CAST(? AS NUMBER(10, 0))") shouldBe true
            queries.contains("|) INTO :salary_out") shouldBe true
            queries.contains("salary_out") shouldBe true
            queries.contains("bindString(parameterIndex++, salary_out)") shouldBe false
            queries.contains("public fun callObjectMethodInto(") shouldBe true
            queries.contains("warehouse_id: Long,") shouldBe true
            queries.contains("warehouse_name: String,") shouldBe true
            queries.contains("warehouse_area: Long,") shouldBe true
            queries.contains("|CALL warehouse_typ(") shouldBe true
            queries.contains("|  CAST(? AS NUMBER(10, 0)),") shouldBe true
            queries.contains("|  CAST(? AS VARCHAR2(128)),") shouldBe true
            queries.contains("|  CAST(? AS NUMBER(10, 0))") shouldBe true
            queries.contains("|).ret_name() INTO :warehouse_name_out") shouldBe true
            queries.contains("bindString(parameterIndex++, warehouse_name_out)") shouldBe false
        }

        test("generates Oracle hierarchical query bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Region;

                    CREATE TABLE employees (
                      id NUMBER(10, 0) NOT NULL,
                      manager_id NUMBER(10, 0),
                      name VARCHAR2(100) NOT NULL,
                      region NVARCHAR2(16) AS Region NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectOrgTree:
                    SELECT id, CONNECT_BY_ROOT name AS root_name, LEVEL AS depth
                    FROM employees
                    START WITH id = :root_id AND region = :region
                    CONNECT BY NOCYCLE PRIOR id = manager_id AND LEVEL <= :max_depth
                    ORDER SIBLINGS BY name;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectOrgTree(") shouldBe true
            queries.contains("root_id: Long,") shouldBe true
            queries.contains("region: Region,") shouldBe true
            queries.contains("max_depth: Long,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("root_name: String,") shouldBe true
            queries.contains("depth: Long,") shouldBe true
            queries.contains("|START WITH id = ? AND region = ?") shouldBe true
            queries.contains("|CONNECT BY NOCYCLE PRIOR id = manager_id AND LEVEL <= ?") shouldBe true
            queries.contains("|ORDER SIBLINGS BY name") shouldBe true
            queries.contains("bindLong(parameterIndex++, root_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, employeesAdapter.regionAdapter.encode(region))") shouldBe true
            queries.contains("bindLong(parameterIndex++, max_depth)") shouldBe true
        }

        test("generates Oracle match recognize bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import com.example.Symbol;

                    CREATE TABLE trades (
                      id NUMBER(10, 0) NOT NULL,
                      symbol NVARCHAR2(16) AS Symbol NOT NULL,
                      price NUMBER(12, 2) NOT NULL,
                      traded_at TIMESTAMP NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectPriceRuns:
                    SELECT runs.matched_symbol, runs.match_id, runs.match_type, runs.match_row_number
                    FROM trades MATCH_RECOGNIZE (
                      PARTITION BY symbol
                      ORDER BY traded_at
                      MEASURES
                        symbol AS matched_symbol,
                        id AS match_id,
                        CLASSIFIER() AS match_type,
                        ROW_NUMBER() AS match_row_number
                      ONE ROW PER MATCH
                      PATTERN (rising+)
                      DEFINE rising AS rising.price >= :min_price AND rising.symbol = :symbol
                    ) runs;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectPriceRuns(") shouldBe true
            queries.contains("min_price: BigDecimal,") shouldBe true
            queries.contains("symbol: Symbol,") shouldBe true
            queries.contains("matched_symbol: Symbol,") shouldBe true
            queries.contains("match_id: Long,") shouldBe true
            queries.contains("match_type: String,") shouldBe true
            queries.contains("match_row_number: Long,") shouldBe true
            queries.contains("|  DEFINE rising AS rising.price >= ? AND rising.symbol = ?") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_price)") shouldBe true
            queries.contains("bindString(parameterIndex++, tradesAdapter.symbolAdapter.encode(symbol))") shouldBe true
        }

        test("generates Oracle model clause bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE sales (
                      region VARCHAR2(30) NOT NULL,
                      product VARCHAR2(30) NOT NULL,
                      year_num NUMBER(10, 0) NOT NULL,
                      amount NUMBER(12, 2)
                    );

                    selectModeledSales:
                    SELECT region, product, year_num, amount
                    FROM sales
                    WHERE region = :region
                    MODEL RETURN UPDATED ROWS
                      PARTITION BY (region)
                      DIMENSION BY (product, year_num)
                      MEASURES (amount)
                      RULES UPSERT ALL SEQUENTIAL ORDER (
                        amount['chairs', 2026] ORDER BY year_num = amount['chairs', 2025] + 10
                      );
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectModeledSales(region: String, mapper: (") shouldBe true
            queries.contains("region: String,") shouldBe true
            queries.contains("product: String,") shouldBe true
            queries.contains("year_num: Long,") shouldBe true
            queries.contains("amount: BigDecimal?,") shouldBe true
            queries.contains("|WHERE region = ?") shouldBe true
            queries.contains("|MODEL RETURN UPDATED ROWS") shouldBe true
            queries.contains("|  PARTITION BY (region)") shouldBe true
            queries.contains("|  DIMENSION BY (product, year_num)") shouldBe true
            queries.contains("|  MEASURES (amount)") shouldBe true
            queries.contains("|  RULES UPSERT ALL SEQUENTIAL ORDER (") shouldBe true
            queries.contains("|    amount['chairs', 2026] ORDER BY year_num = amount['chairs', 2025] + 10") shouldBe true
            queries.contains("bindString(parameterIndex++, region)") shouldBe true
        }

        test("generates Oracle row limiting bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE ranked_accounts (
                      id NUMBER(10, 0) NOT NULL,
                      status VARCHAR2(32) NOT NULL,
                      score NUMBER(10, 2),
                      PRIMARY KEY (id)
                    );

                    selectPage:
                    SELECT id, status
                    FROM ranked_accounts
                    WHERE status = :status
                    ORDER BY score DESC
                    OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY;

                    selectPartitions:
                    SELECT id, status
                    FROM ranked_accounts
                    ORDER BY status, score DESC
                    FETCH EXACT FIRST :partition_count PARTITIONS BY status, :rows_per_partition ROWS ONLY;

                    selectApproximate:
                    SELECT id, status
                    FROM ranked_accounts
                    ORDER BY score DESC
                    FETCH APPROX FIRST :row_count ROWS ONLY
                    WITH TARGET ACCURACY PARAMETERS (
                      EFSEARCH :ef_search,
                      NEIGHBOR PARTITION PROBES :partition_probes
                    );
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectPage(") shouldBe true
            queries.contains("status: String,") shouldBe true
            queries.contains("offset: Long,") shouldBe true
            queries.contains("limit: Long,") shouldBe true
            queries.contains("|OFFSET ? ROWS FETCH NEXT ? ROWS ONLY") shouldBe true
            queries.contains("bindString(parameterIndex++, status)") shouldBe true
            queries.contains("bindLong(parameterIndex++, offset)") shouldBe true
            queries.contains("bindLong(parameterIndex++, limit)") shouldBe true
            queries.contains("public fun <T : Any> selectPartitions(") shouldBe true
            queries.contains("partition_count: Long,") shouldBe true
            queries.contains("rows_per_partition: Long,") shouldBe true
            queries.contains("|FETCH EXACT FIRST ? PARTITIONS BY status, ? ROWS ONLY") shouldBe true
            queries.contains("bindLong(parameterIndex++, partition_count)") shouldBe true
            queries.contains("bindLong(parameterIndex++, rows_per_partition)") shouldBe true
            queries.contains("public fun <T : Any> selectApproximate(") shouldBe true
            queries.contains("row_count: Long,") shouldBe true
            queries.contains("ef_search: Long,") shouldBe true
            queries.contains("partition_probes: Long,") shouldBe true
            queries.contains("|FETCH APPROX FIRST ? ROWS ONLY") shouldBe true
            queries.contains("|WITH TARGET ACCURACY PARAMETERS (") shouldBe true
            queries.contains("|  EFSEARCH ?,") shouldBe true
            queries.contains("|  NEIGHBOR PARTITION PROBES ?") shouldBe true
            queries.contains("bindLong(parameterIndex++, row_count)") shouldBe true
            queries.contains("bindLong(parameterIndex++, ef_search)") shouldBe true
            queries.contains("bindLong(parameterIndex++, partition_probes)") shouldBe true
        }

        test("generates Oracle sample clause bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE sampled_orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      order_total NUMBER(10, 2) NOT NULL,
                      region VARCHAR2(16) NOT NULL,
                      PRIMARY KEY (order_id)
                    );

                    selectSampledOrders:
                    SELECT so.order_id
                    FROM sampled_orders SAMPLE BLOCK (CAST(:sample_percent AS NUMBER(5, 2)))
                      SEED (CAST(:sample_seed AS NUMBER(10, 0))) so
                    WHERE so.order_total >= :min_total;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun selectSampledOrders(") shouldBe true
            queries.contains("sample_percent: BigDecimal,") shouldBe true
            queries.contains("sample_seed: Long,") shouldBe true
            queries.contains("min_total: BigDecimal,") shouldBe true
            queries.contains("|FROM sampled_orders SAMPLE BLOCK (CAST(? AS NUMBER(5, 2)))") shouldBe true
            queries.contains("|  SEED (CAST(? AS NUMBER(10, 0))) so") shouldBe true
            queries.contains("|WHERE so.order_total >= ?") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, sample_percent)") shouldBe true
            queries.contains("bindLong(parameterIndex++, sample_seed)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_total)") shouldBe true
        }

        test("generates Oracle cross apply bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import com.example.DepartmentName;
                    import com.example.EmployeeName;

                    CREATE TABLE departments (
                      id NUMBER(10, 0) NOT NULL,
                      department_name NVARCHAR2(100) AS DepartmentName NOT NULL,
                      PRIMARY KEY (id)
                    );

                    CREATE TABLE employees (
                      id NUMBER(10, 0) NOT NULL,
                      department_id NUMBER(10, 0) NOT NULL,
                      employee_name NVARCHAR2(100) AS EmployeeName NOT NULL,
                      salary NUMBER(10, 2) NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectDepartmentMatches:
                    SELECT d.department_name AS dept_name,
                           employee_matches.employee_name AS match_name,
                           employee_matches.salary
                    FROM departments d
                    CROSS APPLY (
                      SELECT e.employee_name, e.salary
                      FROM employees e
                      WHERE e.department_id = d.id
                        AND e.employee_name = :filter_employee_name
                        AND e.salary >= :min_salary
                    ) employee_matches
                    WHERE d.department_name = :filter_department_name;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectDepartmentMatches(") shouldBe true
            queries.contains("filter_employee_name: EmployeeName,") shouldBe true
            queries.contains("min_salary: BigDecimal,") shouldBe true
            queries.contains("filter_department_name: DepartmentName,") shouldBe true
            queries.contains("dept_name: DepartmentName,") shouldBe true
            queries.contains("match_name: EmployeeName,") shouldBe true
            queries.contains("salary: BigDecimal,") shouldBe true
            queries.contains("departmentsAdapter.department_nameAdapter.decode(cursor.getString(0)!!)") shouldBe true
            queries.contains("employeesAdapter.employee_nameAdapter.decode(cursor.getString(1)!!)") shouldBe true
            queries.contains("|CROSS APPLY (") shouldBe true
            queries.contains("|  WHERE e.department_id = d.id") shouldBe true
            queries.contains("|    AND e.employee_name = ?") shouldBe true
            queries.contains("|    AND e.salary >= ?") shouldBe true
            queries.contains("|WHERE d.department_name = ?") shouldBe true
            queries.contains("bindString(parameterIndex++, employeesAdapter.employee_nameAdapter.encode(filter_employee_name))") shouldBe
                true
            queries.contains("bindBigDecimal(parameterIndex++, min_salary)") shouldBe true
            queries.contains(
                "bindString(parameterIndex++, departmentsAdapter.department_nameAdapter.encode(filter_department_name))",
            ) shouldBe
                true
        }

        test("generates Oracle join to one bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE employees (
                      id NUMBER(10, 0) NOT NULL,
                      department_id NUMBER(10, 0),
                      salary NUMBER(10, 2) NOT NULL,
                      PRIMARY KEY (id)
                    );

                    CREATE TABLE departments (
                      id NUMBER(10, 0) NOT NULL,
                      department_name VARCHAR2(80) NOT NULL,
                      status VARCHAR2(20),
                      PRIMARY KEY (id)
                    );

                    selectJoinToOneDepartment:
                    SELECT e.id, d.department_name AS dept_name
                    FROM employees e JOIN TO ONE (
                      departments d ON d.id = e.department_id
                        AND d.department_name = :department_name
                    )
                    WHERE e.salary >= :min_salary;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectJoinToOneDepartment(") shouldBe true
            queries.contains("department_name: String,") shouldBe true
            queries.contains("min_salary: BigDecimal,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("dept_name: String) -> T,") shouldBe true
            queries.contains("|FROM employees e JOIN TO ONE (") shouldBe true
            queries.contains("|  departments d ON d.id = e.department_id") shouldBe true
            queries.contains("|    AND d.department_name = ?") shouldBe true
            queries.contains("|WHERE e.salary >= ?") shouldBe true
            queries.contains("bindString(parameterIndex++, department_name)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_salary)") shouldBe true
        }

        test("generates Oracle qualify bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import com.example.Region;

                    CREATE TABLE ranked_orders (
                      id NUMBER(10, 0) NOT NULL,
                      region NVARCHAR2(16) AS Region NOT NULL,
                      score NUMBER(10, 2) NOT NULL,
                      PRIMARY KEY (id)
                    );

                    selectRankedOrders:
                    SELECT id,
                           region,
                           score,
                           RANK() OVER (PARTITION BY region ORDER BY score DESC) AS ranking
                    FROM ranked_orders
                    WHERE region = :region
                    QUALIFY RANK() OVER (PARTITION BY region ORDER BY score DESC) <= :max_rank
                      AND score >= :min_score;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectRankedOrders(") shouldBe true
            queries.contains("region: Region,") shouldBe true
            queries.contains("max_rank: Long,") shouldBe true
            queries.contains("min_score: BigDecimal,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("score: BigDecimal,") shouldBe true
            queries.contains("ranking: Long,") shouldBe true
            queries.contains("ranked_ordersAdapter.regionAdapter.decode(cursor.getString(1)!!)") shouldBe true
            queries.contains("|WHERE region = ?") shouldBe true
            queries.contains("|QUALIFY RANK() OVER (PARTITION BY region ORDER BY score DESC) <= ?") shouldBe true
            queries.contains("|  AND score >= ?") shouldBe true
            queries.contains("bindString(parameterIndex++, ranked_ordersAdapter.regionAdapter.encode(region))") shouldBe true
            queries.contains("bindLong(parameterIndex++, max_rank)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, min_score)") shouldBe true
        }

        test("generates Oracle collection table bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    selectNumberCollection:
                    SELECT numbers.COLUMN_VALUE AS number_value
                    FROM TABLE(ODCINUMBERLIST(
                      CAST(:first_number AS NUMBER(10, 2)),
                      CAST(:second_number AS NUMBER(10, 2))
                    )) numbers
                    WHERE numbers.COLUMN_VALUE >= CAST(:minimum_number AS NUMBER(10, 2));

                    selectNameCollection:
                    SELECT names.COLUMN_VALUE AS name_value
                    FROM TABLE(SYS.ODCIVARCHAR2LIST(
                      CAST(:first_name AS VARCHAR2(50)),
                      CAST(:second_name AS VARCHAR2(50))
                    )) names
                    WHERE names.COLUMN_VALUE LIKE CAST(:name_pattern AS VARCHAR2(50))
                      ESCAPE CAST(:escape_char AS VARCHAR2(1));
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun selectNumberCollection(") shouldBe true
            queries.contains("first_number: BigDecimal,") shouldBe true
            queries.contains("second_number: BigDecimal,") shouldBe true
            queries.contains("minimum_number: BigDecimal,") shouldBe true
            queries.contains("public fun selectNameCollection(") shouldBe true
            queries.contains("first_name: String,") shouldBe true
            queries.contains("second_name: String,") shouldBe true
            queries.contains("name_pattern: String,") shouldBe true
            queries.contains("escape_char: String,") shouldBe true
            queries.contains("Query<BigDecimal>") shouldBe true
            queries.contains("Query<String>") shouldBe true
            queries.contains("|FROM TABLE(ODCINUMBERLIST(") shouldBe true
            queries.contains("|  CAST(? AS NUMBER(10, 2)),") shouldBe true
            queries.contains("|  CAST(? AS NUMBER(10, 2))") shouldBe true
            queries.contains("|)) numbers") shouldBe true
            queries.contains("|WHERE numbers.COLUMN_VALUE >= CAST(? AS NUMBER(10, 2))") shouldBe true
            queries.contains("|FROM TABLE(SYS.ODCIVARCHAR2LIST(") shouldBe true
            queries.contains("|  CAST(? AS VARCHAR2(50)),") shouldBe true
            queries.contains("|  CAST(? AS VARCHAR2(50))") shouldBe true
            queries.contains("|)) names") shouldBe true
            queries.contains("|WHERE names.COLUMN_VALUE LIKE CAST(? AS VARCHAR2(50))") shouldBe true
            queries.contains("|  ESCAPE CAST(? AS VARCHAR2(1))") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, first_number)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, second_number)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, minimum_number)") shouldBe true
            queries.contains("bindString(parameterIndex++, first_name)") shouldBe true
            queries.contains("bindString(parameterIndex++, second_name)") shouldBe true
            queries.contains("bindString(parameterIndex++, name_pattern)") shouldBe true
            queries.contains("bindString(parameterIndex++, escape_char)") shouldBe true
        }

        test("generates Oracle containers and shards bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE accounts (
                      id NUMBER(10, 0) NOT NULL,
                      region VARCHAR2(30) NOT NULL,
                      status VARCHAR2(20),
                      PRIMARY KEY (id)
                    );

                    selectContainerAccounts:
                    SELECT c.id, c.region, c.CON_ID
                    FROM CONTAINERS(accounts) c
                    WHERE c.CON_ID = :container_id
                      AND c.region = :region;

                    selectShardAccounts:
                    SELECT s.id, s.status, s.ORA_SHARDSPACE_NAME
                    FROM SHARDS(accounts) s
                    WHERE s.ORA_SHARDSPACE_NAME LIKE :shard_pattern
                      AND s.id > :minimum_id;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectContainerAccounts(") shouldBe true
            queries.contains("container_id: Long,") shouldBe true
            queries.contains("region: String,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("CON_ID: Long,") shouldBe true
            queries.contains("|FROM CONTAINERS(accounts) c") shouldBe true
            queries.contains("|WHERE c.CON_ID = ?") shouldBe true
            queries.contains("|  AND c.region = ?") shouldBe true
            queries.contains("bindLong(parameterIndex++, container_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, region)") shouldBe true
            queries.contains("public fun <T : Any> selectShardAccounts(") shouldBe true
            queries.contains("shard_pattern: String,") shouldBe true
            queries.contains("minimum_id: Long,") shouldBe true
            queries.contains("ORA_SHARDSPACE_NAME: String,") shouldBe true
            queries.contains("|FROM SHARDS(accounts) s") shouldBe true
            queries.contains("|WHERE s.ORA_SHARDSPACE_NAME LIKE ?") shouldBe true
            queries.contains("|  AND s.id > ?") shouldBe true
            queries.contains("bindString(parameterIndex++, shard_pattern)") shouldBe true
            queries.contains("bindLong(parameterIndex++, minimum_id)") shouldBe true
        }

        test("generates Oracle partition extension DML bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;
                    import com.example.Region;

                    CREATE TABLE partitioned_orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      region_code NVARCHAR2(16) AS Region NOT NULL,
                      order_total NUMBER(10, 2),
                      PRIMARY KEY (order_id)
                    );

                    insertPartitionByName:
                    INSERT INTO partitioned_orders PARTITION (orders_2026_q1) (order_id, region_code, order_total)
                    VALUES (:order_id, :region_code, :order_total);

                    updateSubpartitionForKey:
                    UPDATE partitioned_orders SUBPARTITION FOR ('US')
                    SET order_total = order_total + :delta
                    WHERE region_code = :region_code;

                    deletePartitionForKey:
                    DELETE FROM partitioned_orders PARTITION FOR (2026, 1)
                    WHERE region_code = :region_code AND order_total < :max_total;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun insertPartitionByName(") shouldBe true
            queries.contains("order_id: Long,") shouldBe true
            queries.contains("region_code: Region,") shouldBe true
            queries.contains("order_total: BigDecimal?,") shouldBe true
            queries.contains("|INSERT INTO partitioned_orders PARTITION (orders_2026_q1) (order_id, region_code, order_total)") shouldBe
                true
            queries.contains("|VALUES (?, ?, ?)") shouldBe true
            queries.contains("bindLong(parameterIndex++, order_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, partitioned_ordersAdapter.region_codeAdapter.encode(region_code))") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, order_total)") shouldBe true
            queries.contains("public fun updateSubpartitionForKey(delta: BigDecimal?, region_code: Region): QueryResult<Long>") shouldBe
                true
            queries.contains("|UPDATE partitioned_orders SUBPARTITION FOR ('US')") shouldBe true
            queries.contains("|SET order_total = order_total + ?") shouldBe true
            queries.contains("|WHERE region_code = ?") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, delta)") shouldBe true
            queries.contains("public fun deletePartitionForKey(region_code: Region, max_total: BigDecimal?): QueryResult<Long>") shouldBe
                true
            queries.contains("|DELETE FROM partitioned_orders PARTITION FOR (2026, 1)") shouldBe true
            queries.contains("|WHERE region_code = ? AND order_total < ?") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, max_total)") shouldBe true
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

        test("generates Oracle DML error logging bind parameters exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    import java.math.BigDecimal;

                    CREATE TABLE import_orders (
                      order_id NUMBER(10, 0) NOT NULL,
                      customer_name VARCHAR2(128) NOT NULL,
                      order_total NUMBER(10, 2),
                      PRIMARY KEY (order_id)
                    );

                    insertWithErrorLoggingTag:
                    INSERT INTO import_orders (order_id, customer_name, order_total)
                    VALUES (:order_id, :customer_name, :order_total)
                    LOG ERRORS INTO import_order_errors (:error_tag) REJECT LIMIT 25;

                    updateWithErrorLoggingTag:
                    UPDATE import_orders
                    SET order_total = :order_total
                    WHERE order_id = :order_id
                    LOG ERRORS INTO import_order_errors (:error_tag) REJECT LIMIT UNLIMITED;

                    deleteWithErrorLoggingTag:
                    DELETE FROM import_orders
                    WHERE order_id = :order_id
                    LOG ERRORS INTO import_order_errors (:error_tag) REJECT LIMIT 10;

                    mergeWithErrorLoggingTag:
                    MERGE INTO import_orders target
                    USING import_orders source
                    ON (target.order_id = :order_id AND source.order_id = :source_id)
                    WHEN MATCHED THEN
                      UPDATE SET target.order_total = :order_total
                    WHEN NOT MATCHED THEN
                      INSERT (order_id, customer_name, order_total)
                      VALUES (:insert_order_id, :insert_customer_name, :insert_order_total)
                    LOG ERRORS INTO import_order_errors (:error_tag) REJECT LIMIT UNLIMITED;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun insertWithErrorLoggingTag(") shouldBe true
            queries.contains("order_id: Long,") shouldBe true
            queries.contains("customer_name: String,") shouldBe true
            queries.contains("order_total: BigDecimal?,") shouldBe true
            queries.contains("error_tag: String,") shouldBe true
            queries.contains("|VALUES (?, ?, ?)") shouldBe true
            queries.contains("|LOG ERRORS INTO import_order_errors (?) REJECT LIMIT 25") shouldBe true
            queries.contains("bindLong(parameterIndex++, order_id)") shouldBe true
            queries.contains("bindString(parameterIndex++, customer_name)") shouldBe true
            queries.contains("bindBigDecimal(parameterIndex++, order_total)") shouldBe true
            queries.contains("bindString(parameterIndex++, error_tag)") shouldBe true
            queries.contains("public fun updateWithErrorLoggingTag(") shouldBe true
            queries.contains("|UPDATE import_orders") shouldBe true
            queries.contains("|SET order_total = ?") shouldBe true
            queries.contains("|WHERE order_id = ?") shouldBe true
            queries.contains("|LOG ERRORS INTO import_order_errors (?) REJECT LIMIT UNLIMITED") shouldBe true
            queries.contains("public fun deleteWithErrorLoggingTag(order_id: Long, error_tag: String): QueryResult<Long>") shouldBe true
            queries.contains("|DELETE FROM import_orders") shouldBe true
            queries.contains("|LOG ERRORS INTO import_order_errors (?) REJECT LIMIT 10") shouldBe true
            queries.contains("public fun mergeWithErrorLoggingTag(") shouldBe true
            queries.contains("source_id: Long,") shouldBe true
            queries.contains("insert_order_id: Long,") shouldBe true
            queries.contains("insert_customer_name: String,") shouldBe true
            queries.contains("insert_order_total: BigDecimal?,") shouldBe true
            queries.contains("|ON (target.order_id = ? AND source.order_id = ?)") shouldBe true
            queries.contains("|  UPDATE SET target.order_total = ?") shouldBe true
            queries.contains("|  VALUES (?, ?, ?)") shouldBe true
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

        test("generates Oracle recursive CTE search cycle columns exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE org_units (
                      id NUMBER(10, 0) NOT NULL,
                      parent_id NUMBER(10, 0),
                      unit_name VARCHAR2(128) NOT NULL
                    );

                    selectOrgTree:
                    WITH unit_tree (id, parent_id, unit_name) AS (
                      SELECT id, parent_id, unit_name
                      FROM org_units
                      WHERE parent_id IS NULL
                      UNION ALL
                      SELECT child.id, child.parent_id, child.unit_name
                      FROM org_units child
                      JOIN unit_tree parent ON child.parent_id = parent.id
                      WHERE child.unit_name LIKE :name_pattern
                    )
                    SEARCH DEPTH FIRST BY unit_name ASC NULLS LAST SET traversal_order
                    CYCLE id SET is_cycle TO 'Y' DEFAULT 'N'
                    SELECT id, parent_id, unit_name, traversal_order, is_cycle
                    FROM unit_tree
                    WHERE is_cycle = :cycle_marker
                    ORDER BY traversal_order;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("public fun <T : Any> selectOrgTree(") shouldBe true
            queries.contains("name_pattern: String,") shouldBe true
            queries.contains("cycle_marker: String,") shouldBe true
            queries.contains("id: Long,") shouldBe true
            queries.contains("parent_id: Long?,") shouldBe true
            queries.contains("unit_name: String,") shouldBe true
            queries.contains("traversal_order: Long,") shouldBe true
            queries.contains("is_cycle: String,") shouldBe true
            queries.contains("|SEARCH DEPTH FIRST BY unit_name ASC NULLS LAST SET traversal_order") shouldBe true
            queries.contains("|CYCLE id SET is_cycle TO 'Y' DEFAULT 'N'") shouldBe true
            queries.contains("|WHERE is_cycle = ?") shouldBe true
            queries.contains("|ORDER BY traversal_order") shouldBe true
            queries.contains("bindString(parameterIndex++, name_pattern)") shouldBe true
            queries.contains("bindString(parameterIndex++, cycle_marker)") shouldBe true
        }

        test("generates Oracle approximate rank result exactly") {
            val generated =
                generateOracleSqlDelight(
                    """
                    CREATE TABLE sample (
                      department_id NUMBER(10) NOT NULL,
                      salary NUMBER(10, 2)
                    );

                    topDepartments:
                    SELECT department_id,
                           APPROX_SUM(salary) AS total_salary,
                           APPROX_RANK(
                             PARTITION BY department_id
                             ORDER BY APPROX_SUM(salary) DESC
                           ) AS ranking
                    FROM sample
                    GROUP BY department_id
                    HAVING APPROX_RANK(
                             PARTITION BY department_id
                             ORDER BY APPROX_SUM(salary) DESC
                           ) <= 10;
                    """.trimIndent(),
                )

            val queries = generated.contentsByFile.getValue("com/example/TestQueries.kt")
            queries.contains("ranking: Long,") shouldBe true
            queries.contains("cursor.getLong(2)!!") shouldBe true
            queries.contains("APPROX_RANK(") shouldBe true
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
