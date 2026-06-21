package dev.s7a.sqldelight.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.mixins.AlterTableElementType
import com.alecstrong.sql.psi.core.psi.mixins.AlterTableStmtStub
import dev.s7a.sqldelight.oracle.grammar.psi.impl.OracleAlterTableStmtImpl

internal class AlterTableElementType(
    name: String,
) : AlterTableElementType("oracle.$name") {
    override fun createPsi(stub: SchemaContributorStub) =
        OracleAlterTableStmtImpl(
            stub as AlterTableStmtStub,
            this,
        )
}
