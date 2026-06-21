package dev.s7a.sqldelight.oracle.dialect

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName

public class OracleTypeResolver(
    private val parentResolver: TypeResolver,
) : TypeResolver by parentResolver {
    override fun definitionType(typeName: SqlTypeName): IntermediateType = IntermediateType(OracleType.fromSqlTypeName(typeName.text))

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        OracleType.fromFunctionName(functionExpr.functionName.text)?.let { type -> IntermediateType(type) }
            ?: parentResolver.functionType(functionExpr)
}
