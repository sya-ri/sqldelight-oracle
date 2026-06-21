package dev.s7a.sqldelight.oracle.check.dialect

import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleDialectProviderTest :
    FunSpec({
        test("resolves the published Oracle SQLDelight dialect coordinate") {
            OracleDialectProvider().resolve(
                SqlDialectCoordinate("dev.s7a.sqldelight.oracle", "sqldelight-oracle-dialect", null),
            ) shouldBe OracleDialect
        }

        test("ignores unrelated coordinates") {
            OracleDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "postgresql-dialect", null)) shouldBe null
        }

        test("registers provider through ServiceLoader") {
            val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

            providers.map { provider -> provider::class } shouldContain OracleDialectProvider::class
        }

        test("recognizes Oracle statement starts and clauses") {
            val patterns = OracleDialectSourcePatterns

            patterns.matches(SqlDialectSourcePatternRole.StatementStart, listOf("merge")) shouldBe true
            patterns.matches(CreateSequenceStatementStart, listOf("create", "sequence")) shouldBe true
            patterns.matches(ConnectByClause, listOf("connect", "by")) shouldBe true
            patterns.matches(StartWithClause, listOf("start", "with")) shouldBe true
            patterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("varchar2")) shouldBe true
            patterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("vector")) shouldBe true
        }
    })
