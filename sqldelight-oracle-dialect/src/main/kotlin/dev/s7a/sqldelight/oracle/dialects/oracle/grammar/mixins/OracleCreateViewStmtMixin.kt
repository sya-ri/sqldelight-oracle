package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateViewStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

internal abstract class OracleCreateViewStmtMixin : SqlCreateViewStmtImpl {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: SchemaContributorStub,
        stubType: IStubElementType<*, *>,
    ) : super(stub, stubType)

    override fun tableExposed(): LazyQuery =
        LazyQuery(viewName) {
            val baseQuery = super.tableExposed().query
            val oracleViewColumns = oracleViewColumnNames()
            if (oracleViewColumns.isEmpty()) {
                baseQuery
            } else {
                QueryResult(
                    table = viewName,
                    columns = baseQuery.columns,
                    synthesizedColumns =
                        oracleViewColumns.map { columnName ->
                            SynthesizedColumn(viewName, listOf(columnName))
                        },
                )
            }
        }

    private fun oracleViewColumnNames(): List<String> {
        val beforeQuery = text.substringBeforeOracleKeyword("AS") ?: return emptyList()
        val afterViewName = beforeQuery.substringAfter(viewName.text, missingDelimiterValue = "")
        if (afterViewName.isBlank()) return emptyList()

        val openIndex = afterViewName.indexOf('(')
        if (openIndex == -1) return emptyList()

        val prefix = afterViewName.substring(0, openIndex)
        if (prefix.contains(Regex("""\b(AS|OF)\b|\bWITH\s+OBJECT\b""", RegexOption.IGNORE_CASE))) {
            return emptyList()
        }

        return afterViewName
            .oracleParenthesizedBodyAt(openIndex)
            .oracleTopLevelCommaParts()
            .mapNotNull { part -> part.oracleViewColumnName() }
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

private fun String.oracleTopLevelCommaParts(): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var start = 0
    forEachIndexed { index, char ->
        when (char) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
            }

            ',' -> {
                if (depth == 0) {
                    parts += substring(start, index)
                    start = index + 1
                }
            }
        }
    }
    parts += substring(start)
    return parts
}

private fun String.oracleViewColumnName(): String? {
    val trimmed = trim()
    if (trimmed.startsWith("CONSTRAINT", ignoreCase = true)) return null
    return ORACLE_IDENTIFIER_REGEX.find(trimmed)?.value?.trim('"')
}

private val ORACLE_IDENTIFIER_REGEX = Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
