package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.mixins

import app.cash.sqldelight.dialect.grammar.mixins.BindParameterMixin
import com.intellij.lang.ASTNode

/**
 * Treats Oracle bind parameters (`?` / `:name`) as a SQLDelight [BindParameterMixin].
 *
 * SQLDelight rewrites the user-provided parameter to the positional `?` in the generated SQL.
 * Oracle JDBC (ojdbc) also uses positional `?`, so [replaceWith] keeps the base default (`?`).
 */
public abstract class OracleBindParameterMixin(
    node: ASTNode,
) : BindParameterMixin(node)
