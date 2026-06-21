package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NullableNotInPredicateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.RequireNumberPrecisionRule

/**
 * Provides Oracle-specific sqldelight-check rules.
 */
public class OracleRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("oracle")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NullableNotInPredicateRule),
            RuleProvider(::NoEmptyStringComparisonRule),
            RuleProvider(::RequireNumberPrecisionRule),
        )
}
