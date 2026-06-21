package dev.s7a.sqldelight.oracle.dialect

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleDialectTest :
    FunSpec({
        test("registers SQLDelight dialect through ServiceLoader") {
            val dialects = ServiceLoader.load(SqlDelightDialect::class.java).toList()

            dialects.map { dialect -> dialect::class } shouldContain OracleDialect::class
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
    })
