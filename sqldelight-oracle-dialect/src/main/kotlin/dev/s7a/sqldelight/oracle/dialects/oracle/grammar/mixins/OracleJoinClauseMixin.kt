package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlJoinConstraint
import com.alecstrong.sql.psi.core.psi.SqlJoinOperator
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlJoinClauseImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiNamedElement

/**
 * Oracle rewrites the `join_operator` grammar so that the join type keywords
 * (`LEFT`, `RIGHT`, `FULL`, `OUTER`, `INNER`, `CROSS`) are emitted as plain tokens
 * instead of the composite `left_join_operator` / `right_join_operator` /
 * `full_join_operator` nodes that sql-psi's [SqlJoinClauseImpl] looks for.
 *
 * As a result the core outer-join detection never fires, and columns coming from
 * the nullable side of a `LEFT`/`RIGHT`/`FULL` join keep their declared
 * nullability. This mixin reimplements the core `queryExposed()` algorithm while
 * detecting outer joins from the Oracle keyword tokens, so that outer-join
 * columns become nullable as they should.
 */
internal abstract class OracleJoinClauseMixin(
    node: ASTNode,
) : SqlJoinClauseImpl(node) {
    private val oracleQueryExposed =
        ModifiableFileLazy {
            var queryAvailable: Collection<QueryResult> = tableOrSubqueryList.first().queryExposed()

            for ((index, subquery) in tableOrSubqueryList.zipWithNext().withIndex()) {
                val constraint: SqlJoinConstraint? = joinConstraintList.getOrNull(index)
                val operator: SqlJoinOperator = joinOperatorList[index]

                val query = subquery.second.queryExposed()
                if (query.isEmpty()) continue

                var columns = query.flatMap { it.columns }
                var synthesizedColumns = query.flatMap { it.synthesizedColumns }

                if (operator.oracleRightJoinOperator()) {
                    val rightQuery = subquery.first.queryExposed()
                    val rightColumns = rightQuery.flatMap { it.columns }.map { it.copy(nullable = true) }
                    val rightSynthesizedColumns =
                        rightQuery.flatMap { it.synthesizedColumns }.map { it.copy(nullable = true) }

                    queryAvailable = queryAvailable - rightQuery.toSet()
                    queryAvailable =
                        queryAvailable +
                        QueryResult(
                            table = rightQuery.first().table,
                            columns = rightColumns,
                            synthesizedColumns = rightSynthesizedColumns,
                            joinConstraint = joinConstraintList[index],
                        )
                }

                if (operator.oracleLeftJoinOperator()) {
                    columns = columns.map { it.copy(nullable = true) }
                    synthesizedColumns = synthesizedColumns.map { it.copy(nullable = true) }
                }

                if (constraint != null && constraint.oracleUsingConstraint()) {
                    val columnNames = constraint.columnNameList.map { it.name }
                    columns =
                        columns.map { column ->
                            val element = column.element
                            column.copy(hiddenByUsing = element is PsiNamedElement && element.name in columnNames)
                        }
                }

                queryAvailable =
                    queryAvailable +
                    QueryResult(
                        table = query.first().table,
                        columns = columns,
                        synthesizedColumns = synthesizedColumns,
                        joinConstraint = constraint,
                    )
            }

            queryAvailable
        }

    override fun queryExposed(): Collection<QueryResult> = oracleQueryExposed.forFile(containingFile)

    private fun SqlJoinOperator.oracleJoinKeywords(): Set<String> =
        node
            .getChildren(null)
            .filter { it.firstChildNode == null }
            .map { it.text.uppercase() }
            .toSet()

    private fun SqlJoinOperator.oracleLeftJoinOperator(): Boolean {
        val keywords = oracleJoinKeywords()
        return "LEFT" in keywords ||
            "FULL" in keywords ||
            ("APPLY" in keywords && "OUTER" in keywords)
    }

    private fun SqlJoinOperator.oracleRightJoinOperator(): Boolean {
        val keywords = oracleJoinKeywords()
        return "RIGHT" in keywords || "FULL" in keywords
    }

    private fun SqlJoinConstraint.oracleUsingConstraint(): Boolean = node?.findChildByType(SqlTypes.USING) != null
}
