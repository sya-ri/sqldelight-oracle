package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.core.annotators.OptimisticLockCompilerAnnotator
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.psi.PsiDocumentManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

class OracleResultColumnTypeTest :
    FunSpec({
        val schema =
            """
            CREATE TABLE emp (
              id NUMBER(10) NOT NULL,
              small_id NUMBER(5) NOT NULL,
              big_id NUMBER(19) NOT NULL,
              name VARCHAR2(100) NOT NULL,
              nickname VARCHAR2(100),
              salary NUMBER(10, 2),
              bonus BINARY_DOUBLE,
              raw_col RAW(16),
              embedding VECTOR,
              target_embedding VECTOR NOT NULL,
              hire_date DATE NOT NULL,
              created_ts TIMESTAMP,
              dept_id NUMBER(10),
              fiscal_start DATE,
              periods NUMBER(10),
              fmt VARCHAR2(100),
              nls VARCHAR2(100),
              restated VARCHAR2(20),
              xml_doc XMLTYPE,
              json_doc JSON
            );

            CREATE SEQUENCE emp_seq;
            """.trimIndent()

        fun typeOf(query: String): String {
            val sql = "$schema\n\nresult:\n$query;"
            val columns = oracleResultColumnTypes(sql)
            return columns.single()
        }

        test("resolves Oracle aggregate function result column types exactly") {
            typeOf("SELECT COUNT(*) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT SUM(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT AVG(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT MAX(hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT MIN(name) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT CORR(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT COVAR_POP(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT LISTAGG(nickname, ',') WITHIN GROUP (ORDER BY nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT MEDIAN(hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT APPROX_MEDIAN(hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT APPROX_PERCENTILE(0.5) WITHIN GROUP (ORDER BY hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf(
                "SELECT APPROX_PERCENTILE(0.5 DETERMINISTIC, 'ERROR_RATE') WITHIN GROUP (ORDER BY id) AS c FROM emp",
            ) shouldBe "java.math.BigDecimal?"
        }

        test("resolves Oracle scalar function result column types exactly") {
            typeOf("SELECT ABS(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT MOD(id, 2) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT ROUND(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT TO_CHAR(hire_date) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT LENGTH(name) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT SUBSTR(name, 1, 3) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT LENGTH(nickname) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT UPPER(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT SUBSTR(nickname, 1, 3) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT NVL(name, 'x') AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT NVL2(name, salary, 0) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT NVL2(nickname, id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT COALESCE(salary, 0) AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT TO_CHAR(created_ts) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_NUMBER(nickname) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT NULLIF(id, small_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT DECODE(name, 'A', id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT DECODE(name, 'A', id, small_id) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT DECODE(name, 'A', salary, 0) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT DECODE(name, 'A', id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT GREATEST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT EXTRACT(YEAR FROM hire_date) AS c FROM emp") shouldBe "java.math.BigDecimal"
        }

        test("propagates Oracle function result column nullability exactly") {
            typeOf("SELECT ROUND(id, 2) AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT ROUND(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT SIGN(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT SQRT(salary) AS c FROM emp") shouldBe "kotlin.Double?"
            typeOf("SELECT EXP(salary) AS c FROM emp") shouldBe "kotlin.Double?"
            typeOf("SELECT BITAND(dept_id, 1) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT MOD(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REMAINDER(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT POWER(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT NANVL(bonus, salary) AS c FROM emp") shouldBe "kotlin.Double?"
            typeOf("SELECT WIDTH_BUCKET(salary, 0, 100, 10) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT TRUNC(created_ts) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT ADD_MONTHS(created_ts, 1) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT LAST_DAY(created_ts) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT NEXT_DAY(created_ts, 'MONDAY') AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT MONTHS_BETWEEN(hire_date, created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT EXTRACT(YEAR FROM created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT CHR(dept_id) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT NCHR(dept_id) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT SOUNDEX(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_CLOB(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_NCLOB(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_LOB(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_BLOB(raw_col) AS c FROM emp") shouldBe "kotlin.ByteArray?"
            typeOf("SELECT HEXTORAW(nickname) AS c FROM emp") shouldBe "kotlin.ByteArray?"
            typeOf("SELECT RAWTONHEX(raw_col) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT RAWTOHEX(raw_col) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT CHARTOROWID(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT ROWIDTOCHAR(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT ROWIDTONCHAR(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT RAW_TO_UUID(raw_col) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT UUID_TO_RAW(nickname) AS c FROM emp") shouldBe "kotlin.ByteArray?"
            typeOf("SELECT BIN_TO_NUM(dept_id, 0) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT GREATEST(id, small_id) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT GREATEST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT LEAST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
        }

        test("resolves Oracle CAST result column types exactly") {
            typeOf("SELECT CAST(salary AS NUMBER(5)) AS c FROM emp") shouldBe "kotlin.Int?"
            typeOf("SELECT CAST(dept_id AS VARCHAR2(20)) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT CAST(id AS VARCHAR2(20)) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT CAST(name AS DATE) AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT CAST(hire_date AS TIMESTAMP WITH TIME ZONE) AS c FROM emp") shouldBe "java.time.OffsetDateTime"
            typeOf("SELECT XMLCAST(xml_doc AS NUMBER) AS c FROM emp") shouldBe
                "java.math.BigDecimal?"
        }

        test("resolves Oracle TREAT result column types exactly") {
            typeOf("SELECT TREAT(json_doc AS JSON) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("resolves Oracle JSON id operator result column types exactly") {
            typeOf("SELECT JSON_ID('OID') AS c FROM emp") shouldBe "kotlin.ByteArray"
        }

        test("resolves Oracle SQL JSON null-returning result column types exactly") {
            typeOf("SELECT JSON_VALUE(name, '${'$'}.id' RETURNING NUMBER) AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT JSON_QUERY(name, '${'$'}.items' RETURNING CLOB) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT JSON_VALUE(name, '${'$'}.id' RETURNING NUMBER NULL ON EMPTY) AS c FROM emp") shouldBe
                "java.math.BigDecimal?"
            typeOf("SELECT JSON_QUERY(name, '${'$'}.items' RETURNING CLOB NULL ON ERROR) AS c FROM emp") shouldBe
                "kotlin.String?"
            typeOf("SELECT JSON_SERIALIZE(name RETURNING CLOB NULL ON ERROR) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT JSON_MERGEPATCH(name, restated RETURNING CLOB NULL ON ERROR) AS c FROM emp") shouldBe
                "kotlin.String?"
        }

        test("resolves Oracle SQL JSON input nullability exactly") {
            typeOf("SELECT JSON_VALUE(nickname, '${'$'}.id' RETURNING NUMBER) AS c FROM emp") shouldBe
                "java.math.BigDecimal?"
            typeOf("SELECT JSON_QUERY(nickname, '${'$'}.items' RETURNING CLOB) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("resolves Oracle serialization input nullability exactly") {
            typeOf("SELECT JSON_SERIALIZE(nickname RETURNING CLOB) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("resolves Oracle pseudocolumn result column types exactly") {
            typeOf("SELECT ROWNUM AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT LEVEL AS c FROM emp CONNECT BY PRIOR id = dept_id") shouldBe "kotlin.Long"
            typeOf("SELECT ROWID AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT ORA_ROWSCN AS c FROM emp") shouldBe "kotlin.Long"
        }

        test("resolves Oracle sequence pseudocolumn result column types exactly") {
            typeOf("SELECT emp_seq.NEXTVAL AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT emp_seq.CURRVAL AS c FROM emp") shouldBe "kotlin.Long"
        }

        test("resolves Oracle flashback versions pseudocolumn result column types exactly") {
            val flashbackClause = "FROM emp VERSIONS BETWEEN SCN MINVALUE AND MAXVALUE"

            typeOf("SELECT VERSIONS_STARTSCN AS c $flashbackClause") shouldBe "kotlin.Long?"
            typeOf("SELECT VERSIONS_ENDSCN AS c $flashbackClause") shouldBe "kotlin.Long?"
            typeOf("SELECT VERSIONS_STARTTIME AS c $flashbackClause") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT VERSIONS_ENDTIME AS c $flashbackClause") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT VERSIONS_XID AS c $flashbackClause") shouldBe "kotlin.ByteArray?"
            typeOf("SELECT VERSIONS_OPERATION AS c $flashbackClause") shouldBe "kotlin.String?"
        }

        test("resolves Oracle literal result column types exactly") {
            typeOf("SELECT DATE '2024-01-02' AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT TIMESTAMP '2024-01-02 03:04:05' AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT TIMESTAMP '2024-01-02 03:04:05' AT TIME ZONE 'UTC' AS c FROM emp") shouldBe "java.time.OffsetDateTime"
            typeOf("SELECT INTERVAL '2' DAY AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT TRUE AS c FROM emp") shouldBe "kotlin.Boolean"
        }

        test("resolves Oracle current datetime result column types exactly") {
            typeOf("SELECT SYSDATE AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT CURRENT_DATE AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT LOCALTIMESTAMP AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT SYSTIMESTAMP AS c FROM emp") shouldBe "java.time.OffsetDateTime"
            typeOf("SELECT CURRENT_TIMESTAMP AS c FROM emp") shouldBe "java.time.OffsetDateTime"
        }

        test("resolves Oracle numeric operator result column types exactly") {
            typeOf("SELECT id + 1 AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT salary + 1 AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT salary * 2 AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT id / 2 AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT bonus + salary AS c FROM emp") shouldBe "kotlin.Double?"
        }

        test("resolves Oracle datetime arithmetic result column types exactly") {
            typeOf("SELECT hire_date + 1 AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT hire_date - 7 AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT 1 + hire_date AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT created_ts + 30 AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT hire_date - hire_date AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT hire_date - created_ts AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT created_ts - created_ts AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("resolves Oracle datetime ceil and floor result column types exactly") {
            typeOf("SELECT CEIL(hire_date) AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT FLOOR(created_ts) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
        }

        test("resolves Oracle datetime interval arithmetic result column types exactly") {
            typeOf("SELECT hire_date + INTERVAL '1' DAY AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT hire_date - INTERVAL '2' HOUR AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT created_ts + INTERVAL '1' DAY AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT hire_date + NUMTODSINTERVAL(1, 'DAY') AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT INTERVAL '1' DAY + hire_date AS c FROM emp") shouldBe "java.time.LocalDateTime"
        }

        test("propagates Oracle interval conversion nullability exactly") {
            typeOf("SELECT NUMTODSINTERVAL(dept_id, 'DAY') AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT NUMTOYMINTERVAL(dept_id, 'MONTH') AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_DSINTERVAL(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT TO_YMINTERVAL(nickname) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("propagates Oracle system conversion nullability exactly") {
            typeOf("SELECT SCN_TO_TIMESTAMP(dept_id) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT TIMESTAMP_TO_SCN(created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT ORA_DST_AFFECTED(created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT ORA_DST_ERROR(created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT ORA_DST_CONVERT(created_ts) AS c FROM emp") shouldBe "java.time.OffsetDateTime?"
            typeOf("SELECT TZ_OFFSET(nickname) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("propagates Oracle calendar function nullability exactly") {
            typeOf("SELECT CALENDAR_YEAR(created_ts) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT FISCAL_YEAR(created_ts, fiscal_start) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT RETAIL_YEAR(created_ts, fmt, restated) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT CALENDAR_YEAR_START_DATE(created_ts) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT FISCAL_YEAR_START_DATE(created_ts, fiscal_start) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT RETAIL_YEAR_START_DATE(created_ts, restated) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT CALENDAR_YEAR_NUMBER(created_ts) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT FISCAL_YEAR_NUMBER(created_ts, fiscal_start) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT RETAIL_YEAR_NUMBER(created_ts, restated) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT CALENDAR_ADD_DAYS(created_ts, periods) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT FISCAL_ADD_DAYS(created_ts, periods, fiscal_start) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT RETAIL_ADD_DAYS(created_ts, periods, restated) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT RETAIL_DAY_EXISTS(created_ts, restated) AS c FROM emp") shouldBe "kotlin.Boolean?"
        }

        test("propagates Oracle vector function nullability exactly") {
            typeOf("SELECT TO_VECTOR(nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT FROM_VECTOR(embedding) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT VECTOR_DISTANCE(embedding, target_embedding) AS c FROM emp") shouldBe "kotlin.Double?"
            typeOf("SELECT L2_DISTANCE(embedding, target_embedding) AS c FROM emp") shouldBe "kotlin.Double?"
            typeOf("SELECT VECTOR_DIMS(embedding) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT VECTOR_DIMENSION_COUNT(embedding) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT VECTOR_DIMENSION_FORMAT(embedding) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT VECTOR_SERIALIZE(embedding RETURNING CLOB FORMAT DENSE) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT VECTOR_EMBEDDING(all_minilm_l12 USING nickname AS DATA) AS c FROM emp") shouldBe "kotlin.String?"
        }

        test("resolves Oracle grouping function result column types exactly") {
            typeOf("SELECT GROUPING(dept_id) AS c FROM emp GROUP BY ROLLUP(dept_id)") shouldBe "kotlin.Long"
            typeOf("SELECT GROUPING_ID(dept_id) AS c FROM emp GROUP BY ROLLUP(dept_id)") shouldBe "kotlin.Long"
            typeOf("SELECT GROUP_ID() AS c FROM emp GROUP BY dept_id") shouldBe "kotlin.Long"
        }

        test("resolves Oracle concatenation operator result column types exactly") {
            typeOf("SELECT id || name AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT salary || name AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT CONCAT(nickname, nickname) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT CONCAT(nickname, name) AS c FROM emp") shouldBe "kotlin.String"
        }

        test("resolves Oracle analytic function result column types exactly") {
            typeOf("SELECT ROW_NUMBER() OVER (ORDER BY id) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT LAG(salary) OVER (ORDER BY id) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT FIRST_VALUE(name) OVER (ORDER BY id) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT SUM(salary) OVER (PARTITION BY dept_id) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT RATIO_TO_REPORT(salary) OVER () AS c FROM emp") shouldBe "kotlin.Double?"
        }

        test("resolves Oracle linear regression aggregate result column types exactly") {
            typeOf("SELECT REGR_COUNT(salary, bonus) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT REGR_SLOPE(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_INTERCEPT(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_R2(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_AVGX(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_AVGY(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_SXX(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_SYY(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT REGR_SXY(salary, bonus) AS c FROM emp") shouldBe "java.math.BigDecimal?"
        }

        test("resolves Oracle value aggregate result column types exactly") {
            typeOf("SELECT ANY_VALUE(name) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT ANY_VALUE(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT STATS_MODE(id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT STATS_MODE(nickname) AS c FROM emp") shouldBe "kotlin.String?"
        }
    })

private fun oracleResultColumnTypes(
    sql: String,
    fileName: String = "Test.sq",
): List<String> {
    val root = Files.createTempDirectory("sqldelight-oracle-result-column-type-test").toFile()
    val sourceDirectory = File(root, "com/example").apply { mkdirs() }
    File(sourceDirectory, fileName).writeText(sql)

    val errors = mutableListOf<String>()
    val compilationUnit = OracleResultColumnTypeCompilationUnit(File(root, "output"))
    val environment =
        SqlDelightEnvironment(
            sourceFolders = listOf(root),
            dependencyFolders = emptyList(),
            properties =
                OracleResultColumnTypeDatabaseProperties(
                    rootDirectory = root,
                    compilationUnit = compilationUnit,
                ),
            dialect = OracleDialect(),
            verifyMigrations = true,
            moduleName = "oracle-result-column-type-test",
            compilationUnit = compilationUnit,
        )

    LanguageParserDefinitions.INSTANCE.forLanguage(SqlDelightLanguage).createParser(environment.project)
    LanguageParserDefinitions.INSTANCE.forLanguage(MigrationLanguage).createParser(environment.project)
    environment.annotate(listOf(OptimisticLockCompilerAnnotator())) { element, message ->
        val documentManager = PsiDocumentManager.getInstance(element.project)
        val document = requireNotNull(documentManager.getDocument(element.containingFile))
        val lineNumber = document.getLineNumber(element.textOffset)
        errors += "${element.containingFile.name}: (${lineNumber + 1}): $message"
    }
    check(errors.isEmpty()) { "Unexpected annotation errors: $errors" }

    val resultTypes = mutableListOf<String>()
    environment.forSourceFiles { psiFile ->
        if (psiFile is SqlDelightQueriesFile) {
            psiFile.namedQueries.forEach { query ->
                query.resultColumns.forEach { type ->
                    resultTypes += type.javaType.toString()
                }
            }
        }
    }
    return resultTypes
}

private data class OracleResultColumnTypeCompilationUnit(
    override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit {
    override val name: String = "test"
    override val sourceFolders: Set<SqlDelightSourceFolder> = emptySet()
}

private data class OracleResultColumnTypeDatabaseProperties(
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
