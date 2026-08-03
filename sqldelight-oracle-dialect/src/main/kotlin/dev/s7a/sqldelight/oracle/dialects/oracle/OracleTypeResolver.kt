package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.psi.SqlDelightColumnType
import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.ExposableType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmt
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlInsertStmtValues
import com.alecstrong.sql.psi.core.psi.SqlMultiColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlMultiColumnExpression
import com.alecstrong.sql.psi.core.psi.SqlSetterExpression
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.ClassName
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY_DOUBLE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BINARY_FLOAT
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.BOOLEAN_TYPE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.DATE
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.DECIMAL_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.INTEGER_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.LONG_NUMBER
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.TIMESTAMP
import dev.s7a.sqldelight.oracle.dialects.oracle.OracleType.TIMESTAMP_TIME_ZONE
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins.indexOfKeyword
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins.oracleParenthesizedBodyAfter
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins.oracleParenthesizedBodyAt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins.oracleTopLevelCommaParts
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins.trimOracleIdentifier
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleMergeStmt
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleOracleVectorDistanceOperand

public class OracleTypeResolver(
    private val parentResolver: TypeResolver,
) : TypeResolver by parentResolver {
    override fun definitionType(typeName: SqlTypeName): IntermediateType = IntermediateType(OracleType.fromSqlTypeName(typeName.text))

    override fun resolvedType(expr: SqlExpr): IntermediateType =
        oracleVectorDistanceShorthandType(expr)
            ?: run {
                oracleExtensionConditionType(expr)
                    ?: oracleExtensionFunctionType(expr)
                    ?: oracleExtensionOperatorType(expr)
                    ?: oracleAtTimeZoneExpressionType(expr)
                    ?: oracleExtensionPseudocolumnType(expr)
                    ?: oracleExtensionLiteralType(expr)
                    ?: oracleCollateExpressionType(expr)
                    ?: oracleHierarchicalOperatorType(expr)
                    ?: oracleConcatenationOperatorType(expr)
                    ?: oracleDatetimeOperatorType(expr)
                    ?: oracleNumericOperatorType(expr)
                    ?: oracleEmptyStringLiteralType(expr)
                    ?: oracleCaseExpressionType(expr)
                    ?: parentResolver.resolvedType(expr)
            }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
        val functionName = functionExpr.functionName.text
        return oracleFunctionType(functionName, functionExpr.text, functionExpr.exprList)
            ?: parentResolver.functionType(functionExpr)
    }

    override fun argumentType(
        parent: PsiElement,
        argument: SqlExpr,
    ): IntermediateType {
        argument.oracleCastArgumentType()?.let { return it }
        argument.oracleFlashbackArgumentType()?.let { return it }
        argument.oraclePivotArgumentType()?.let { return it }
        argument.oracleErrorLoggingTagArgumentType()?.let { return it }
        argument.oracleVectorFunctionArgumentType()?.let { return it }
        argument.oracleFunctionArgumentType()?.let { return it }
        argument.oracleExtensionFunctionArgumentType()?.let { return it }

        return when {
            parent is SqlSetterExpression -> {
                parent.oracleSetterTargetType()
                    ?: argument.oracleInsertSetArgumentType()
                    ?: parentResolver.argumentType(parent, argument)
            }

            PsiTreeUtil.getParentOfType(argument, OracleMergeStmt::class.java) != null -> {
                argument.oracleMergeArgumentType() ?: parentResolver.argumentType(parent, argument)
            }

            PsiTreeUtil.getParentOfType(argument, SqlMultiColumnExpr::class.java) != null -> {
                argument.oracleMultiColumnArgumentType() ?: parentResolver.argumentType(parent, argument)
            }

            PsiTreeUtil.getParentOfType(argument, SqlExtensionExpr::class.java) != null -> {
                argument.oracleCastArgumentType()
                    ?: argument.oraclePivotArgumentType()
                    ?: argument.oracleMultiColumnTextArgumentType()
                    ?: argument.oracleExtensionArgumentType()
                    ?: parentResolver.argumentType(parent, argument)
            }

            else -> {
                argument.oracleInsertSetArgumentType()
                    ?: argument.oracleMultiTableInsertArgumentType()
                    ?: argument.oracleCastArgumentType()
                    ?: argument.oracleFlashbackArgumentType()
                    ?: argument.oraclePivotArgumentType()
                    ?: argument.oracleErrorLoggingTagArgumentType()
                    ?: parentResolver.argumentType(parent, argument)
            }
        }
    }

    private fun oracleExtensionFunctionType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        val functionName = extensionExpr.text.oracleFunctionName() ?: return null
        val invocationEnd = extensionExpr.text.oracleFirstFunctionInvocationEnd()
        val childExpressions = PsiTreeUtil.findChildrenOfType(extensionExpr, SqlExpr::class.java).toList()
        val invocationArguments =
            childExpressions
                .filter { argument ->
                    argument.textRange.startOffset - extensionExpr.textRange.startOffset < invocationEnd
                }.let { arguments ->
                    if (functionName == "ANY_VALUE") arguments.oracleOutermostExpressions() else arguments
                }
        val arguments =
            if (functionName.isOracleWithinGroupOrderedValueFunction()) {
                invocationArguments + childExpressions.oracleWithinGroupOrderingExpressions(extensionExpr)
            } else {
                invocationArguments
            }
        return oracleFunctionType(functionName, extensionExpr.text, arguments)
    }

    private fun List<SqlExpr>.oracleOutermostExpressions(): List<SqlExpr> =
        filter { candidate ->
            none { expression -> expression !== candidate && PsiTreeUtil.isAncestor(expression, candidate, false) }
        }

    private fun oracleExtensionOperatorType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        return when (val operatorName = extensionExpr.text.oracleLeadingIdentifier().substringBefore(".")) {
            "ORA_END_USER_CONTEXT" -> IntermediateType(OracleType.TEXT).asNullable()
            "JSON_ID" -> OracleType.fromFunctionName(operatorName)?.let { type -> IntermediateType(type) }
            "SHARD_CHUNK_ID" -> OracleType.fromFunctionName(operatorName)?.let { type -> IntermediateType(type) }
            else -> null
        }
    }

    private fun oracleExtensionPseudocolumnType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        return when (extensionExpr.text.oracleTerminalIdentifier()) {
            "COLUMN_VALUE",
            -> extensionExpr.oracleAvailableColumnType("COLUMN_VALUE")

            "CONNECT_BY_ISCYCLE",
            "CONNECT_BY_ISLEAF",
            "CURRVAL",
            "LEVEL",
            "NEXTVAL",
            "OBJECT_ID",
            "ORA_INVOKING_USERID",
            "ORA_ROWSCN",
            "ROWNUM",
            "UID",
            -> IntermediateType(LONG_NUMBER)

            "VERSIONS_ENDSCN",
            "VERSIONS_STARTSCN",
            -> IntermediateType(LONG_NUMBER).asNullable()

            "CURRENT_DATE",
            "SYSDATE",
            -> IntermediateType(DATE)

            "LOCALTIMESTAMP",
            -> IntermediateType(TIMESTAMP)

            "VERSIONS_ENDTIME",
            "VERSIONS_STARTTIME",
            -> IntermediateType(TIMESTAMP).asNullable()

            "CURRENT_TIMESTAMP",
            "SYSTIMESTAMP",
            -> IntermediateType(TIMESTAMP_TIME_ZONE)

            "VERSIONS_XID",
            -> IntermediateType(BINARY).asNullable()

            "DBTIMEZONE",
            "CURRENT_SCHEMA",
            "CURRENT_USER",
            "OBJECT_VALUE",
            "ORA_INVOKING_USER",
            "ORA_SHARDSPACE_NAME",
            "ROWID",
            "SESSION_USER",
            "SESSIONTIMEZONE",
            "USER",
            "XMLDATA",
            -> IntermediateType(OracleType.TEXT)

            "VERSIONS_OPERATION",
            -> IntermediateType(OracleType.TEXT).asNullable()

            else -> null
        }
    }

    private fun oracleExtensionLiteralType(expr: SqlExpr): IntermediateType? {
        val text =
            expr
                .oracleExtensionExpr()
                ?.text
                ?.trim()
                ?.uppercase()
                ?: return null
        return when {
            text == "TRUE" || text == "FALSE" || text == "UNKNOWN" -> IntermediateType(BOOLEAN_TYPE)
            text.startsWith("DATE ") -> IntermediateType(DATE)
            text.startsWith("TIMESTAMP ") && text.contains(" TIME ZONE ") -> IntermediateType(TIMESTAMP_TIME_ZONE)
            text.startsWith("TIMESTAMP ") -> IntermediateType(TIMESTAMP)
            text.startsWith("INTERVAL ") -> IntermediateType(OracleType.TEXT)
            else -> null
        }
    }

    private fun oracleEmptyStringLiteralType(expr: SqlExpr): IntermediateType? =
        IntermediateType(OracleType.TEXT)
            .asNullable()
            .takeIf { expr.text.isOracleEmptyStringLiteral() }

    private fun oracleCaseExpressionType(expr: SqlExpr): IntermediateType? {
        val text = expr.text.trimStart()
        if (!text.startsWith("CASE", ignoreCase = true)) return null
        val branches = expr.oracleCaseReturnExpressions()
        if (branches.isEmpty()) return null
        val resultExpressions = branches.map { branch -> branch.expression }
        val typedResultExpressions = resultExpressions.filterNot { expression -> expression.text.isOracleNullLiteral() }
        return encapsulatingTypePreferringKotlin(typedResultExpressions.ifEmpty { resultExpressions }, *COMPARABLE_TYPE_ORDER)
            .nullableIf(
                branches.none { branch -> branch.isElse } ||
                    resultExpressions.any { expression -> expression.text.isOracleNullLiteral() } ||
                    resultExpressions.any { expression -> resolvedType(expression).javaType.isNullable },
            )
    }

    private fun oracleExtensionConditionType(expr: SqlExpr): IntermediateType? {
        val text =
            expr
                .oracleExtensionExpr()
                ?.text
                ?.trim()
                ?: return null
        return IntermediateType(BOOLEAN_TYPE).takeIf { text.isOracleBooleanConditionExpression() }
    }

    private fun oracleVectorDistanceShorthandType(expr: SqlExpr): IntermediateType? {
        val extensionExpr =
            expr.oracleExtensionExpr()
                ?: return IntermediateType(BINARY_DOUBLE).asNullable().takeIf { expr.text.hasOracleVectorDistanceShorthand() }
        if (!extensionExpr.text.hasOracleVectorDistanceShorthand()) return null
        val operandTypes =
            PsiTreeUtil
                .findChildrenOfType(extensionExpr, OracleOracleVectorDistanceOperand::class.java)
                .toList()
                .takeIf { operands -> operands.size == 2 }
                ?.map { operand -> operand.oracleVectorDistanceOperandType() }
                ?: return IntermediateType(BINARY_DOUBLE).asNullable()
        return IntermediateType(BINARY_DOUBLE)
            .nullableIf(operandTypes.any { type -> type.javaType.isNullable || type.dialectType == TEXT })
    }

    private fun OracleOracleVectorDistanceOperand.oracleVectorDistanceOperandType(): IntermediateType =
        PsiTreeUtil
            .findChildrenOfType(this, SqlExpr::class.java)
            .toList()
            .takeIf { expressions -> expressions.isNotEmpty() }
            ?.let { expressions ->
                oracleFunctionType(text.oracleFunctionName().orEmpty(), text, expressions)
                    ?: expressions.singleOrNull()?.let(::resolvedType)
                    ?: IntermediateType(BINARY_DOUBLE)
                        .nullableIf(
                            expressions.any { expression ->
                                resolvedType(expression).javaType.isNullable
                            },
                        )
            }
            ?: oracleAvailableColumnType(this)
            ?: oracleAvailableColumnType(text)
            ?: IntermediateType(OracleType.TEXT)

    private fun oracleHierarchicalOperatorType(expr: SqlExpr): IntermediateType? {
        val text = expr.text.trimStart().uppercase()
        if (!text.startsWith("CONNECT_BY_ROOT ") && !text.startsWith("PRIOR ")) return null
        val operand =
            expr.children
                .filterIsInstance<SqlExpr>()
                .singleOrNull()
                ?: return null
        return resolvedType(operand)
    }

    private fun oracleNumericOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands) ?: return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        val operandDialectTypes = operandTypes.map { type -> type.dialectType }
        if (operandDialectTypes.any { type -> type !in NUMERIC_TYPE_ORDER }) return null

        val resultType =
            when {
                operator == "/" && operandDialectTypes.none { type -> type == REAL || type == BINARY_FLOAT || type == BINARY_DOUBLE } -> {
                    DECIMAL_NUMBER
                }

                else -> {
                    NUMERIC_TYPE_ORDER.last { type -> type in operandDialectTypes }
                }
            }
        return IntermediateType(resultType).nullableIf(operandTypes.any { type -> type.javaType.isNullable })
    }

    private fun oracleConcatenationOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands)
        if (operator != "||") return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        return IntermediateType(OracleType.TEXT).nullableIf(operandTypes.all { type -> type.javaType.isNullable })
    }

    private fun oracleDatetimeOperatorType(expr: SqlExpr): IntermediateType? {
        val operands =
            runCatching { expr.children.filterIsInstance<SqlExpr>() }
                .getOrNull()
                ?.takeIf { children -> children.size == 2 }
                ?: return null
        val operator = expr.oracleBinaryOperatorBetween(operands) ?: return null
        if (operator != "+" && operator != "-") return null
        val operandTypes = operands.map { operand -> resolvedType(operand) }
        val dialectTypes = operandTypes.map { type -> type.dialectType }
        val datetimeCount = dialectTypes.count { type -> type in DATETIME_TYPE_ORDER }
        if (datetimeCount == 0) return null
        val nullable = operandTypes.any { type -> type.javaType.isNullable }
        return when {
            // Oracle datetime subtraction: DATE - DATE yields a NUMBER of days, while any
            // subtraction involving TIMESTAMP/TIMESTAMP WITH TIME ZONE yields an INTERVAL DAY TO SECOND.
            operator == "-" && datetimeCount == 2 -> {
                if (dialectTypes.all { type -> type == DATE }) {
                    IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                } else {
                    IntermediateType(OracleType.TEXT).nullableIf(nullable)
                }
            }

            // Oracle interprets a number added to or subtracted from any datetime value as a number of
            // days, and the result is always a DATE (TIMESTAMP operands are converted to DATE first).
            datetimeCount == 1 && dialectTypes.any { type -> type in NUMERIC_TYPE_ORDER } -> {
                IntermediateType(DATE).nullableIf(nullable)
            }

            // Oracle datetime ± interval keeps the datetime operand's type
            // (DATE -> DATE, TIMESTAMP -> TIMESTAMP, TIMESTAMP WITH TIME ZONE -> TIMESTAMP WITH TIME ZONE).
            datetimeCount == 1 && operands.any { operand -> operand.isOracleIntervalOperand() } -> {
                val datetimeType = dialectTypes.first { type -> type in DATETIME_TYPE_ORDER }
                IntermediateType(datetimeType).nullableIf(nullable)
            }

            else -> {
                null
            }
        }
    }

    private fun SqlExpr.oracleExtensionExpr(): SqlExtensionExpr? =
        this as? SqlExtensionExpr
            ?: runCatching { children.filterIsInstance<SqlExtensionExpr>().singleOrNull() }.getOrNull()

    private fun oracleCollateExpressionType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        val operandText =
            extensionExpr
                .text
                .oracleCollateOperandText()
                ?: return null
        return when {
            operandText.startsWith("'") ||
                operandText.startsWith(
                    "N'",
                    ignoreCase = true,
                ) ||
                operandText.startsWith(
                    "Q'",
                    ignoreCase = true,
                ) -> {
                IntermediateType(OracleType.TEXT)
            }

            else -> {
                extensionExpr.oracleAvailableColumnType(operandText)
                    ?: PsiTreeUtil
                        .findChildrenOfType(extensionExpr, SqlExpr::class.java)
                        .singleOrNull()
                        ?.let(::resolvedType)
            }
        }
    }

    private fun oracleAtTimeZoneExpressionType(expr: SqlExpr): IntermediateType? {
        val extensionExpr = expr.oracleExtensionExpr() ?: return null
        val operandText = extensionExpr.text.oracleAtTimeZoneOperandText() ?: return null
        val operandType =
            when {
                operandText.startsWith("(") -> {
                    PsiTreeUtil
                        .findChildrenOfType(extensionExpr, SqlExpr::class.java)
                        .firstOrNull()
                        ?.let(::resolvedType)
                }

                else -> {
                    extensionExpr.oracleAvailableColumnType(operandText)
                        ?: OracleType.fromFunctionName(operandText.oracleTerminalIdentifier())?.let(::IntermediateType)
                }
            } ?: return null
        return IntermediateType(TIMESTAMP_TIME_ZONE)
            .nullableIf(operandType.javaType.isNullable)
    }

    private fun String.oracleAtTimeZoneOperandText(): String? =
        Regex("""(?is)^\s*(.+?)\s+AT\s+TIME\s+ZONE\b""")
            .find(this)
            ?.groupValues
            ?.get(1)
            ?.trim()

    private fun SqlSetterExpression.oracleSetterTargetType(): IntermediateType? {
        val owner = parent ?: return null
        val setterStart = textRange.startOffset - owner.textRange.startOffset
        val beforeSetter = owner.text.substring(0, setterStart)
        val targetText =
            Regex("""(?is)(?:\bSET\b|,)\s*([A-Za-z_][\w$#]*(?:\s*\.\s*[A-Za-z_][\w$#]*)*)\s*=\s*$""")
                .find(beforeSetter)
                ?.groupValues
                ?.get(1)
                ?: return null
        return owner.oracleAvailableColumnType(targetText)
    }

    private fun SqlExpr.oracleMultiColumnArgumentType(): IntermediateType? {
        val expression = PsiTreeUtil.getParentOfType(this, SqlMultiColumnExpression::class.java) ?: return null
        val multiColumnExpr = expression.parent as? SqlMultiColumnExpr ?: return null
        val expressionIndex = multiColumnExpr.multiColumnExpressionList.indexOf(expression)
        if (expressionIndex <= 0) return null
        val argumentIndex = expression.exprList.indexOf(this)
        if (argumentIndex == -1) return null

        return multiColumnExpr
            .multiColumnExpressionList
            .firstOrNull()
            ?.exprList
            ?.getOrNull(argumentIndex)
            ?.let(::resolvedType)
    }

    private fun SqlExpr.oracleExtensionArgumentType(): IntermediateType? {
        val extensionExpr = PsiTreeUtil.getParentOfType(this, SqlExtensionExpr::class.java) ?: return null
        val extensionText = extensionExpr.text
        val argumentOffset = textRange.startOffset - extensionExpr.textRange.startOffset
        if (argumentOffset !in extensionText.indices) return null
        val beforeArgument = extensionText.substring(0, argumentOffset)

        if (Regex("""(?is)\bLIKE[234C]?\s*$""").containsMatchIn(beforeArgument)) {
            return IntermediateType(TEXT)
        }
        if (Regex("""(?is)\bESCAPE\s*$""").containsMatchIn(beforeArgument)) {
            return IntermediateType(TEXT)
        }
        if (
            Regex("""(?is)\bXMLELEMENT\s*\(\s*(?:(?:ENTITYESCAPING|NOENTITYESCAPING)\s+)?EVALNAME\s*$""")
                .containsMatchIn(beforeArgument)
        ) {
            return IntermediateType(TEXT)
        }
        oracleVectorArgumentType(extensionExpr)?.let { return it }
        oracle26AiConversionArgumentType(extensionExpr)?.let { return it }
        oracleTimeBucketArgumentType(extensionExpr, argumentOffset)?.let { return it }

        val regexpLikeStart = Regex("""(?is)\bREGEXP_LIKE\s*\(""").find(extensionText)?.range?.last ?: return null
        if (argumentOffset <= regexpLikeStart) return null
        val argumentIndex = extensionText.substring(regexpLikeStart + 1, argumentOffset).oracleTopLevelCommaParts().size - 1
        return when (argumentIndex) {
            1 -> IntermediateType(TEXT)
            2 -> IntermediateType(TEXT).asNullable()
            else -> null
        }
    }

    private fun SqlExpr.oracle26AiConversionArgumentType(extensionExpr: SqlExtensionExpr): IntermediateType? {
        val functionName = extensionExpr.text.oracleFunctionName() ?: return null
        if (functionName !in setOf("TO_BOOLEAN", "TO_UTC_TIMESTAMP_TZ")) return null

        val argumentOffset = textRange.startOffset - extensionExpr.textRange.startOffset
        val invocationStart = Regex("""(?is)^\s*$functionName\s*\(""").find(extensionExpr.text)?.range?.last ?: return null
        if (argumentOffset <= invocationStart) return null

        return IntermediateType(TEXT)
    }

    private fun oracleTimeBucketArgumentType(
        extensionExpr: SqlExtensionExpr,
        argumentOffset: Int,
    ): IntermediateType? {
        if (extensionExpr.text.oracleFunctionName() != "TIME_BUCKET") return null
        val invocationStart = extensionExpr.text.indexOf('(').takeIf { it >= 0 } ?: return null
        if (argumentOffset <= invocationStart) return null
        val argumentIndex =
            extensionExpr.text
                .substring(invocationStart + 1, argumentOffset)
                .oracleTopLevelCommaParts()
                .size - 1
        return when (argumentIndex) {
            0, 2 -> IntermediateType(TIMESTAMP).asNullable()
            1 -> IntermediateType(TEXT)
            else -> null
        }
    }

    private fun SqlExpr.oracleVectorArgumentType(extensionExpr: SqlExtensionExpr): IntermediateType? {
        val argumentOffset = textRange.startOffset - extensionExpr.textRange.startOffset
        if (argumentOffset !in extensionExpr.text.indices) return null
        val beforeArgument = extensionExpr.text.substring(0, argumentOffset)
        val invocation =
            Regex("""(?is)\b(?:VECTOR|TO_VECTOR)\s*\(""")
                .findAll(beforeArgument)
                .lastOrNull()
                ?: return null
        val functionOpenOffset = invocation.range.last
        val invocationPrefix =
            extensionExpr
                .text
                .substring(functionOpenOffset + 1, argumentOffset)
        val argumentIndex =
            if (invocationPrefix.isBlank()) {
                0
            } else {
                invocationPrefix.oracleTopLevelCommaParts().size - 1
            }
        return when (argumentIndex) {
            0 -> IntermediateType(TEXT)
            1 -> IntermediateType(LONG_NUMBER)
            else -> null
        }
    }

    private fun SqlExpr.oracleVectorFunctionArgumentType(): IntermediateType? {
        val functionExpr = PsiTreeUtil.getParentOfType(this, SqlFunctionExpr::class.java) ?: return null
        if (!functionExpr.functionName.text.equals("VECTOR", ignoreCase = true) &&
            !functionExpr.functionName.text.equals("TO_VECTOR", ignoreCase = true)
        ) {
            return null
        }
        return when (functionExpr.exprList.indexOf(this)) {
            0 -> IntermediateType(TEXT)
            1 -> IntermediateType(LONG_NUMBER)
            else -> null
        }
    }

    private fun SqlExpr.oracleFunctionArgumentType(): IntermediateType? {
        val functionExpr = PsiTreeUtil.getParentOfType(this, SqlFunctionExpr::class.java) ?: return null
        val argumentIndex = functionExpr.exprList.indexOf(this)
        if (argumentIndex == -1) return null

        return oracleFunctionArgumentType(functionExpr.functionName.text, argumentIndex, functionExpr.exprList)
    }

    private fun SqlExpr.oracleExtensionFunctionArgumentType(): IntermediateType? {
        val extensionExpr = PsiTreeUtil.getParentOfType(this, SqlExtensionExpr::class.java) ?: return null
        val functionName = extensionExpr.text.oracleFunctionName() ?: return null
        val invocationEnd = extensionExpr.text.oracleFirstFunctionInvocationEnd()
        val arguments =
            PsiTreeUtil
                .findChildrenOfType(extensionExpr, SqlExpr::class.java)
                .filter { expression ->
                    expression.textRange.startOffset - extensionExpr.textRange.startOffset < invocationEnd
                }.toList()
        val argumentIndex = arguments.indexOf(this)
        if (argumentIndex == -1) return null

        return oracleFunctionArgumentType(functionName, argumentIndex, arguments)
    }

    private fun SqlExpr.oracleFunctionArgumentType(
        rawFunctionName: String,
        argumentIndex: Int,
        arguments: List<SqlExpr>,
    ): IntermediateType? {
        val functionName = rawFunctionName.trim().uppercase()

        val siblingType: (Int) -> IntermediateType? = { index ->
            arguments.getOrNull(index)?.takeUnless { expression -> expression == this }?.oracleResolvedTypeOrNull()
        }
        val firstSiblingType =
            arguments.firstNotNullOfOrNull { expression ->
                expression.takeUnless { it == this }?.oracleResolvedTypeOrNull()
            }

        return when (functionName) {
            "COALESCE", "GREATEST", "LEAST", "NANVL" -> {
                firstSiblingType
            }

            "NVL" -> {
                siblingType(0) ?: siblingType(1)
            }

            "NVL2" -> {
                when (argumentIndex) {
                    0 -> siblingType(1) ?: siblingType(2)
                    1 -> siblingType(2)
                    2 -> siblingType(1)
                    else -> null
                }
            }

            "DECODE" -> {
                when {
                    argumentIndex == 0 -> {
                        siblingType(1)
                    }

                    argumentIndex % 2 == 1 -> {
                        siblingType(0)
                    }

                    else -> {
                        arguments.withIndex().firstNotNullOfOrNull { (index, expression) ->
                            if (index >= 2 && index % 2 == 0 && expression != this) {
                                expression.oracleResolvedTypeOrNull()
                            } else {
                                null
                            }
                        }
                    }
                }
            }

            "LAG", "LEAD" -> {
                when (argumentIndex) {
                    0 -> siblingType(2)
                    1 -> IntermediateType(LONG_NUMBER)
                    2 -> siblingType(0)
                    else -> null
                }
            }

            "JSON_DATAGUIDE", "JSON_EXISTS", "JSON_QUERY", "JSON_VALUE" -> {
                if (argumentIndex == 0) firstSiblingType ?: IntermediateType(TEXT) else IntermediateType(TEXT)
            }

            "NTH_VALUE" -> {
                when (argumentIndex) {
                    0 -> null
                    1 -> IntermediateType(LONG_NUMBER)
                    else -> null
                }
            }

            "LOWER", "UPPER", "INITCAP", "LTRIM", "RTRIM", "TRIM",
            "ASCII", "ASCIISTR", "COMPOSE", "DECOMPOSE", "LENGTH", "LENGTHB", "LENGTHC", "LENGTH2", "LENGTH4",
            "TO_DATE", "TO_TIMESTAMP", "TO_TIMESTAMP_TZ", "TO_NUMBER",
            -> {
                IntermediateType(TEXT)
            }

            "CONCAT" -> {
                IntermediateType(TEXT)
            }

            else -> {
                null
            }
        }
    }

    private fun SqlExpr.oracleResolvedTypeOrNull(): IntermediateType? = runCatching { resolvedType(this) }.getOrNull()

    private fun SqlExpr.oracleMultiColumnTextArgumentType(): IntermediateType? {
        val extensionExpr = PsiTreeUtil.getParentOfType(this, SqlExtensionExpr::class.java) ?: return null
        val expressionText = extensionExpr.text
        val argumentOffset = textRange.startOffset - extensionExpr.textRange.startOffset
        if (argumentOffset !in expressionText.indices) return null

        val leftTuple =
            Regex("""(?is)\(([^()]+)\)\s*(?:=|IN)\s*\(""")
                .find(expressionText)
                ?.groupValues
                ?.get(1)
                ?: return null
        val argumentIndex =
            expressionText
                .substring(0, argumentOffset)
                .substringAfterLast("(")
                .oracleTopLevelCommaParts()
                .size - 1

        val columnName =
            leftTuple
                .oracleTopLevelCommaParts()
                .getOrNull(argumentIndex)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
        return extensionExpr.oracleAvailableColumnType(columnName)
    }

    private fun SqlExpr.oraclePivotArgumentType(): IntermediateType? {
        var current: PsiElement? = parent
        while (current != null) {
            val pivotType = current.oraclePivotArgumentType(this)
            if (pivotType != null) return pivotType
            current = current.parent
        }
        return null
    }

    private fun PsiElement.oraclePivotArgumentType(argument: SqlExpr): IntermediateType? {
        val pivotBody = text.oracleParenthesizedBodyAfter("PIVOT") ?: return null
        val pivotBodyOffset = text.indexOf(pivotBody).takeIf { it >= 0 } ?: return null
        val argumentOffset = argument.textRange.startOffset - textRange.startOffset
        if (argumentOffset !in pivotBodyOffset..<(pivotBodyOffset + pivotBody.length)) return null

        val forOffset = pivotBody.indexOfKeyword("FOR") ?: return null
        val inOffset = pivotBody.indexOfKeyword("IN", startIndex = forOffset + "FOR".length) ?: return null
        val pivotColumns =
            pivotBody
                .substring(forOffset + "FOR".length, inOffset)
                .oracleIdentifierList()
        if (pivotColumns.isEmpty()) return null

        val relativeArgumentOffset = argumentOffset - pivotBodyOffset
        val inBodyStart = pivotBody.indexOf('(', startIndex = inOffset).takeIf { it >= 0 } ?: return null
        if (relativeArgumentOffset <= inBodyStart) return null
        val entryPrefix = pivotBody.substring(inBodyStart + 1, relativeArgumentOffset).substringAfterLast("(")
        val argumentIndex = entryPrefix.oracleTopLevelCommaParts().size - 1
        val columnName = pivotColumns.getOrNull(argumentIndex) ?: return null
        return oracleAvailableTableColumnType(columnName)
            ?: argument.oracleAvailableColumnType(columnName)
    }

    private fun String.oracleIdentifierList(): List<String> =
        trim()
            .removeSurrounding("(", ")")
            .let { body ->
                Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
                    .findAll(body)
                    .map { match -> match.value.trimOracleIdentifier() }
                    .toList()
            }

    private fun SqlExpr.oracleCastArgumentType(): IntermediateType? {
        val functionExpr = PsiTreeUtil.getParentOfType(this, SqlFunctionExpr::class.java)
        if (functionExpr != null && functionExpr.functionName.text.isOracleCastLikeFunctionName()) {
            val argumentOffset = textRange.startOffset - functionExpr.textRange.startOffset
            val asOffset = functionExpr.text.indexOfKeyword("AS") ?: return null
            if (argumentOffset <= asOffset) {
                return functionExpr.text.oracleCastTargetType()
            }
        }

        return oracleCastExtensionArgumentType()
    }

    private fun SqlExpr.oracleCastExtensionArgumentType(): IntermediateType? {
        val extensionExpr = PsiTreeUtil.getParentOfType(this, SqlExtensionExpr::class.java) ?: return null
        val functionName = extensionExpr.text.oracleLeadingIdentifier()
        if (!functionName.isOracleCastLikeFunctionName()) return null

        val argumentTextRange = textRange ?: return null
        val extensionTextRange = extensionExpr.textRange ?: return null
        val argumentOffset = argumentTextRange.startOffset - extensionTextRange.startOffset
        val asOffset = extensionExpr.text.indexOfKeyword("AS") ?: return null
        if (argumentOffset > asOffset) return null

        return extensionExpr.text.oracleCastTargetType()
    }

    private fun SqlExpr.oracleFlashbackArgumentType(): IntermediateType? {
        val fileText = containingFile.text
        val argumentTextRange = textRange
        if (argumentTextRange != null && argumentTextRange.startOffset in fileText.indices) {
            val beforeArgument = fileText.substring(0, argumentTextRange.startOffset)
            when {
                beforeArgument.hasOracleScnFlashbackBoundary() -> return IntermediateType(LONG_NUMBER)
                beforeArgument.hasOracleTimestampFlashbackBoundary() -> return IntermediateType(TIMESTAMP)
            }
        }

        var current: PsiElement? = parent
        while (current != null) {
            val flashbackType = current.oracleFlashbackArgumentType(this)
            if (flashbackType != null) return flashbackType
            current = current.parent
        }
        return null
    }

    private fun PsiElement.oracleFlashbackArgumentType(argument: SqlExpr): IntermediateType? {
        val parentTextRange = textRange ?: return null
        val argumentTextRange = argument.textRange ?: return null
        val argumentOffset = argumentTextRange.startOffset - parentTextRange.startOffset
        if (argumentOffset !in text.indices) return null

        val beforeArgument = text.substring(0, argumentOffset)
        return when {
            beforeArgument.hasOracleScnFlashbackBoundary() -> IntermediateType(LONG_NUMBER)
            beforeArgument.hasOracleTimestampFlashbackBoundary() -> IntermediateType(TIMESTAMP)
            else -> null
        }
    }

    private fun String.hasOracleScnFlashbackBoundary(): Boolean =
        contains(Regex("""(?is)\bAS\s+OF\s+SCN\s*$""")) ||
            contains(Regex("""(?is)\bVERSIONS\s+BETWEEN\s+SCN\s+(?:.*\bAND\s+)?$"""))

    private fun String.hasOracleTimestampFlashbackBoundary(): Boolean =
        contains(Regex("""(?is)\bAS\s+OF\s+TIMESTAMP\s*$""")) ||
            contains(Regex("""(?is)\bAS\s+OF\s+PERIOD\s+FOR\s+[A-Za-z_][A-Za-z0-9_$#]*\s*$""")) ||
            contains(Regex("""(?is)\bVERSIONS\s+BETWEEN\s+TIMESTAMP\s+(?:.*\bAND\s+)?$""")) ||
            contains(Regex("""(?is)\bVERSIONS\s+PERIOD\s+FOR\s+[A-Za-z_][A-Za-z0-9_$#]*\s+BETWEEN\s+(?:.*\bAND\s+)?$"""))

    private fun SqlExpr.oracleMergeArgumentType(): IntermediateType? {
        val mergeStmt = PsiTreeUtil.getParentOfType(this, OracleMergeStmt::class.java) ?: return null
        return mergeStmt.oracleMergeAssignmentArgumentType(this)
            ?: mergeStmt.oracleMergeComparisonArgumentType(this)
            ?: mergeStmt.oracleMergeInsertArgumentType(this)
    }

    private fun OracleMergeStmt.oracleMergeAssignmentArgumentType(argument: SqlExpr): IntermediateType? =
        oracleAssignmentTargetType(argument)

    private fun OracleMergeStmt.oracleMergeComparisonArgumentType(argument: SqlExpr): IntermediateType? {
        val argumentStart = argument.textRange.startOffset - textRange.startOffset
        if (argumentStart !in text.indices) return null
        val argumentEnd = argument.textRange.endOffset - textRange.startOffset
        if (argumentEnd !in 0..text.length) return null

        val beforeArgument = text.substring(0, argumentStart)
        val leftOperandText =
            Regex("""(?is)([A-Za-z_][\w$#]*(?:\s*\.\s*[A-Za-z_][\w$#]*)*)\s*(?:=|<>|!=|<=|>=|<|>)\s*$""")
                .find(beforeArgument)
                ?.groupValues
                ?.get(1)
        if (leftOperandText != null) {
            return oracleAvailableColumnType(leftOperandText)
        }

        val afterArgument = text.substring(argumentEnd)
        val rightOperandText =
            Regex("""(?is)^\s*(?:=|<>|!=|<=|>=|<|>)\s*([A-Za-z_][\w$#]*(?:\s*\.\s*[A-Za-z_][\w$#]*)*)""")
                .find(afterArgument)
                ?.groupValues
                ?.get(1)
                ?: return null
        return oracleAvailableColumnType(rightOperandText)
    }

    private fun OracleMergeStmt.oracleMergeInsertArgumentType(argument: SqlExpr): IntermediateType? =
        oracleColumnTypeForValue(argument, columnsKeyword = "INSERT")

    private fun SqlExpr.oracleMultiTableInsertArgumentType(): IntermediateType? {
        val insertStmt = PsiTreeUtil.getParentOfType(this, SqlInsertStmt::class.java) ?: return null
        return insertStmt.oracleMultiTableInsertArgumentType(this)
    }

    private fun SqlExpr.oracleInsertSetArgumentType(): IntermediateType? {
        val insertStmt = PsiTreeUtil.getParentOfType(this, SqlInsertStmt::class.java) ?: return null
        return insertStmt.oracleInsertSetArgumentType(this)
    }

    private fun SqlInsertStmt.oracleInsertSetArgumentType(argument: SqlExpr): IntermediateType? {
        val valuesText = PsiTreeUtil.getChildOfType(this, SqlInsertStmtValues::class.java)?.text ?: return null
        if (!valuesText.trimStart().startsWith("SET ", ignoreCase = true)) return null
        return oracleAssignmentTargetType(argument)
    }

    private fun SqlInsertStmt.oracleMultiTableInsertArgumentType(argument: SqlExpr): IntermediateType? {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("INSERT ALL", ignoreCase = true) && !trimmed.startsWith("INSERT FIRST", ignoreCase = true)) {
            return null
        }

        return oracleColumnTypeForValue(argument, columnsKeyword = "INTO")
    }

    private fun PsiElement.oracleAssignmentTargetType(argument: SqlExpr): IntermediateType? {
        val argumentStart = argument.textRange.startOffset - textRange.startOffset
        if (argumentStart !in text.indices) return null
        val beforeArgument = text.substring(0, argumentStart)
        val targetText =
            Regex("""(?is)(?:\bSET\b|,)\s*([A-Za-z_][\w$#]*(?:\s*\.\s*[A-Za-z_][\w$#]*)*)\s*=\s*$""")
                .find(beforeArgument)
                ?.groupValues
                ?.get(1)
                ?: return null
        return oracleAvailableColumnType(targetText)
    }

    private fun PsiElement.oracleColumnTypeForValue(
        argument: SqlExpr,
        columnsKeyword: String,
    ): IntermediateType? {
        val argumentStart = argument.textRange.startOffset - textRange.startOffset
        if (argumentStart !in text.indices) return null

        val valuesKeywordOffset =
            Regex("""(?i)\bVALUES\b""")
                .findAll(text.substring(0, argumentStart))
                .lastOrNull()
                ?.range
                ?.first
                ?: return null
        val valuesOpenOffset = text.indexOf('(', startIndex = valuesKeywordOffset).takeIf { it != -1 } ?: return null
        val valueIndex = text.substring(valuesOpenOffset + 1, argumentStart).oracleTopLevelCommaParts().size - 1

        val beforeValues = text.substring(0, valuesKeywordOffset)
        val columnsKeywordOffset =
            Regex("""(?i)\b${Regex.escape(columnsKeyword)}\b""")
                .findAll(beforeValues)
                .lastOrNull()
                ?.range
                ?.first
                ?: return null
        val columnsOpenOffset = text.indexOf('(', startIndex = columnsKeywordOffset).takeIf { it in 0..<valuesKeywordOffset } ?: return null
        val columnName =
            text
                .oracleParenthesizedBodyAt(columnsOpenOffset)
                .oracleTopLevelCommaParts()
                .getOrNull(valueIndex)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        return oracleAvailableColumnType(columnName)
    }

    private fun SqlExpr.oracleErrorLoggingTagArgumentType(): IntermediateType? {
        var current: PsiElement? = parent
        while (current != null && current !is PsiFile) {
            val argumentStart = textRange.startOffset - current.textRange.startOffset
            if (argumentStart in current.text.indices) {
                val beforeArgument = current.text.substring(0, argumentStart)
                val logErrorsOffset =
                    Regex("""(?i)\bLOG\s+ERRORS\b""")
                        .findAll(beforeArgument)
                        .lastOrNull()
                        ?.range
                        ?.first
                if (logErrorsOffset != null && beforeArgument.substring(logErrorsOffset).lastIndexOf('(') != -1) {
                    return IntermediateType(TEXT)
                }
            }
            if (current.isOracleDmlStatement()) break
            current = current.parent
        }
        return null
    }

    private fun PsiElement.isOracleDmlStatement(): Boolean =
        this is SqlInsertStmt ||
            this is SqlUpdateStmt ||
            this is SqlUpdateStmtLimited ||
            this is SqlDeleteStmt ||
            this is SqlDeleteStmtLimited ||
            this is OracleMergeStmt

    private fun PsiElement.oracleAvailableColumnType(operandText: String): IntermediateType? {
        val operandColumnName = operandText.substringAfterLast(".").trimOracleIdentifier()
        if (operandColumnName.isBlank()) return null
        return oracleAvailableColumnTypeForColumn(operandColumnName)
    }

    private fun PsiElement.oracleAvailableColumnType(operand: PsiElement): IntermediateType? {
        val operandColumnName =
            PsiTreeUtil
                .findChildrenOfType(operand, NamedElement::class.java)
                .lastOrNull()
                ?.name
                ?.trimOracleIdentifier()
                ?: return null
        if (operandColumnName.isBlank()) return null
        return oracleAvailableColumnTypeForColumn(operandColumnName)
    }

    private fun PsiElement.oracleAvailableColumnTypeForColumn(operandColumnName: String): IntermediateType? =
        oracleAvailableColumnTypeInScopes(operandColumnName) { context ->
            queryAvailable(context)
                .asSequence()
                .flatMap { result -> result.columns.asSequence() }
        }

    private fun PsiElement.oracleAvailableTableColumnType(operandText: String): IntermediateType? {
        val operandColumnName = operandText.substringAfterLast(".").trimOracleIdentifier()
        if (operandColumnName.isBlank()) return null

        return oracleAvailableColumnTypeInScopes(operandColumnName) { context ->
            tablesAvailable(context)
                .asSequence()
                .flatMap { table -> table.query.columns.asSequence() }
        }
    }

    private fun PsiElement.oracleAvailableColumnTypeInScopes(
        operandColumnName: String,
        columnsAvailable: SqlCompositeElement.(PsiElement) -> Sequence<QueryColumn>?,
    ): IntermediateType? {
        var current: PsiElement? = this
        while (current != null) {
            val queryElement = current as? SqlCompositeElement
            val column =
                queryElement
                    ?.columnsAvailable(this)
                    ?.firstOrNull { column ->
                        (column.element as? NamedElement)?.name.equals(operandColumnName, ignoreCase = true)
                    }
            val exposedType = column?.element as? ExposableType
            if (exposedType != null) return exposedType.type()
            val columnDef = column?.element?.parent as? SqlColumnDef
            if (columnDef != null) return columnDef.oracleColumnDefinitionType()
            current = current.parent
        }
        return null
    }

    private fun SqlColumnDef.oracleColumnDefinitionType(): IntermediateType {
        val baseType = definitionType(columnType.typeName)
        val customType = (columnType as? SqlDelightColumnType)?.javaTypeName?.text
        if (!customType.isNullOrBlank() && !customType.equals("VALUE", ignoreCase = true)) {
            val customIntermediateType =
                baseType.copy(
                    javaType = ClassName.bestGuess(customType),
                    column = this,
                )
            return customIntermediateType.nullableIf(!text.contains(Regex("""(?i)\bNOT\s+NULL\b""")))
        }
        return baseType.nullableIf(!text.contains(Regex("""(?i)\bNOT\s+NULL\b""")))
    }

    private fun SqlExpr.oracleBinaryOperatorBetween(operands: List<SqlExpr>): String? {
        val leftEnd = operands[0].textRange.endOffset - textRange.startOffset
        val rightStart = operands[1].textRange.startOffset - textRange.startOffset
        val between = text.substring(leftEnd, rightStart)
        return listOf("||", "*", "/", "+", "-").firstOrNull { operator -> operator in between }
    }

    private fun SqlExpr.isOracleIntervalOperand(): Boolean {
        val normalized = text.trim().uppercase()
        return normalized.startsWith("INTERVAL ") ||
            ORACLE_INTERVAL_FUNCTION_REGEX.containsMatchIn(normalized)
    }

    private fun oracleFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        argumentDependentFunctionType(functionName, functionText, exprList)
            ?: returningClauseFunctionType(functionName, functionText, exprList)
            ?: nullableAggregateFunctionType(functionName)
            ?: OracleType.fromFunctionName(functionName)?.let { type ->
                val propagatesNull = functionName.isOracleNullPropagatingFixedReturnFunction()
                val hasNullableInput =
                    functionName.isOracleDefaultNullableSqlJsonFunction() ||
                        functionName.isOracleNullableDomainFunction() ||
                        (propagatesNull && exprList.any { expression -> resolvedType(expression).javaType.isNullable })
                IntermediateType(type)
                    .nullableIf(hasNullableInput)
            }

    private fun nullableAggregateFunctionType(functionName: String): IntermediateType? =
        OracleType
            .fromFunctionName(functionName)
            ?.takeIf { functionName.isOracleNullableAggregateFunction() }
            ?.let { type -> IntermediateType(type).asNullable() }

    private fun returningClauseFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        when (functionName.trim().uppercase()) {
            "JSON_ARRAY",
            "JSON_OBJECT",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }
            }

            "JSON_ARRAYAGG",
            "JSON_OBJECTAGG",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName ->
                    IntermediateType(OracleType.fromSqlTypeName(typeName)).asNullable()
                }
            }

            "JSON_VALUE",
            "JSON_QUERY",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName ->
                    IntermediateType(OracleType.fromSqlTypeName(typeName))
                        .nullableIf(
                            functionName.isOracleDefaultNullableSqlJsonFunction() ||
                                functionText.hasOracleSqlJsonNullReturningClause() ||
                                exprList.firstOrNull()?.let { expression -> resolvedType(expression).javaType.isNullable } == true,
                        )
                }
            }

            "JSON_MERGEPATCH",
            "JSON_TRANSFORM",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName ->
                    IntermediateType(OracleType.fromSqlTypeName(typeName))
                        .nullableIf(
                            functionText.hasOracleSqlJsonNullReturningClause() ||
                                exprList.any { expression -> resolvedType(expression).javaType.isNullable },
                        )
                }
            }

            "JSON_SERIALIZE",
            -> {
                functionText.oracleReturningTypeName()?.let { typeName ->
                    IntermediateType(OracleType.fromSqlTypeName(typeName))
                        .nullableIf(
                            functionText.hasOracleSqlJsonNullReturningClause() ||
                                exprList.firstOrNull()?.let { expression -> resolvedType(expression).javaType.isNullable } == true,
                        )
                }
            }

            "XMLQUERY",
            -> {
                IntermediateType(OracleType.TEXT)
                    .nullableIf(functionText.hasOracleXmlNullReturningClause())
            }

            "XMLSERIALIZE",
            -> {
                IntermediateType(functionText.oracleCastTypeName()?.let(OracleType::fromSqlTypeName) ?: OracleType.TEXT)
                    .nullableIf(exprList.firstOrNull()?.let { expression -> resolvedType(expression).javaType.isNullable } == true)
            }

            "CAST",
            "XMLCAST",
            "TREAT",
            -> {
                functionText.oracleCastTypeName()?.let { typeName ->
                    IntermediateType(OracleType.fromSqlTypeName(typeName))
                        .nullableIf(
                            exprList.firstOrNull()?.let { expression -> resolvedType(expression).javaType.isNullable } == true ||
                                exprList.firstOrNull()?.text.isOracleNullLiteral() ||
                                functionText.hasOracleNullCastInput() ||
                                (
                                    functionName.equals("CAST", ignoreCase = true) &&
                                        functionText.hasOracleDefaultNullOnConversionError()
                                ),
                        )
                }
            }

            else -> {
                null
            }
        }

    private fun argumentDependentFunctionType(
        functionName: String,
        functionText: String,
        exprList: List<SqlExpr>,
    ): IntermediateType? =
        when (functionName.trim().lowercase()) {
            "abs" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).takeIf { type -> type.dialectType in NUMERIC_TYPE_ORDER }
                }
            }

            "acos", "asin", "atan", "cos", "cosh", "exp", "ln", "sin", "sinh", "sqrt", "tan", "tanh" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).singleNumericFunctionType()
                }
            }

            "atan2", "log" -> {
                exprList
                    .takeIf { args -> args.size == 2 }
                    ?.map { expression ->
                        resolvedType(expression)
                    }?.binaryNumericFunctionType()
            }

            "ceil", "floor" -> {
                exprList.singleOrNull()?.let { expression ->
                    resolvedType(expression).ceilOrFloorSingleArgumentType()
                }
            }

            "median", "approx_median" -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER, DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        in DATETIME_TYPE_ORDER -> {
                            resolvedType(expression).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "mod", "remainder" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
                        .nullableIf(args.any { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "power" -> {
                exprList
                    .takeIf { args -> args.size == 2 }
                    ?.map { expression ->
                        resolvedType(expression)
                    }?.let { argumentTypes ->
                        val nullable = argumentTypes.any { type -> type.javaType.isNullable }
                        when {
                            argumentTypes.any { type ->
                                type.dialectType == REAL ||
                                    type.dialectType == BINARY_FLOAT ||
                                    type.dialectType == BINARY_DOUBLE
                            } -> {
                                IntermediateType(BINARY_DOUBLE).nullableIf(nullable)
                            }

                            argumentTypes.all { type -> type.dialectType in NUMERIC_TYPE_ORDER } -> {
                                IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                            }

                            else -> {
                                null
                            }
                        }
                    }
            }

            "round", "trunc" -> {
                when (exprList.size) {
                    1 -> {
                        resolvedType(exprList.single()).roundOrTruncSingleArgumentType()
                    }

                    2 -> {
                        exprList
                            .map { expression ->
                                resolvedType(expression)
                            }.roundOrTruncTwoArgumentType()
                    }

                    else -> {
                        null
                    }
                }
            }

            "coalesce" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.all { isNullable -> isNullable } },
                    )
                }
            }

            "nvl" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    val resultType =
                        if (args.first().text.isOracleNullLiteral()) {
                            resolvedType(args[1])
                        } else {
                            resolvedType(args.first())
                        }
                    resultType.nullableIf(args.all { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "concat" -> {
                exprList.takeIf { args -> args.size >= 2 }?.let { args ->
                    IntermediateType(OracleType.TEXT)
                        .nullableIf(args.all { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "nullif" -> {
                exprList.takeIf { args -> args.size == 2 }?.firstOrNull()?.let { expression ->
                    resolvedType(expression).asNullable()
                }
            }

            "nvl2" -> {
                exprList.takeIf { args -> args.size == 3 }?.let { args ->
                    val selectorNullable = resolvedType(args[0]).javaType.isNullable
                    val resultArgs = args.drop(1)
                    encapsulatingTypePreferringKotlin(
                        resultArgs,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability ->
                            if (selectorNullable) {
                                nullability.any { isNullable -> isNullable }
                            } else {
                                nullability.first()
                            }
                        },
                    )
                }
            }

            "greatest", "least" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(
                        args,
                        *COMPARABLE_TYPE_ORDER,
                        nullability = { nullability -> nullability.any { isNullable -> isNullable } },
                    )
                }
            }

            "decode" -> {
                exprList.drop(1).takeIf { args -> args.size >= 2 }?.let { args ->
                    val resultExpressions =
                        args
                            .withIndex()
                            .filter { (index) -> index % 2 == 1 || (index == args.lastIndex && args.size % 2 == 1) }
                            .map { (_, expression) -> expression }
                    encapsulatingTypePreferringKotlin(resultExpressions, *COMPARABLE_TYPE_ORDER)
                        .nullableIf(
                            args.size % 2 == 0 ||
                                resultExpressions.any { expression -> resolvedType(expression).javaType.isNullable },
                        )
                }
            }

            "nanvl" -> {
                exprList.takeIf { args -> args.size == 2 }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *NUMERIC_TYPE_ORDER)
                        .nullableIf(args.any { expression -> resolvedType(expression).javaType.isNullable })
                }
            }

            "max" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *MAX_TYPE_ORDER).asNullable()
                }
            }

            "min" -> {
                exprList.takeIf { args -> args.isNotEmpty() }?.let { args ->
                    encapsulatingTypePreferringKotlin(args, *MIN_TYPE_ORDER).asNullable()
                }
            }

            "avg",
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
            -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER, DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "sum" -> {
                exprList.singleOrNull()?.let { expression ->
                    when (resolvedType(expression).dialectType) {
                        INTEGER, INTEGER_NUMBER, LONG_NUMBER -> {
                            IntermediateType(LONG_NUMBER).asNullable()
                        }

                        DECIMAL_NUMBER -> {
                            IntermediateType(DECIMAL_NUMBER).asNullable()
                        }

                        REAL -> {
                            IntermediateType(REAL).asNullable()
                        }

                        BINARY_FLOAT -> {
                            IntermediateType(BINARY_FLOAT).asNullable()
                        }

                        BINARY_DOUBLE -> {
                            IntermediateType(BINARY_DOUBLE).asNullable()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }

            "percentile_cont", "percentile_disc" -> {
                exprList.getOrNull(1)?.let { expression ->
                    resolvedType(expression).asNullable()
                }
            }

            "approx_percentile" -> {
                when {
                    functionText.hasOracleApproxPercentileDiagnosticReturn() -> {
                        IntermediateType(DECIMAL_NUMBER).asNullable()
                    }

                    else -> {
                        exprList.lastOrNull()?.let { expression -> resolvedType(expression).asNullable() }
                    }
                }
            }

            "any_value", "stats_mode" -> {
                exprList.singleOrNull()?.let { expression -> resolvedType(expression).asNullable() }
            }

            "first_value", "lag", "last_value", "lead", "nth_value" -> {
                exprList.firstOrNull()?.let { expression -> resolvedType(expression).asNullable() }
            }

            "to_lob" -> {
                exprList.singleOrNull()?.let { expression ->
                    OracleType
                        .fromToLobArgumentType(resolvedType(expression).dialectType)
                        ?.let { type ->
                            IntermediateType(type)
                                .nullableIf(resolvedType(expression).javaType.isNullable)
                        }
                }
            }

            "userenv" -> {
                exprList.singleOrNull()?.let { expression ->
                    IntermediateType(OracleType.fromUserEnvParameter(expression.text))
                }
            }

            "extract" -> {
                val nullable =
                    exprList.any { expression ->
                        runCatching { resolvedType(expression).javaType.isNullable }.getOrDefault(false)
                    }
                when (exprList.size) {
                    1 -> {
                        when (functionText.oracleExtractDatetimeField()) {
                            "TIMEZONE_REGION", "TIMEZONE_ABBR" -> IntermediateType(OracleType.TEXT).nullableIf(nullable)
                            null -> null
                            else -> IntermediateType(DECIMAL_NUMBER).nullableIf(nullable)
                        }
                    }

                    2 -> {
                        IntermediateType(OracleType.TEXT).nullableIf(nullable)
                    }

                    else -> {
                        null
                    }
                }
            }

            else -> {
                null
            }
        }

    private companion object {
        private val VECTOR_DISTANCE_SHORTHAND_OPERATORS = listOf("<->", "<=>", "<#>")

        private val ORACLE_INTERVAL_FUNCTION_REGEX = Regex("""\b(?:NUMTODSINTERVAL|NUMTOYMINTERVAL|TO_DSINTERVAL|TO_YMINTERVAL)\s*\(""")

        private fun String.hasOracleVectorDistanceShorthand(): Boolean =
            VECTOR_DISTANCE_SHORTHAND_OPERATORS.any { operator -> contains(operator) }

        private fun String.oracleCollateOperandText(): String? =
            indexOfKeyword("COLLATE")
                ?.let { collateOffset -> substring(0, collateOffset).trim() }
                ?.takeIf { operand -> operand.isNotBlank() }

        private fun String.isOracleNullPropagatingFixedReturnFunction(): Boolean {
            val functionName = trim().uppercase()
            return functionName in
                setOf(
                    "ACOS",
                    "ADD_MONTHS",
                    "ASCII",
                    "ASCIISTR",
                    "ASIN",
                    "ATAN",
                    "ATAN2",
                    "BFILENAME",
                    "BIN_TO_NUM",
                    "BITAND",
                    "BITMAP_BIT_POSITION",
                    "BITMAP_BUCKET_NUMBER",
                    "BITMAP_COUNT",
                    "CHARTOROWID",
                    "CHR",
                    "COMPOSE",
                    "CONVERT",
                    "COS",
                    "COSH",
                    "DECOMPOSE",
                    "DATEDIFF",
                    "DUMP",
                    "EXP",
                    "FUZZY_MATCH",
                    "FROM_VECTOR",
                    "FROM_TZ",
                    "HEXTORAW",
                    "INITCAP",
                    "INSTR",
                    "INSTRB",
                    "INSTRC",
                    "INSTR2",
                    "INSTR4",
                    "LAST_DAY",
                    "LENGTH",
                    "LENGTHB",
                    "LENGTHC",
                    "LENGTH2",
                    "LENGTH4",
                    "LN",
                    "LOG",
                    "LOWER",
                    "LPAD",
                    "LTRIM",
                    "MONTHS_BETWEEN",
                    "NEW_TIME",
                    "NEXT_DAY",
                    "NCHR",
                    "NLS_CHARSET_DECL_LEN",
                    "NLS_CHARSET_ID",
                    "NLS_CHARSET_NAME",
                    "NLS_COLLATION_ID",
                    "NLS_COLLATION_NAME",
                    "NLS_INITCAP",
                    "NLS_LOWER",
                    "NLSSORT",
                    "NLS_UPPER",
                    "NUMTODSINTERVAL",
                    "NUMTOYMINTERVAL",
                    "ORA_DST_AFFECTED",
                    "ORA_DST_CONVERT",
                    "ORA_DST_ERROR",
                    "PHONIC_ENCODE",
                    "REGEXP_COUNT",
                    "REGEXP_INSTR",
                    "REGEXP_REPLACE",
                    "REGEXP_SUBSTR",
                    "REPLACE",
                    "RAWTOHEX",
                    "RAWTONHEX",
                    "RAW_TO_UUID",
                    "RATIO_TO_REPORT",
                    "ROWIDTOCHAR",
                    "ROWIDTONCHAR",
                    "RPAD",
                    "RTRIM",
                    "SCN_TO_TIMESTAMP",
                    "SIGN",
                    "SIN",
                    "SINH",
                    "SOUNDEX",
                    "SQRT",
                    "STANDARD_HASH",
                    "SUBSTR",
                    "SUBSTRB",
                    "SUBSTRC",
                    "SUBSTR2",
                    "SUBSTR4",
                    "SYS_EXTRACT_UTC",
                    "TAN",
                    "TANH",
                    "TO_CLOB",
                    "TO_BLOB",
                    "TO_BINARY_DOUBLE",
                    "TO_BINARY_FLOAT",
                    "TO_BOOLEAN",
                    "TO_CHAR",
                    "TO_DATE",
                    "TO_DSINTERVAL",
                    "TO_MULTI_BYTE",
                    "TO_NCHAR",
                    "TO_NCLOB",
                    "TO_NUMBER",
                    "TO_SINGLE_BYTE",
                    "TO_TIMESTAMP",
                    "TO_TIMESTAMP_TZ",
                    "TO_UTC_TIMESTAMP_TZ",
                    "TO_VECTOR",
                    "TO_YMINTERVAL",
                    "TRANSLATE",
                    "TRIM",
                    "TIMESTAMP_TO_SCN",
                    "TIMESTAMPDIFF",
                    "TZ_OFFSET",
                    "UNISTR",
                    "UPPER",
                    "UUID_TO_RAW",
                    "VECTOR_DISTANCE",
                    "VECTOR_DIMENSION_COUNT",
                    "VECTOR_DIMENSION_FORMAT",
                    "VECTOR_DIMS",
                    "VECTOR_EMBEDDING",
                    "L1_DISTANCE",
                    "L2_DISTANCE",
                    "COSINE_DISTANCE",
                    "INNER_PRODUCT",
                    "HAMMING_DISTANCE",
                    "JACCARD_DISTANCE",
                    "VECTOR_NORM",
                    "VECTOR_SERIALIZE",
                    "VSIZE",
                    "WIDTH_BUCKET",
                    "XMLDIFF",
                    "XMLPATCH",
                    "XMLTRANSFORM",
                    "XMLTYPE",
                ) || functionName in oracleNullPropagatingCalendarFunctions()
        }

        private fun oracleNullPropagatingCalendarFunctions(): Set<String> =
            setOf(
                "CALENDAR_ADD_DAYS",
                "CALENDAR_ADD_MONTHS",
                "CALENDAR_ADD_QUARTERS",
                "CALENDAR_ADD_WEEKS",
                "CALENDAR_ADD_YEARS",
                "CALENDAR_DAY",
                "CALENDAR_DAY_OF_MONTH",
                "CALENDAR_DAY_OF_QUARTER",
                "CALENDAR_DAY_OF_WEEK",
                "CALENDAR_DAY_OF_YEAR",
                "CALENDAR_MONTH",
                "CALENDAR_MONTH_END_DATE",
                "CALENDAR_MONTH_OF_QUARTER",
                "CALENDAR_MONTH_OF_YEAR",
                "CALENDAR_MONTH_START_DATE",
                "CALENDAR_QUARTER",
                "CALENDAR_QUARTER_END_DATE",
                "CALENDAR_QUARTER_OF_YEAR",
                "CALENDAR_QUARTER_START_DATE",
                "CALENDAR_SINCE",
                "CALENDAR_WEEK",
                "CALENDAR_WEEK_END_DATE",
                "CALENDAR_WEEK_OF_YEAR",
                "CALENDAR_WEEK_START_DATE",
                "CALENDAR_YEAR",
                "CALENDAR_YEAR_END_DATE",
                "CALENDAR_YEAR_NUMBER",
                "CALENDAR_YEAR_START_DATE",
                "FISCAL_ADD_DAYS",
                "FISCAL_ADD_MONTHS",
                "FISCAL_ADD_QUARTERS",
                "FISCAL_ADD_WEEKS",
                "FISCAL_ADD_YEARS",
                "FISCAL_DAY",
                "FISCAL_DAY_OF_MONTH",
                "FISCAL_DAY_OF_QUARTER",
                "FISCAL_DAY_OF_WEEK",
                "FISCAL_DAY_OF_YEAR",
                "FISCAL_MONTH",
                "FISCAL_MONTH_END_DATE",
                "FISCAL_MONTH_OF_QUARTER",
                "FISCAL_MONTH_OF_YEAR",
                "FISCAL_MONTH_START_DATE",
                "FISCAL_QUARTER",
                "FISCAL_QUARTER_END_DATE",
                "FISCAL_QUARTER_OF_YEAR",
                "FISCAL_QUARTER_START_DATE",
                "FISCAL_WEEK",
                "FISCAL_WEEK_END_DATE",
                "FISCAL_WEEK_OF_YEAR",
                "FISCAL_WEEK_START_DATE",
                "FISCAL_YEAR",
                "FISCAL_YEAR_END_DATE",
                "FISCAL_YEAR_NUMBER",
                "FISCAL_YEAR_START_DATE",
                "RETAIL_ADD_DAYS",
                "RETAIL_ADD_MONTHS",
                "RETAIL_ADD_QUARTERS",
                "RETAIL_ADD_WEEKS",
                "RETAIL_ADD_YEARS",
                "RETAIL_DAY",
                "RETAIL_DAY_EXISTS",
                "RETAIL_DAY_OF_MONTH",
                "RETAIL_DAY_OF_QUARTER",
                "RETAIL_DAY_OF_WEEK",
                "RETAIL_DAY_OF_YEAR",
                "RETAIL_MONTH",
                "RETAIL_MONTH_END_DATE",
                "RETAIL_MONTH_OF_QUARTER",
                "RETAIL_MONTH_OF_YEAR",
                "RETAIL_MONTH_START_DATE",
                "RETAIL_QUARTER",
                "RETAIL_QUARTER_END_DATE",
                "RETAIL_QUARTER_OF_YEAR",
                "RETAIL_QUARTER_START_DATE",
                "RETAIL_WEEK",
                "RETAIL_WEEK_END_DATE",
                "RETAIL_WEEK_OF_MONTH",
                "RETAIL_WEEK_OF_QUARTER",
                "RETAIL_WEEK_OF_YEAR",
                "RETAIL_WEEK_START_DATE",
                "RETAIL_YEAR",
                "RETAIL_YEAR_END_DATE",
                "RETAIL_YEAR_NUMBER",
                "RETAIL_YEAR_START_DATE",
            )

        private fun String.isOracleNullableAggregateFunction(): Boolean =
            trim().uppercase() in
                setOf(
                    "APPROX_MEDIAN",
                    "APPROX_PERCENTILE",
                    "APPROX_SUM",
                    "BIT_AND_AGG",
                    "BIT_OR_AGG",
                    "BIT_XOR_AGG",
                    "BITMAP_CONSTRUCT_AGG",
                    "BITMAP_OR_AGG",
                    "BOOLEAN_AND_AGG",
                    "BOOLEAN_OR_AGG",
                    "CORR",
                    "COVAR_POP",
                    "COVAR_SAMP",
                    "EVERY",
                    "KURTOSIS_POP",
                    "KURTOSIS_SAMP",
                    "LISTAGG",
                    "PERCENTILE_CONT",
                    "PERCENTILE_DISC",
                    "REGR_AVGX",
                    "REGR_AVGY",
                    "REGR_INTERCEPT",
                    "REGR_R2",
                    "REGR_SLOPE",
                    "REGR_SXX",
                    "REGR_SXY",
                    "REGR_SYY",
                    "SKEWNESS_POP",
                    "SKEWNESS_SAMP",
                    "STATS_BINOMIAL_TEST",
                    "STATS_CROSSTAB",
                    "STATS_F_TEST",
                    "STATS_KS_TEST",
                    "STATS_MW_TEST",
                    "STATS_ONE_WAY_ANOVA",
                    "STATS_T_TEST_ONE",
                    "STATS_T_TEST_PAIRED",
                    "STATS_T_TEST_INDEP",
                    "STATS_T_TEST_INDEPU",
                    "STATS_WSR_TEST",
                )

        private fun String.isOracleDefaultNullableSqlJsonFunction(): Boolean = trim().uppercase() in setOf("JSON_QUERY", "JSON_VALUE")

        private fun String.isOracleNullableDomainFunction(): Boolean = trim().uppercase() in setOf("DOMAIN_DISPLAY", "DOMAIN_ORDER")

        private fun String.oracleFunctionName(): String? =
            Regex("""(?i)^\s*(?:[A-Z_][A-Z0-9_$#]*\s*\.\s*)*([A-Z_][A-Z0-9_$#]*)\s*\(""")
                .find(this)
                ?.groupValues
                ?.get(1)
                ?.uppercase()

        private fun String.isOracleWithinGroupOrderedValueFunction(): Boolean =
            trim().uppercase() in setOf("APPROX_PERCENTILE", "PERCENTILE_CONT", "PERCENTILE_DISC")

        private fun String.hasOracleApproxPercentileDiagnosticReturn(): Boolean {
            val normalized = uppercase()
            return "DETERMINISTIC" in normalized && ("'ERROR_RATE'" in normalized || "'CONFIDENCE'" in normalized)
        }

        private fun List<SqlExpr>.oracleWithinGroupOrderingExpressions(extensionExpr: SqlExtensionExpr): List<SqlExpr> {
            val orderByStart = extensionExpr.text.oracleWithinGroupOrderByExpressionStart() ?: return emptyList()
            val withinGroupEnd = extensionExpr.text.oracleWithinGroupClauseEnd(orderByStart) ?: return emptyList()
            val extensionStart = extensionExpr.textRange.startOffset
            return filter { expression ->
                val relativeStart = expression.textRange.startOffset - extensionStart
                relativeStart in orderByStart..<withinGroupEnd
            }
        }

        private fun String.oracleWithinGroupOrderByExpressionStart(): Int? =
            Regex("""(?i)\bWITHIN\s+GROUP\s*\(\s*ORDER\s+BY\s+""")
                .find(this)
                ?.range
                ?.last
                ?.plus(1)

        private fun String.oracleWithinGroupClauseEnd(orderByStart: Int): Int? {
            val openParen = lastIndexOf('(', startIndex = orderByStart).takeIf { index -> index >= 0 } ?: return null
            var depth = 0
            var inStringLiteral = false
            var index = openParen
            while (index < length) {
                val char = this[index]
                if (inStringLiteral) {
                    if (char == '\'' && getOrNull(index + 1) == '\'') {
                        index += 2
                        continue
                    }
                    if (char == '\'') {
                        inStringLiteral = false
                    }
                } else {
                    when (char) {
                        '\'' -> {
                            inStringLiteral = true
                        }

                        '(' -> {
                            depth += 1
                        }

                        ')' -> {
                            depth -= 1
                            if (depth == 0) return index
                        }
                    }
                }
                index += 1
            }
            return null
        }

        private fun String.oracleTerminalIdentifier(): String =
            trim()
                .substringBefore("(")
                .substringAfterLast(".")
                .trim()
                .uppercase()

        private fun String.oracleLeadingIdentifier(): String =
            trim()
                .substringBefore("(")
                .trim()
                .uppercase()

        private fun String.hasOracleSqlJsonNullReturningClause(): Boolean =
            Regex("""\bNULL\s+ON\s+(?:EMPTY|ERROR)\b""", RegexOption.IGNORE_CASE).containsMatchIn(this)

        private fun String.hasOracleXmlNullReturningClause(): Boolean =
            Regex("""\bNULL\s+ON\s+EMPTY\b""", RegexOption.IGNORE_CASE).containsMatchIn(this)

        private fun String.oracleFirstFunctionInvocationEnd(): Int {
            val start = indexOf('(').takeIf { index -> index >= 0 } ?: return length
            var depth = 0
            var inStringLiteral = false
            var index = start
            while (index < length) {
                val char = this[index]
                if (inStringLiteral) {
                    if (char == '\'' && getOrNull(index + 1) == '\'') {
                        index += 2
                        continue
                    }
                    if (char == '\'') {
                        inStringLiteral = false
                    }
                } else {
                    when (char) {
                        '\'' -> {
                            inStringLiteral = true
                        }

                        '(' -> {
                            depth += 1
                        }

                        ')' -> {
                            depth -= 1
                            if (depth == 0) return index + 1
                        }
                    }
                }
                index += 1
            }
            return length
        }

        private fun String.oracleReturningTypeName(): String? = oracleTypeNameAfterKeyword("RETURNING")

        private fun String.oracleCastTypeName(): String? = oracleTypeNameAfterKeyword("AS")

        private fun String.oracleCastTargetType(): IntermediateType? =
            oracleCastTypeName()
                ?.let { typeName -> IntermediateType(OracleType.fromSqlTypeName(typeName)) }

        private fun String.isOracleCastLikeFunctionName(): Boolean =
            equals("CAST", ignoreCase = true) ||
                equals("XMLCAST", ignoreCase = true) ||
                equals("TREAT", ignoreCase = true)

        private fun String.hasOracleDefaultNullOnConversionError(): Boolean {
            val asOffset = indexOfKeyword("AS") ?: return false
            val defaultOffset = indexOfKeyword("DEFAULT", startIndex = asOffset + "AS".length) ?: return false
            val onOffset = indexOfKeyword("ON", startIndex = defaultOffset + "DEFAULT".length) ?: return false
            val conversionOffset = indexOfKeyword("CONVERSION", startIndex = onOffset + "ON".length) ?: return false
            indexOfKeyword("ERROR", startIndex = conversionOffset + "CONVERSION".length) ?: return false

            return substring(defaultOffset + "DEFAULT".length, onOffset).trim().equals("NULL", ignoreCase = true)
        }

        private fun String.isOracleEmptyStringLiteral(): Boolean {
            val value = trim()
            return value == "''" ||
                value.equals("N''", ignoreCase = true) ||
                value.isOracleEmptyAlternativeQuotedString()
        }

        private fun String?.isOracleNullLiteral(): Boolean = this?.trim().equals("NULL", ignoreCase = true)

        private fun String.hasOracleNullCastInput(): Boolean =
            Regex("""(?i)^\s*(?:XMLCAST|CAST|TREAT)\s*\(\s*NULL\s+AS\b""")
                .containsMatchIn(this)

        private fun String.isOracleEmptyAlternativeQuotedString(): Boolean {
            val value = trim()
            val openDelimiterIndex =
                when {
                    value.startsWith("q'", ignoreCase = true) -> 2
                    value.startsWith("nq'", ignoreCase = true) -> 3
                    else -> return false
                }
            if (value.length < openDelimiterIndex + 3 || value[openDelimiterIndex - 1] != '\'') return false

            val closeDelimiter =
                when (val openDelimiter = value[openDelimiterIndex]) {
                    '[' -> ']'
                    '{' -> '}'
                    '(' -> ')'
                    '<' -> '>'
                    else -> openDelimiter
                }
            return value.length == openDelimiterIndex + 3 &&
                value[openDelimiterIndex + 1] == closeDelimiter &&
                value[openDelimiterIndex + 2] == '\''
        }

        private fun SqlExpr.oracleCaseReturnExpressions(): List<OracleCaseReturnExpression> =
            children
                .filterIsInstance<SqlExpr>()
                .mapNotNull { child ->
                    val localStart = child.textRange.startOffset - textRange.startOffset
                    val keyword =
                        text
                            .take(localStart.coerceAtLeast(0))
                            .oracleTrailingCaseReturnKeyword()
                            ?: return@mapNotNull null
                    OracleCaseReturnExpression(child, keyword.equals("ELSE", ignoreCase = true))
                }

        private fun String.oracleTrailingCaseReturnKeyword(): String? =
            Regex("""(?i)\b(THEN|ELSE)\s*$""")
                .find(this.trimEnd())
                ?.groupValues
                ?.get(1)

        private fun String.oracleExtractDatetimeField(): String? =
            Regex("""(?i)^\s*EXTRACT\s*\(\s*([A-Z_]+)\s+FROM\b""")
                .find(this)
                ?.groupValues
                ?.get(1)
                ?.uppercase()

        private fun String.isOracleBooleanConditionExpression(): Boolean =
            ORACLE_BOOLEAN_CONDITION_REGEXES.any { regex -> regex.containsMatchIn(this) }

        private fun String.oracleTypeNameAfterKeyword(keyword: String): String? {
            val match = oracleReturningTypeRegex(keyword).find(this)
            return match?.groupValues?.get(1)?.trim()
        }

        private fun oracleReturningTypeRegex(keyword: String): Regex =
            Regex(
                """(?i)\b${Regex.escape(keyword)}\s+""" +
                    """(DOUBLE\s+PRECISION|TIMESTAMP(?:\s*\([^)]*\))?(?:\s+WITH(?:\s+LOCAL)?\s+TIME\s+ZONE)?|""" +
                    """INTERVAL\s+(?:YEAR|DAY)\s+TO\s+(?:MONTH|SECOND)|""" +
                    """NATIONAL\s+CHARACTER\s+VARYING\s*\([^)]*\)|NATIONAL\s+CHAR\s+VARYING\s*\([^)]*\)|""" +
                    """CHARACTER\s+VARYING\s*\([^)]*\)|VARYING\s+ARRAY\s*(?:\([^)]*\))?|""" +
                    """[A-Z_]+(?:\s*\([^)]*\))?)""",
            )

        private fun IntermediateType.roundOrTruncSingleArgumentType(): IntermediateType? =
            when (dialectType) {
                in NUMERIC_TYPE_ORDER -> this
                in DATETIME_TYPE_ORDER -> IntermediateType(DATE).nullableIf(javaType.isNullable)
                else -> null
            }

        private fun IntermediateType.ceilOrFloorSingleArgumentType(): IntermediateType? =
            when (dialectType) {
                in NUMERIC_TYPE_ORDER -> this
                in DATETIME_TYPE_ORDER -> IntermediateType(DATE).nullableIf(javaType.isNullable)
                else -> null
            }

        private fun IntermediateType.singleNumericFunctionType(): IntermediateType? =
            when (dialectType) {
                BINARY_FLOAT -> IntermediateType(BINARY_DOUBLE).nullableIf(javaType.isNullable)
                in NUMERIC_TYPE_ORDER -> this
                else -> null
            }

        private fun List<IntermediateType>.binaryNumericFunctionType(): IntermediateType? =
            when {
                any { type -> type.dialectType == BINARY_FLOAT || type.dialectType == BINARY_DOUBLE } -> {
                    IntermediateType(BINARY_DOUBLE).nullableIf(any { type -> type.javaType.isNullable })
                }

                all { type -> type.dialectType in NUMERIC_TYPE_ORDER } -> {
                    IntermediateType(DECIMAL_NUMBER).nullableIf(any { type -> type.javaType.isNullable })
                }

                else -> {
                    null
                }
            }

        private fun List<IntermediateType>.roundOrTruncTwoArgumentType(): IntermediateType? =
            when {
                all { type -> type.dialectType in NUMERIC_TYPE_ORDER } -> {
                    IntermediateType(DECIMAL_NUMBER).nullableIf(any { type -> type.javaType.isNullable })
                }

                first().dialectType in DATETIME_TYPE_ORDER && this[1].dialectType in TEXT_TYPE_ORDER -> {
                    IntermediateType(DATE).nullableIf(any { type -> type.javaType.isNullable })
                }

                else -> {
                    null
                }
            }

        private val COMPARABLE_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                BOOLEAN,
                BOOLEAN_TYPE,
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TEXT,
                BLOB,
                BINARY,
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private val MAX_TYPE_ORDER = COMPARABLE_TYPE_ORDER

        private val NUMERIC_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
            )

        private val DATETIME_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                DATE,
                TIMESTAMP,
                TIMESTAMP_TIME_ZONE,
            )

        private val TEXT_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                TEXT,
                OracleType.TEXT,
            )

        private val ORACLE_BOOLEAN_CONDITION_REGEXES: List<Regex> =
            listOf(
                Regex("""(?i)^\s*NOT\b"""),
                Regex("""(?i)\bIS\s+(?:NOT\s+)?(?:TRUE|FALSE|UNKNOWN)\b"""),
                Regex("""(?i)\bIS\s+(?:NOT\s+)?JSON\b"""),
                Regex("""(?i)\bIS\s+(?:NOT\s+)?(?:NAN|INFINITE)\b"""),
                Regex("""(?i)\bIS\s+(?:NOT\s+)?(?:OF\s+(?:TYPE\s+)?\(|DANGLING\b)"""),
                Regex("""(?i)\bIS\s+(?:ANY|PRESENT)\b"""),
                Regex("""(?i)\b(?:LIKEC|LIKE2|LIKE4)\b"""),
                Regex("""(?i)^\s*XMLEXISTS\s*\("""),
                Regex("""(?i)\bIS\s+(?:NOT\s+)?(?:A\s+SET|EMPTY)\b"""),
                Regex("""(?i)\b(?:NOT\s+)?MEMBER(?:\s+OF)?\b"""),
                Regex("""(?i)\b(?:NOT\s+)?SUBMULTISET(?:\s+OF)?\b"""),
                Regex("""\^="""),
            )

        private val MIN_TYPE_ORDER: Array<DialectType> =
            arrayOf(
                BLOB,
                BINARY,
                TEXT,
                BOOLEAN,
                BOOLEAN_TYPE,
                INTEGER,
                INTEGER_NUMBER,
                LONG_NUMBER,
                DECIMAL_NUMBER,
                REAL,
                BINARY_FLOAT,
                BINARY_DOUBLE,
                TIMESTAMP_TIME_ZONE,
                TIMESTAMP,
                DATE,
            )
    }

    private data class OracleCaseReturnExpression(
        val expression: SqlExpr,
        val isElse: Boolean,
    )
}
