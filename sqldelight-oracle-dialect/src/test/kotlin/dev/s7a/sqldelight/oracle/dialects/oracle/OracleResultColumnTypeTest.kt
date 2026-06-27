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
              salary NUMBER(10, 2),
              bonus BINARY_DOUBLE,
              hire_date DATE NOT NULL,
              created_ts TIMESTAMP,
              dept_id NUMBER(10)
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
        }

        test("resolves Oracle scalar function result column types exactly") {
            typeOf("SELECT ABS(salary) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT MOD(id, 2) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT ROUND(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT TO_CHAR(hire_date) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT LENGTH(name) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT SUBSTR(name, 1, 3) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT NVL(name, 'x') AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT COALESCE(salary, 0) AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT GREATEST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT EXTRACT(YEAR FROM hire_date) AS c FROM emp") shouldBe "java.math.BigDecimal"
        }

        test("propagates Oracle function result column nullability exactly") {
            typeOf("SELECT ROUND(id, 2) AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT ROUND(salary, 2) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT TRUNC(created_ts) AS c FROM emp") shouldBe "java.time.LocalDateTime?"
            typeOf("SELECT GREATEST(id, small_id) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT GREATEST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
            typeOf("SELECT LEAST(id, dept_id) AS c FROM emp") shouldBe "kotlin.Long?"
        }

        test("resolves Oracle CAST result column types exactly") {
            typeOf("SELECT CAST(salary AS NUMBER(5)) AS c FROM emp") shouldBe "kotlin.Int"
            typeOf("SELECT CAST(id AS VARCHAR2(20)) AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT CAST(name AS DATE) AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT CAST(hire_date AS TIMESTAMP WITH TIME ZONE) AS c FROM emp") shouldBe "java.time.OffsetDateTime"
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

        test("resolves Oracle literal result column types exactly") {
            typeOf("SELECT DATE '2024-01-02' AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT TIMESTAMP '2024-01-02 03:04:05' AS c FROM emp") shouldBe "java.time.LocalDateTime"
            typeOf("SELECT TIMESTAMP '2024-01-02 03:04:05' AT TIME ZONE 'UTC' AS c FROM emp") shouldBe "java.time.OffsetDateTime"
            typeOf("SELECT INTERVAL '2' DAY AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT TRUE AS c FROM emp") shouldBe "kotlin.Boolean"
        }

        test("resolves Oracle numeric operator result column types exactly") {
            typeOf("SELECT id + 1 AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT salary + 1 AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT salary * 2 AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT id / 2 AS c FROM emp") shouldBe "java.math.BigDecimal"
            typeOf("SELECT bonus + salary AS c FROM emp") shouldBe "kotlin.Double?"
        }

        test("resolves Oracle concatenation operator result column types exactly") {
            typeOf("SELECT id || name AS c FROM emp") shouldBe "kotlin.String"
            typeOf("SELECT salary || name AS c FROM emp") shouldBe "kotlin.String"
        }

        test("resolves Oracle analytic function result column types exactly") {
            typeOf("SELECT ROW_NUMBER() OVER (ORDER BY id) AS c FROM emp") shouldBe "kotlin.Long"
            typeOf("SELECT LAG(salary) OVER (ORDER BY id) AS c FROM emp") shouldBe "java.math.BigDecimal?"
            typeOf("SELECT FIRST_VALUE(name) OVER (ORDER BY id) AS c FROM emp") shouldBe "kotlin.String?"
            typeOf("SELECT SUM(salary) OVER (PARTITION BY dept_id) AS c FROM emp") shouldBe "java.math.BigDecimal?"
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
