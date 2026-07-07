package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTableOrSubquery
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.util.PsiTreeUtil
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.OracleParser
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.OracleTableAlias
import javax.swing.Icon

internal abstract class OracleTableAliasMixin(
    node: ASTNode,
) : SqlNamedElementImpl(node),
    OracleTableAlias,
    SqlTableAlias {
    override val parseRule: (PsiBuilder, Int) -> Boolean = OracleParser::table_alias_real

    override fun source(): PsiElement =
        (parent as? SqlTableOrSubquery)?.let { tableOrSubquery ->
            tableOrSubquery.tableName
                ?: tableOrSubquery.compoundSelectStmt
                ?: oracleCollectionTableSource(tableOrSubquery)
                ?: tableOrSubquery
        } ?: oracleCollectionTableSource() ?: oracleTableFunctionSource() ?: parent.parent

    override fun getIcon(flags: Int): Icon = AllIcons.Nodes.DataTables

    override fun getId(): PsiElement? = findChildByType(SqlTypes.ID)

    override fun getString(): PsiElement? = findChildByType(SqlTypes.STRING)

    override fun getText(): String = node.text

    override fun getName(): String = text

    override fun getNameIdentifier(): PsiElement? = firstChild

    private fun oracleCollectionTableSource(tableReference: PsiElement? = parent?.parent): PsiElement? {
        tableReference ?: return null
        val body = tableReference.text.trimStart()
        if (!body.startsWith("TABLE", ignoreCase = true) && !body.startsWith("THE", ignoreCase = true)) return null
        return OracleSyntheticTableNameElement(this)
    }

    private fun oracleTableFunctionSource(): PsiElement? {
        val sourceAliasName =
            Regex("""(?is)\b(?:JSON_TABLE|XMLTABLE)\s*\(\s*([A-Za-z_][A-Za-z0-9_$#]*)\.""")
                .find(parent.text)
                ?.groupValues
                ?.get(1)
        val aliases = PsiTreeUtil.findChildrenOfType(containingFile, SqlTableAlias::class.java)
        if (sourceAliasName != null) {
            aliases.firstOrNull { alias -> alias.name.equals(sourceAliasName, ignoreCase = true) }?.let { return it }
        }
        aliases.firstOrNull { alias -> alias !== this }?.let { return it }
        return PsiTreeUtil.findChildrenOfType(containingFile, SqlTableName::class.java).firstOrNull()
    }
}

private class OracleSyntheticTableNameElement(
    private val alias: SqlTableAlias,
) : LightElement(alias.manager, alias.language),
    SqlTableName {
    override fun annotate(annotationHolder: SqlAnnotationHolder) = Unit

    override fun queryAvailable(child: PsiElement): Collection<QueryResult> = emptyList()

    override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> = emptyList()

    override fun getId(): PsiElement? = null

    override fun getString(): PsiElement? = null

    override fun getName(): String = alias.name

    override fun setName(name: String): PsiElement = this

    override fun getText(): String = alias.name

    override fun getContainingFile(): SqlFileBase = alias.containingFile

    override fun getParent(): PsiElement = alias.parent

    override fun getNameIdentifier(): PsiElement? = alias.nameIdentifier

    override fun toString(): String = "Oracle synthetic table function source: $name"
}
