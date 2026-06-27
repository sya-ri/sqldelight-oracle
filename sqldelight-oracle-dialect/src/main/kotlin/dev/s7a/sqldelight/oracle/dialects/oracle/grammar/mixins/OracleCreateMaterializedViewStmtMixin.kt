package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
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
                baseQuery.withOracleSynthesizedColumns(viewName, columnNames)
            }
        }

    private fun oracleMaterializedViewColumnNames(): List<String> =
        text.oracleCreateViewColumnNames(
            viewNameText = viewName.text,
            skipObjectViewSyntax = false,
            skipConstraintColumns = false,
        )
}
