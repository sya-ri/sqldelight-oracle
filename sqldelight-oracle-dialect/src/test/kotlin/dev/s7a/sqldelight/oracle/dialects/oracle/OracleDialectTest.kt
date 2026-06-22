package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionName
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.psi.PsiElement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ServiceLoader

class OracleDialectTest :
    FunSpec({
        test("registers SQLDelight dialect through ServiceLoader") {
            val dialects = ServiceLoader.load(SqlDelightDialect::class.java).toList()

            dialects.map { dialect -> dialect::class } shouldBe listOf(OracleDialect::class)
        }

        test("publishes SQLDelight dialect ServiceLoader resource exactly") {
            serviceResource("META-INF/services/app.cash.sqldelight.dialect.api.SqlDelightDialect") shouldBe
                "dev.s7a.sqldelight.oracle.dialects.oracle.OracleDialect\n"
        }

        test("uses JDBC runtime types") {
            val dialect = OracleDialect()

            dialect.runtimeTypes.cursorType.canonicalName shouldBe "app.cash.sqldelight.driver.jdbc.JdbcCursor"
            dialect.runtimeTypes.preparedStatementType.canonicalName shouldBe
                "app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement"
        }

        test("does not allow create-table reference cycles") {
            OracleDialect().allowsReferenceCycles shouldBe false
        }

        test("uses Oracle type resolver") {
            OracleDialect().typeResolver(ParentTypeResolver)::class shouldBe OracleTypeResolver::class
        }

        test("resolves Oracle EXTRACT function overloads exactly") {
            val resolver = OracleDialect().typeResolver(ParentTypeResolver)

            resolver.functionType(sqlFunctionExpr("EXTRACT", argumentCount = 1)) shouldBe
                IntermediateType(OracleType.DECIMAL_NUMBER)
            resolver.functionType(sqlFunctionExpr("EXTRACT", argumentCount = 2)) shouldBe
                IntermediateType(OracleType.TEXT)
        }

        test("keeps optional dialect services explicit") {
            val dialect = OracleDialect()

            dialect.connectionManager shouldBe null
            dialect.isSqlite shouldBe false
            dialect.icon.iconWidth shouldBe 16
            dialect.icon.iconHeight shouldBe 16
            dialect.setup() shouldBe Unit
        }

        test("installs Oracle parser hooks") {
            SqlParserUtil.type_name = null

            OracleDialect().setup()

            SqlParserUtil.type_name.shouldNotBeNull()
        }
    })

private data object ParentTypeResolver : TypeResolver {
    override fun resolvedType(expr: SqlExpr): IntermediateType = error("resolvedType is not used by this test")

    override fun argumentType(
        parent: PsiElement,
        argument: SqlExpr,
    ): IntermediateType = error("argumentType is not used by this test")

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? = error("functionType is not used by this test")

    override fun definitionType(typeName: SqlTypeName): IntermediateType = error("definitionType is not used by this test")

    override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? = error("queryWithResults is not used by this test")
}

private fun serviceResource(path: String): String =
    requireNotNull(OracleDialectTest::class.java.classLoader.getResource(path)) {
        "Missing test resource $path"
    }.readText()

private fun sqlFunctionExpr(
    name: String,
    argumentCount: Int,
): SqlFunctionExpr =
    proxy(SqlFunctionExpr::class.java) { proxy, method, arguments ->
        when (method.name) {
            "getFunctionName" -> sqlFunctionName(name)
            "getExprList" -> List(argumentCount) { sqlExpr() }
            "getText" -> "$name()"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "$name($argumentCount arguments)"
            else -> error("Unexpected SqlFunctionExpr method: ${method.name}")
        }
    }

private fun sqlFunctionName(name: String): SqlFunctionName =
    proxy(SqlFunctionName::class.java) { proxy, method, arguments ->
        when (method.name) {
            "getText" -> name
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> name
            else -> error("Unexpected SqlFunctionName method: ${method.name}")
        }
    }

private fun sqlExpr(): SqlExpr =
    proxy(SqlExpr::class.java) { proxy, method, arguments ->
        when (method.name) {
            "getText" -> "argument"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "argument"
            else -> error("Unexpected SqlExpr method: ${method.name}")
        }
    }

private fun <T> proxy(
    type: Class<T>,
    handler: (proxy: Any, method: Method, arguments: Array<Any?>?) -> Any?,
): T =
    type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, arguments ->
            handler(proxy, method, arguments)
        },
    )
