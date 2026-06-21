package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NullableNotInPredicateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferIdentityColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.RequireNumberPrecisionRule
import dev.s7a.sqldelight.oracle.check.rule.rules.UnsafeDdlMigrationRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleRuleSetProviderTest :
    FunSpec({
        test("provides Oracle rule set metadata") {
            val provider = OracleRuleSetProvider()

            provider.id.value shouldBe "oracle"
            provider.ruleProviders().map { ruleProvider -> ruleProvider.create()::class } shouldBe
                listOf(
                    NullableNotInPredicateRule::class,
                    NoEmptyStringComparisonRule::class,
                    PreferIdentityColumnRule::class,
                    RequireNumberPrecisionRule::class,
                    UnsafeDdlMigrationRule::class,
                )
        }

        test("registers provider through ServiceLoader") {
            val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()

            providers.map { provider -> provider::class } shouldBe listOf(OracleRuleSetProvider::class)
        }

        test("publishes rule set provider ServiceLoader resource exactly") {
            serviceResource("META-INF/services/dev.s7a.sqldelight.check.rule.api.RuleSetProvider") shouldBe
                "dev.s7a.sqldelight.oracle.check.rule.OracleRuleSetProvider\n"
        }
    })

private fun serviceResource(path: String): String =
    requireNotNull(OracleRuleSetProviderTest::class.java.classLoader.getResource(path)) {
        "Missing test resource $path"
    }.readText()
