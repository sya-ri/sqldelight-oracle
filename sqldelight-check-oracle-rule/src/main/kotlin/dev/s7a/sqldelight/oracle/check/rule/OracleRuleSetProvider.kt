package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingSequenceClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NullableNotInPredicateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferIdentityColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.RequireNumberPrecisionRule
import dev.s7a.sqldelight.oracle.check.rule.rules.UnsafeDdlMigrationRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidDmlHintPlacementRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidFormatModelRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidFunctionArityRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidRegexpMatchParamRule

/**
 * Provides Oracle-specific sqldelight-check rules.
 */
public class OracleRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("oracle")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NullableNotInPredicateRule),
            RuleProvider(::NoEmptyStringComparisonRule),
            RuleProvider(::NoConflictingSequenceClausesRule),
            RuleProvider(::PreferIdentityColumnRule),
            RuleProvider(::RequireNumberPrecisionRule),
            RuleProvider(::UnsafeDdlMigrationRule),
            RuleProvider(::ValidRegexpMatchParamRule),
            RuleProvider(::ValidDmlHintPlacementRule),
            RuleProvider(::ValidFunctionArityRule),
            RuleProvider(::ValidFormatModelRule),
        )
}
