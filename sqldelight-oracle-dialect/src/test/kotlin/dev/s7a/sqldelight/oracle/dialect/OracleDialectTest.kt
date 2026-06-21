package dev.s7a.sqldelight.oracle.dialect

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import java.util.ServiceLoader

class OracleDialectTest :
    FunSpec({
        test("registers SQLDelight dialect through ServiceLoader") {
            val dialects = ServiceLoader.load(SqlDelightDialect::class.java).toList()

            dialects.map { dialect -> dialect::class } shouldContain OracleDialect::class
        }
    })
