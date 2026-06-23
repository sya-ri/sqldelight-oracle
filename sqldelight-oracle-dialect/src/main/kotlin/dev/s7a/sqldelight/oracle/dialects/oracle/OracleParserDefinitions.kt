package dev.s7a.sqldelight.oracle.dialects.oracle

import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.ILightStubFileElementType

internal class OracleSqlDelightParserDefinition : SqlParserDefinition() {
    private val parserInitializer = SqlDelightParserInitializer()

    override fun createLexer(project: Project): Lexer = OracleCommentLexer()

    override fun createFile(viewProvider: FileViewProvider): SqlDelightQueriesFile = SqlDelightQueriesFile(viewProvider)

    override fun getFileNodeType(): ILightStubFileElementType<PsiFileStub<SqlFileBase>> = FILE

    override fun getLanguage(): SqlDelightLanguage = SqlDelightLanguage

    override fun createParser(project: Project): SqlParser {
        parserInitializer.initializeDialect(project)
        return super.createParser(project)
    }

    private companion object {
        private val FILE =
            object : ILightStubFileElementType<PsiFileStub<SqlFileBase>>(SqlDelightLanguage) {
                override fun getExternalId(): String = "SqlDelight"
            }
    }
}

internal class OracleMigrationParserDefinition : SqlParserDefinition() {
    private val parserInitializer = SqlDelightParserInitializer()

    override fun createLexer(project: Project): Lexer = OracleCommentLexer()

    override fun createFile(viewProvider: FileViewProvider): MigrationFile = MigrationFile(viewProvider)

    override fun getFileNodeType(): ILightStubFileElementType<PsiFileStub<SqlFileBase>> = FILE

    override fun getLanguage(): SqlDelightLanguage = SqlDelightLanguage

    override fun createParser(project: Project): SqlParser {
        parserInitializer.initializeDialect(project)
        return super.createParser(project)
    }

    private companion object {
        private val FILE =
            object : ILightStubFileElementType<PsiFileStub<SqlFileBase>>(MigrationLanguage) {
                override fun getExternalId(): String = "SqlDelight.MIGRATION"
            }
    }
}

private class SqlDelightParserInitializer {
    private val parserUtil: Any =
        Class
            .forName("app.cash.sqldelight.core.lang.ParserUtil")
            .getDeclaredConstructor()
            .newInstance()

    private val initializeDialect =
        parserUtil.javaClass.getDeclaredMethod("initializeDialect", Project::class.java)

    fun initializeDialect(project: Project) {
        initializeDialect.invoke(parserUtil, project)
    }
}
