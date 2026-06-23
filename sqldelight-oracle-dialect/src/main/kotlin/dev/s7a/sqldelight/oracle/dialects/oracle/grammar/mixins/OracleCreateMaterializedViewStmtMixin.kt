package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateViewStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

internal abstract class OracleCreateMaterializedViewStmtMixin : SqlCreateViewStmtImpl {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: SchemaContributorStub,
        stubType: IStubElementType<*, *>,
    ) : super(stub, stubType)

    override fun tableExposed(): LazyQuery =
        LazyQuery(viewName) {
            val baseQuery = super.tableExposed().query
            val columnNames = oracleMaterializedViewColumnNames()
            if (columnNames.isEmpty()) {
                baseQuery
            } else {
                QueryResult(
                    table = viewName,
                    columns = baseQuery.columns,
                    synthesizedColumns =
                        columnNames.map { columnName ->
                            SynthesizedColumn(viewName, listOf(columnName))
                        },
                )
            }
        }

    private fun oracleMaterializedViewColumnNames(): List<String> {
        val beforeQuery = text.substringBeforeOracleKeyword("AS") ?: return emptyList()
        val afterViewName = beforeQuery.substringAfter(viewName.text, missingDelimiterValue = "")
        if (afterViewName.isBlank()) return emptyList()

        val openIndex = afterViewName.indexOf('(')
        if (openIndex == -1) return emptyList()

        return afterViewName
            .oracleParenthesizedBodyAt(openIndex)
            .split(',')
            .mapNotNull { column -> ORACLE_IDENTIFIER_REGEX.find(column.trim())?.value?.trim('"') }
    }
}

private fun String.substringBeforeOracleKeyword(keyword: String): String? {
    val match = Regex("""\b$keyword\b""", RegexOption.IGNORE_CASE).find(this) ?: return null
    return substring(0, match.range.first)
}

private fun String.oracleParenthesizedBodyAt(openOffset: Int): String {
    if (openOffset !in indices || this[openOffset] != '(') return ""
    var depth = 0
    for (index in openOffset until length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
                if (depth == 0) return substring(openOffset + 1, index)
            }
        }
    }
    return ""
}

private val ORACLE_IDENTIFIER_REGEX = Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
