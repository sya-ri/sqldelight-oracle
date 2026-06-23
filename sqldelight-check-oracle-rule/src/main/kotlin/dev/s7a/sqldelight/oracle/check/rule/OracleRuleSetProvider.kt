package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingConstraintStateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingCreateViewClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingDropClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingFlashbackClauseRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingIndexClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingJsonStorageClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingSequenceClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingSynonymClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingTableClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoUppercaseRowidColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NullableNotInPredicateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferIdentityColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferUnifiedAuditingRule
import dev.s7a.sqldelight.oracle.check.rule.rules.RequireNumberPrecisionRule
import dev.s7a.sqldelight.oracle.check.rule.rules.UnsafeDdlMigrationRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidAuditPolicyFormRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidBooleanTestConditionRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidDmlHintPlacementRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidFormatModelRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidFunctionArityRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidJsonConditionOptionsRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidLikeEscapeRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidNlsParameterRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidOuterJoinOperatorRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidRegexpMatchParamRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidRowLimitingClauseRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidSegmentCreationClauseRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidSubqueryRestrictionClauseRule

/**
 * Provides Oracle-specific sqldelight-check rules.
 */
public class OracleRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("oracle")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NullableNotInPredicateRule),
            RuleProvider(::NoEmptyStringComparisonRule),
            RuleProvider(::NoConflictingConstraintStateRule),
            RuleProvider(::NoConflictingCreateViewClausesRule),
            RuleProvider(::NoConflictingDropClausesRule),
            RuleProvider(::NoConflictingFlashbackClauseRule),
            RuleProvider(::NoConflictingIndexClausesRule),
            RuleProvider(::NoConflictingJsonStorageClausesRule),
            RuleProvider(::NoConflictingSequenceClausesRule),
            RuleProvider(::NoConflictingSynonymClausesRule),
            RuleProvider(::NoConflictingTableClausesRule),
            RuleProvider(::NoUppercaseRowidColumnRule),
            RuleProvider(::PreferIdentityColumnRule),
            RuleProvider(::PreferUnifiedAuditingRule),
            RuleProvider(::RequireNumberPrecisionRule),
            RuleProvider(::UnsafeDdlMigrationRule),
            RuleProvider(::ValidAuditPolicyFormRule),
            RuleProvider(::ValidBooleanTestConditionRule),
            RuleProvider(::ValidRegexpMatchParamRule),
            RuleProvider(::ValidDmlHintPlacementRule),
            RuleProvider(::ValidFunctionArityRule),
            RuleProvider(::ValidFormatModelRule),
            RuleProvider(::ValidNlsParameterRule),
            RuleProvider(::ValidJsonConditionOptionsRule),
            RuleProvider(::ValidRowLimitingClauseRule),
            RuleProvider(::ValidLikeEscapeRule),
            RuleProvider(::ValidOuterJoinOperatorRule),
            RuleProvider(::ValidSegmentCreationClauseRule),
            RuleProvider(::ValidSubqueryRestrictionClauseRule),
        )
}
