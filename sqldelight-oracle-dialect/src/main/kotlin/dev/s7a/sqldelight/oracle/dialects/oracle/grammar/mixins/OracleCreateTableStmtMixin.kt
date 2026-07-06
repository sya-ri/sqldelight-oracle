package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlGeneratedClause
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateTableStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil

internal abstract class OracleCreateTableStmtMixin : SqlCreateTableStmtImpl {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: SchemaContributorStub,
        stubType: IStubElementType<*, *>,
    ) : super(stub, stubType)

    override fun tableExposed(): LazyQuery =
        LazyQuery(tableName) {
            val ctasColumnAliases = oracleCtasColumnAliases()
            if (ctasColumnAliases.isNotEmpty()) {
                QueryResult(
                    table = tableName,
                    columns = emptyList(),
                    synthesizedColumns = ctasColumnAliases.map { name -> SynthesizedColumn(this, listOf(name)) },
                )
            } else {
                super.tableExposed().query.withOracleGeneratedColumnExpressions()
            }
        }

    override fun queryAvailable(child: PsiElement): List<QueryResult> =
        super.queryAvailable(child).map { result ->
            val ctasColumnAliases = oracleCtasColumnAliases()
            if (ctasColumnAliases.isNotEmpty()) {
                return@map result.copy(
                    columns = emptyList(),
                    synthesizedColumns = ctasColumnAliases.map { name -> SynthesizedColumn(this, listOf(name)) },
                )
            }

            result
                .withOracleGeneratedColumnExpressions()
                .copy(
                    synthesizedColumns =
                        result.synthesizedColumns +
                            oracleCreateTableExpressionColumns().map { name -> SynthesizedColumn(this, listOf(name)) },
                )
        }

    private fun QueryResult.withOracleGeneratedColumnExpressions(): QueryResult {
        val generatedColumnsByName = oracleGeneratedColumnExpressionsByName()
        if (generatedColumnsByName.isEmpty()) return this

        return copy(
            columns =
                columns.map { column ->
                    val columnName = (column.element as? NamedElement)?.name ?: return@map column
                    val generatedColumn = generatedColumnsByName[columnName.lowercase()] ?: return@map column
                    QueryColumn(
                        element =
                            OracleGeneratedColumnExpressionElement(
                                columnDef = generatedColumn.columnDef,
                                expression = generatedColumn.expression,
                            ),
                        nullable = if (generatedColumn.columnDef.hasOracleNotNullConstraint()) false else null,
                    )
                },
        )
    }

    private fun oracleGeneratedColumnExpressionsByName(): Map<String, OracleGeneratedColumnExpression> =
        oracleColumnDefinitions()
            .mapNotNull { columnDef ->
                val generatedClause = PsiTreeUtil.findChildOfType(columnDef, SqlGeneratedClause::class.java) ?: return@mapNotNull null
                OracleGeneratedColumnExpression(
                    columnDef = columnDef,
                    expression = generatedClause.expr,
                )
            }.associateBy { generatedColumnExpression ->
                val columnName = generatedColumnExpression.columnDef.columnName.name
                columnName.lowercase()
            }

    private fun oracleCtasColumnAliases(): List<String> {
        val body = text.oracleCtasAliasListBody() ?: return emptyList()
        return body
            .oracleTopLevelCommaParts()
            .mapNotNull { column -> column.oracleFirstName() }
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

    private data class OracleGeneratedColumnExpression(
        val columnDef: SqlColumnDef,
        val expression: PsiElement,
    )

    private class OracleGeneratedColumnExpressionElement(
        private val columnDef: SqlColumnDef,
        private val expression: PsiElement,
    ) : LightElement(columnDef.manager, columnDef.language),
        AliasElement {
        override fun source(): PsiElement = expression

        override fun getName(): String = columnDef.columnName.name

        override fun setName(name: String): PsiElement = columnDef.columnName.setName(name)

        override fun getText(): String = columnDef.columnName.text

        override fun getContainingFile(): PsiFile = columnDef.containingFile

        override fun getParent(): PsiElement = columnDef

        override fun getNameIdentifier(): PsiElement = columnDef.columnName

        override fun toString(): String = "Oracle generated column expression: $name"
    }
}

private fun SqlColumnDef.hasOracleNotNullConstraint(): Boolean = Regex("""(?i)\bNOT\s+NULL\b""").containsMatchIn(text)

private fun String.oracleCtasAliasListBody(): String? {
    val tableMatch =
        Regex(
            """(?is)\bCREATE\s+(?:(?:GLOBAL|PRIVATE)\s+TEMPORARY\s+|TEMP\s+|TEMPORARY\s+|SHARDED\s+|DUPLICATED\s+|(?:IMMUTABLE\s+)?BLOCKCHAIN\s+|IMMUTABLE\s+)?TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*)(?:\s*\.\s*(?:"[^"]+"|[A-Za-z_][A-Za-z0-9_$#]*))?""",
        ).find(this) ?: return null
    var index = tableMatch.range.last + 1
    while (index < length && this[index].isWhitespace()) index++
    Regex("""(?is)\GSHARING\s*=\s*(?:METADATA|DATA|EXTENDED\s+DATA|NONE)\b""")
        .find(this, index)
        ?.let { sharingClause ->
            index = sharingClause.range.last + 1
            while (index < length && this[index].isWhitespace()) index++
        }
    if (getOrNull(index) != '(') return null
    val body = oracleParenthesizedBodyAt(index)
    val closeIndex = index + body.length + 1
    val asIndex = indexOfKeyword("AS", startIndex = closeIndex + 1) ?: return null
    if (substring(closeIndex + 1, asIndex).isNotBlank()) return null
    return body
}

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

private fun String.isOracleObjectTypeMemberKeyword(): Boolean =
    equals("CONSTRUCTOR", ignoreCase = true) ||
        equals("FINAL", ignoreCase = true) ||
        equals("MAP", ignoreCase = true) ||
        equals("MEMBER", ignoreCase = true) ||
        equals("NOT", ignoreCase = true) ||
        equals("ORDER", ignoreCase = true) ||
        equals("OVERRIDING", ignoreCase = true) ||
        equals("STATIC", ignoreCase = true)
