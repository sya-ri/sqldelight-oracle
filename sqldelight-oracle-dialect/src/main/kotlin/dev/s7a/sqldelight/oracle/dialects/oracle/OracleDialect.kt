package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.dialect.api.RuntimeTypes
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.application.ApplicationManager
import com.squareup.kotlinpoet.ClassName
import dev.s7a.sqldelight.oracle.dialects.oracle.grammar.OracleParserUtil
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * SQLDelight dialect entry point for Oracle Database.
 */
public class OracleDialect : SqlDelightDialect {
    override val runtimeTypes: RuntimeTypes =
        RuntimeTypes(
            ClassName("app.cash.sqldelight.driver.jdbc", "JdbcCursor"),
            ClassName("app.cash.sqldelight.driver.jdbc", "JdbcPreparedStatement"),
        )

    override val allowsReferenceCycles: Boolean = false

    override val icon: Icon = OracleIcon

    override val connectionManager: ConnectionManager? = null

    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = OracleTypeResolver(parentResolver)

    override fun setup() {
        installOracleParserDefinitions()
        OracleParserUtil.reset()
        OracleParserUtil.overrideSqlParser()
    }
}

private fun installOracleParserDefinitions() {
    if (ApplicationManager.getApplication() == null) return

    val parserDefinitions = LanguageParserDefinitions.INSTANCE
    parserDefinitions.replaceParserDefinition(
        SqlDelightLanguage,
        OracleSqlDelightParserDefinition(),
    )
    parserDefinitions.replaceParserDefinition(
        MigrationLanguage,
        OracleMigrationParserDefinition(),
    )
}

private fun LanguageParserDefinitions.replaceParserDefinition(
    language: Language,
    parserDefinition: ParserDefinition,
) {
    allForLanguage(language).forEach { existingParserDefinition ->
        removeExplicitExtension(language, existingParserDefinition)
    }
    clearCache(language)
    addExplicitExtension(
        language,
        parserDefinition,
    )
    clearCache(language)
}

private data object OracleIcon : Icon {
    override fun getIconHeight(): Int = 16

    override fun getIconWidth(): Int = 16

    override fun paintIcon(
        component: Component?,
        graphics: Graphics?,
        x: Int,
        y: Int,
    ) {
        // The compiler only requires an Icon instance; IDE-specific artwork is intentionally avoided.
    }
}
