package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import com.alecstrong.sql.psi.core.psi.impl.SqlResultColumnImpl
import com.intellij.lang.ASTNode

internal abstract class OracleResultColumnMixin(
    node: ASTNode,
) : SqlResultColumnImpl(node)
