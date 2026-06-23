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

        test("resolves Oracle argument-dependent function types exactly") {
            val parentResolver =
                ArgumentTypeResolver(
                    listOf(
                        OracleType.TEXT,
                        OracleType.INTEGER_NUMBER,
                    ),
                )
            val resolver = OracleDialect().typeResolver(parentResolver)
            val mappings =
                listOf(
                    Triple("NULLIF", 2, IntermediateType(OracleType.TEXT)),
                    Triple("FIRST_VALUE", 1, IntermediateType(OracleType.TEXT).asNullable()),
                    Triple("LAG", 2, IntermediateType(OracleType.TEXT).asNullable()),
                    Triple("LAST_VALUE", 1, IntermediateType(OracleType.TEXT).asNullable()),
                    Triple("LEAD", 2, IntermediateType(OracleType.TEXT).asNullable()),
                    Triple("NTH_VALUE", 2, IntermediateType(OracleType.TEXT).asNullable()),
                    Triple("TO_LOB", 1, IntermediateType(OracleType.TEXT)),
                    Triple("UNSUPPORTED_FUNCTION", 2, null),
                )

            mappings.map { (functionName, argumentCount, expectedType) ->
                Triple(
                    functionName,
                    argumentCount,
                    resolver.functionType(sqlFunctionExpr(functionName, parentResolver.exprList(argumentCount))),
                )
            } shouldBe mappings
        }

        test("resolves Oracle numeric aggregate function types exactly") {
            val integerArguments = ArgumentTypeResolver(listOf(OracleType.INTEGER_NUMBER))
            val decimalArguments = ArgumentTypeResolver(listOf(OracleType.DECIMAL_NUMBER))
            val binaryFloatArguments = ArgumentTypeResolver(listOf(OracleType.BINARY_FLOAT))
            val binaryDoubleArguments = ArgumentTypeResolver(listOf(OracleType.BINARY_DOUBLE))
            val integerResolver = OracleDialect().typeResolver(integerArguments)
            val decimalResolver = OracleDialect().typeResolver(decimalArguments)
            val binaryFloatResolver = OracleDialect().typeResolver(binaryFloatArguments)
            val binaryDoubleResolver = OracleDialect().typeResolver(binaryDoubleArguments)

            listOf(
                resolvedAggregate("AVG", integerResolver, integerArguments),
                resolvedAggregate("MEDIAN", decimalResolver, decimalArguments),
                resolvedAggregate("STDDEV", binaryFloatResolver, binaryFloatArguments),
                resolvedAggregate("STDDEV_POP", binaryDoubleResolver, binaryDoubleArguments),
                resolvedAggregate("STDDEV_SAMP", binaryFloatResolver, binaryFloatArguments),
                resolvedAggregate("VARIANCE", binaryDoubleResolver, binaryDoubleArguments),
                resolvedAggregate("VAR_POP", binaryFloatResolver, binaryFloatArguments),
                resolvedAggregate("VAR_SAMP", binaryDoubleResolver, binaryDoubleArguments),
            ) shouldBe
                listOf(
                    "AVG" to IntermediateType(OracleType.DECIMAL_NUMBER).asNullable(),
                    "MEDIAN" to IntermediateType(OracleType.DECIMAL_NUMBER).asNullable(),
                    "STDDEV" to IntermediateType(OracleType.BINARY_FLOAT).asNullable(),
                    "STDDEV_POP" to IntermediateType(OracleType.BINARY_DOUBLE).asNullable(),
                    "STDDEV_SAMP" to IntermediateType(OracleType.BINARY_FLOAT).asNullable(),
                    "VARIANCE" to IntermediateType(OracleType.BINARY_DOUBLE).asNullable(),
                    "VAR_POP" to IntermediateType(OracleType.BINARY_FLOAT).asNullable(),
                    "VAR_SAMP" to IntermediateType(OracleType.BINARY_DOUBLE).asNullable(),
                )
        }

        test("resolves Oracle vector distance shorthand result types exactly") {
            val resolver = OracleDialect().typeResolver(ArgumentTypeResolver(listOf(OracleType.TEXT)))

            listOf(
                "embedding <-> TO_VECTOR('[1,2,3]', 3, FLOAT32)",
                "embedding <=> TO_VECTOR('[1,2,3]', 3, FLOAT32)",
                "embedding <#> TO_VECTOR('[1,2,3]', 3, FLOAT32)",
            ).map { expression -> resolver.resolvedType(sqlExpr(expression)) } shouldBe
                listOf(
                    IntermediateType(OracleType.BINARY_DOUBLE),
                    IntermediateType(OracleType.BINARY_DOUBLE),
                    IntermediateType(OracleType.BINARY_DOUBLE),
                )
        }

        test("resolves Oracle JSON and XML return type clauses exactly") {
            val resolver = OracleDialect().typeResolver(ArgumentTypeResolver(listOf(OracleType.TEXT)))

            listOf(
                "JSON_VALUE" to "JSON_VALUE(payload, '$.id' RETURNING NUMBER ERROR ON ERROR)",
                "JSON_SERIALIZE" to "JSON_SERIALIZE(payload RETURNING BLOB ERROR ON ERROR)",
                "XMLSERIALIZE" to "XMLSERIALIZE(CONTENT payload AS CLOB)",
                "XMLCAST" to "XMLCAST(XMLQUERY('/Warehouse' PASSING payload RETURNING CONTENT) AS NUMBER(10, 2))",
            ).map { (functionName, text) ->
                resolver.functionType(sqlFunctionExpr(functionName, text = text))
            } shouldBe
                listOf(
                    IntermediateType(OracleType.DECIMAL_NUMBER),
                    IntermediateType(OracleType.BINARY),
                    IntermediateType(OracleType.TEXT),
                    IntermediateType(OracleType.DECIMAL_NUMBER),
                )
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

private class ArgumentTypeResolver(
    argumentTypes: List<OracleType>,
) : TypeResolver {
    private val exprList: List<SqlExpr> = argumentTypes.indices.map { index -> sqlExpr("argument$index") }
    private val typesByExpression =
        exprList.zip(argumentTypes.map { type -> IntermediateType(type) }).associate { (expression, type) -> expression to type }

    fun exprList(argumentCount: Int): List<SqlExpr> = exprList.take(argumentCount)

    override fun resolvedType(expr: SqlExpr): IntermediateType =
        requireNotNull(typesByExpression[expr]) {
            "Unexpected expression ${expr.text}"
        }

    override fun argumentType(
        parent: PsiElement,
        argument: SqlExpr,
    ): IntermediateType = resolvedType(argument)

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? = null

    override fun definitionType(typeName: SqlTypeName): IntermediateType = error("definitionType is not used by this test")

    override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? = error("queryWithResults is not used by this test")
}

private fun resolvedAggregate(
    functionName: String,
    resolver: TypeResolver,
    arguments: ArgumentTypeResolver,
): Pair<String, IntermediateType?> =
    functionName to resolver.functionType(sqlFunctionExpr(functionName, arguments.exprList(argumentCount = 1)))

private fun serviceResource(path: String): String =
    requireNotNull(OracleDialectTest::class.java.classLoader.getResource(path)) {
        "Missing test resource $path"
    }.readText()

private fun sqlFunctionExpr(
    name: String,
    argumentCount: Int,
): SqlFunctionExpr = sqlFunctionExpr(name, List(argumentCount) { sqlExpr() })

private fun sqlFunctionExpr(
    name: String,
    exprList: List<SqlExpr>,
    text: String = "$name()",
): SqlFunctionExpr =
    proxy(SqlFunctionExpr::class.java) { proxy, method, arguments ->
        when (method.name) {
            "getFunctionName" -> sqlFunctionName(name)
            "getExprList" -> exprList
            "getText" -> text
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "$name(${exprList.size} arguments)"
            else -> error("Unexpected SqlFunctionExpr method: ${method.name}")
        }
    }

private fun sqlFunctionExpr(
    name: String,
    text: String,
): SqlFunctionExpr = sqlFunctionExpr(name, emptyList(), text)

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

private fun sqlExpr(text: String = "argument"): SqlExpr =
    proxy(SqlExpr::class.java) { proxy, method, arguments ->
        when (method.name) {
            "getText" -> text
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> text
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
