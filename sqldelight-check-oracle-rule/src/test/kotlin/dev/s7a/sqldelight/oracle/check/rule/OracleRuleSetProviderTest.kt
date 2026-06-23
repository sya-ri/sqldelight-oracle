package dev.s7a.sqldelight.oracle.check.rule

import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingConstraintStateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingFlashbackClauseRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingIndexClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingJsonStorageClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingSequenceClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoConflictingTableClausesRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoEmptyStringComparisonRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NoUppercaseRowidColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.NullableNotInPredicateRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferIdentityColumnRule
import dev.s7a.sqldelight.oracle.check.rule.rules.PreferUnifiedAuditingRule
import dev.s7a.sqldelight.oracle.check.rule.rules.RequireNumberPrecisionRule
import dev.s7a.sqldelight.oracle.check.rule.rules.UnsafeDdlMigrationRule
import dev.s7a.sqldelight.oracle.check.rule.rules.ValidAuditPolicyFormRule
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class OracleRuleSetProviderTest :
    FunSpec({
        test("provides Oracle rule set metadata") {
            val provider = OracleRuleSetProvider()
            val rules = provider.rules()

            provider.id.value shouldBe "oracle"
            rules.map { rule -> rule::class } shouldBe
                listOf(
                    NullableNotInPredicateRule::class,
                    NoEmptyStringComparisonRule::class,
                    NoConflictingConstraintStateRule::class,
                    NoConflictingFlashbackClauseRule::class,
                    NoConflictingIndexClausesRule::class,
                    NoConflictingJsonStorageClausesRule::class,
                    NoConflictingSequenceClausesRule::class,
                    NoConflictingTableClausesRule::class,
                    NoUppercaseRowidColumnRule::class,
                    PreferIdentityColumnRule::class,
                    PreferUnifiedAuditingRule::class,
                    RequireNumberPrecisionRule::class,
                    UnsafeDdlMigrationRule::class,
                    ValidAuditPolicyFormRule::class,
                    ValidRegexpMatchParamRule::class,
                    ValidDmlHintPlacementRule::class,
                    ValidFunctionArityRule::class,
                    ValidFormatModelRule::class,
                    ValidNlsParameterRule::class,
                    ValidJsonConditionOptionsRule::class,
                    ValidRowLimitingClauseRule::class,
                    ValidLikeEscapeRule::class,
                    ValidOuterJoinOperatorRule::class,
                    ValidSegmentCreationClauseRule::class,
                    ValidSubqueryRestrictionClauseRule::class,
                )
            rules.map { rule -> "oracle:${rule.id.value}" } shouldBe
                listOf(
                    "oracle:nullable-not-in-predicate",
                    "oracle:no-empty-string-comparison",
                    "oracle:no-conflicting-constraint-state",
                    "oracle:no-conflicting-flashback-clause",
                    "oracle:no-conflicting-index-clauses",
                    "oracle:no-conflicting-json-storage-clauses",
                    "oracle:no-conflicting-sequence-clauses",
                    "oracle:no-conflicting-table-clauses",
                    "oracle:no-uppercase-rowid-column",
                    "oracle:prefer-identity-column",
                    "oracle:prefer-unified-auditing",
                    "oracle:require-number-precision",
                    "oracle:unsafe-ddl-migration",
                    "oracle:valid-audit-policy-form",
                    "oracle:valid-regexp-match-param",
                    "oracle:valid-dml-hint-placement",
                    "oracle:valid-function-arity",
                    "oracle:valid-format-model",
                    "oracle:valid-nls-parameter",
                    "oracle:valid-json-condition-options",
                    "oracle:valid-row-limiting-clause",
                    "oracle:valid-like-escape",
                    "oracle:valid-outer-join-operator",
                    "oracle:valid-segment-creation-clause",
                    "oracle:valid-subquery-restriction-clause",
                )
            rules.map { rule -> rule.targetDialect } shouldBe
                List(rules.size) { OracleDialectId }
            rules.map { rule -> rule.defaultSeverity } shouldBe
                List(rules.size) { Severity.Warning }
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

private fun OracleRuleSetProvider.rules(): List<Rule> = ruleProviders().map { ruleProvider -> ruleProvider.create() }

private fun serviceResource(path: String): String =
    requireNotNull(OracleRuleSetProviderTest::class.java.classLoader.getResource(path)) {
        "Missing test resource $path"
    }.readText()
