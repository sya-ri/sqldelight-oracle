package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleRuleSetProviderTest :
    FunSpec({
        test("provides Oracle rule set metadata") {
            val provider = OracleRuleSetProvider()

            provider.id.value shouldBe "oracle"
            provider.ruleProviders().map { ruleProvider -> ruleProvider.create()::class } shouldContain NoEmptyStringComparisonRule::class
        }

        test("registers provider through ServiceLoader") {
            val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()

            providers.map { provider -> provider::class } shouldContain OracleRuleSetProvider::class
        }
    })
