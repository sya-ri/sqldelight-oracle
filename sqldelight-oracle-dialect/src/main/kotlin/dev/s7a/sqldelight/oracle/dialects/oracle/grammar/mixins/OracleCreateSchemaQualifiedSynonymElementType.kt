package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.SqlSchemaContributorElementType
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.TableElement
import com.intellij.psi.tree.IElementType
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.impl.OracleCreateSchemaQualifiedSynonymStmtImpl

internal class OracleCreateSchemaQualifiedSynonymElementType(
    name: String,
) : SqlSchemaContributorElementType<TableElement>("oracle.$name", TableElement::class.java) {
    override fun nameType(): IElementType = SqlTypes.TABLE_NAME

    override fun createPsi(stub: SchemaContributorStub): OracleCreateSchemaQualifiedSynonymStmtImpl =
        OracleCreateSchemaQualifiedSynonymStmtImpl(stub, this)
}
