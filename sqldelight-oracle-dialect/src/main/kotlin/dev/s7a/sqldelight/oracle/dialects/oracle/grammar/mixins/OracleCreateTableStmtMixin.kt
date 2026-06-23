package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateTableStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

internal abstract class OracleCreateTableStmtMixin : SqlCreateTableStmtImpl {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: SchemaContributorStub,
        stubType: IStubElementType<*, *>,
    ) : super(stub, stubType)

    override fun queryAvailable(child: PsiElement): List<QueryResult> =
        super.queryAvailable(child).map { result ->
            result.copy(
                synthesizedColumns =
                    result.synthesizedColumns +
                        oracleCreateTableExpressionColumns().map { name -> SynthesizedColumn(this, listOf(name)) },
            )
        }

    private fun oracleCreateTableExpressionColumns(): List<String> =
        oracleJsonCollectionExpressionColumns() + oracleXmltypeVirtualColumns() + oracleObjectTableAttributeColumns()

    private fun oracleJsonCollectionExpressionColumns(): List<String> {
        if (!text.contains("JSON COLLECTION TABLE", ignoreCase = true)) return emptyList()
        val body = text.oracleParenthesizedBodyAfter("TABLE") ?: return emptyList()
        return body
            .oracleTopLevelCommaParts()
            .filter { column -> column.indexOfKeyword("AS") != null }
            .mapNotNull { column -> column.oracleFirstName() }
    }

    private fun oracleXmltypeVirtualColumns(): List<String> {
        if (!text.contains("OF XMLTYPE", ignoreCase = true)) return emptyList()
        val body = text.oracleParenthesizedBodyAfter("VIRTUAL COLUMNS") ?: return emptyList()
        return body
            .oracleTopLevelCommaParts()
            .mapNotNull { column -> column.oracleFirstName() }
    }

    private fun oracleObjectTableAttributeColumns(): List<String> {
        val typeName = oracleObjectTableTypeName() ?: return emptyList()
        val typeBody = containingFile.text.oracleObjectTypeBody(typeName) ?: return emptyList()
        return typeBody
            .oracleTopLevelCommaParts()
            .filterNot { attribute -> attribute.oracleFirstName()?.isOracleObjectTypeMemberKeyword() == true }
            .mapNotNull { attribute -> attribute.oracleFirstName() }
    }

    private fun oracleObjectTableTypeName(): String? {
        if (text.contains("OF XMLTYPE", ignoreCase = true)) return null
        return Regex("""(?is)\bOF\s+((?:"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)(?:\s*\.\s*(?:"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*))?)""")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.substringAfterLast(".")
            ?.trimOracleIdentifier()
    }
}

private fun String.oracleParenthesizedBodyAfter(keyword: String): String? {
    val keywordOffset = indexOfKeyword(keyword) ?: return null
    val openOffset = indexOf('(', startIndex = keywordOffset + keyword.length).takeIf { it != -1 } ?: return null
    return oracleParenthesizedBodyAt(openOffset)
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
    var start = 0
    var depth = 0
    var index = 0
    var inString = false
    while (index < length) {
        when {
            inString && this[index] == '\'' && index + 1 < length && this[index + 1] == '\'' -> {
                index++
            }

            this[index] == '\'' -> {
                inString = !inString
            }

            !inString && this[index] == '(' -> {
                depth++
            }

            !inString && this[index] == ')' -> {
                depth--
            }

            !inString && depth == 0 && this[index] == ',' -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
        index++
    }
    parts += substring(start)
    return parts
}

private fun String.indexOfKeyword(
    keyword: String,
    startIndex: Int = 0,
): Int? {
    var index = startIndex
    while (index < length) {
        index = indexOf(keyword, startIndex = index, ignoreCase = true)
        if (index == -1) return null
        if (isOracleIdentifierBoundary(index - 1) && isOracleIdentifierBoundary(index + keyword.length)) return index
        index += keyword.length
    }
    return null
}

private fun String.oracleFirstName(): String? =
    Regex(""""[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*""")
        .find(this)
        ?.value
        ?.trimOracleIdentifier()

private fun String.oracleObjectTypeBody(typeName: String): String? {
    val objectName = """"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*"""
    val qualifiedObjectName = """($objectName(?:\s*\.\s*$objectName)?)"""
    val match =
        Regex(
            """(?is)\bCREATE\s+(?:OR\s+REPLACE\s+)?(?:EDITIONABLE\s+|NONEDITIONABLE\s+)?TYPE\s+""" +
                """(?:IF\s+NOT\s+EXISTS\s+)?$qualifiedObjectName\s+AS\s+OBJECT\s*""",
        ).findAll(this)
            .firstOrNull { createType ->
                createType.groupValues[1]
                    .substringAfterLast(".")
                    .trimOracleIdentifier()
                    .equals(typeName, ignoreCase = true)
            } ?: return null
    val openOffset = indexOf('(', startIndex = match.range.last + 1)
    return oracleParenthesizedBodyAt(openOffset)
}

private fun String.trimOracleIdentifier(): String = trim().removeSurrounding("\"")

private fun String.isOracleObjectTypeMemberKeyword(): Boolean =
    equals("CONSTRUCTOR", ignoreCase = true) ||
        equals("FINAL", ignoreCase = true) ||
        equals("MAP", ignoreCase = true) ||
        equals("MEMBER", ignoreCase = true) ||
        equals("NOT", ignoreCase = true) ||
        equals("ORDER", ignoreCase = true) ||
        equals("OVERRIDING", ignoreCase = true) ||
        equals("STATIC", ignoreCase = true)

private fun String.isOracleIdentifierBoundary(index: Int): Boolean =
    index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_' && this[index] != '$' && this[index] != '#')
