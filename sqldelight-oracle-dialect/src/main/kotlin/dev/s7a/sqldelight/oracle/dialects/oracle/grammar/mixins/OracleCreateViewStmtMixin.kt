package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.LazyQuery
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
                baseQuery.withOracleSynthesizedColumns(viewName, oracleViewColumns)
            }
        }

    private fun oracleViewColumnNames(): List<String> =
        text.oracleCreateViewColumnNames(
            viewNameText = viewName.text,
            skipObjectViewSyntax = true,
            skipConstraintColumns = true,
        )
}
