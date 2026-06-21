package dev.s7a.sqldelight.oracle.dialect.grammar

import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser

/**
 * Parser hooks installed by [dev.s7a.sqldelight.oracle.dialect.OracleDialect.setup].
 */
internal object OracleParserUtil : GeneratedParserUtilBase() {
    internal var typeName: Parser? = null

    internal fun reset() {
        typeName = null
    }

    internal fun overrideSqlParser() {
        SqlParserUtil.type_name =
            Parser { builder, level ->
                typeName?.parse(builder, level) ?: oracleTypeName(builder, level)
            }
    }

    @JvmStatic
    internal fun oracleTypeName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "oracle_type_name")) return false

        val marker = enter_section_(builder, level, 1, SqlTypes.TYPE_NAME, "<oracle type name>")
        val startOffset = builder.currentOffset
        val matched = consumeOracleTypeName(builder)
        if (!matched && builder.currentOffset != startOffset) {
            error("Oracle type parser advanced without matching a type")
        }
        exit_section_(builder, level, marker, matched, false, null)
        return matched
    }

    private fun consumeOracleTypeName(builder: PsiBuilder): Boolean {
        val typeName = oracleTypeNames.firstOrNull { candidate -> builder.matchesWords(candidate.words) } ?: return false
        typeName.words.forEach { _ ->
            builder.advanceLexer()
            builder.consumeOptionalParenthesizedParameters()
        }
        typeName.parameterPolicy.consume(builder)
        return true
    }

    private fun PsiBuilder.matchesWords(words: List<String>): Boolean {
        val marker = mark()
        val matched =
            words.all { word ->
                when {
                    tokenText.equals(word, ignoreCase = true) -> {
                        advanceLexer()
                        consumeOptionalParenthesizedParameters()
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        marker.rollbackTo()
        return matched
    }

    private fun ParameterPolicy.consume(builder: PsiBuilder) {
        when (this) {
            ParameterPolicy.NONE -> Unit
            ParameterPolicy.OPTIONAL -> builder.consumeOptionalParenthesizedParameters()
        }
    }

    private fun PsiBuilder.consumeOptionalParenthesizedParameters() {
        if (tokenType != SqlTypes.LP) return

        var depth = 0
        do {
            when (tokenType) {
                SqlTypes.LP -> depth += 1
                SqlTypes.RP -> depth -= 1
            }
            advanceLexer()
        } while (!eof() && depth > 0)
    }

    private val oracleTypeNames: List<OracleTypeName> =
        listOf(
            "TIMESTAMP WITH LOCAL TIME ZONE",
            "TIMESTAMP WITH TIME ZONE",
            "INTERVAL YEAR TO MONTH",
            "INTERVAL DAY TO SECOND",
            "NATIONAL CHARACTER VARYING",
            "NATIONAL CHAR VARYING",
            "DOUBLE PRECISION",
            "LONG RAW",
            "CHARACTER VARYING",
            "NESTED TABLE",
            "VARYING ARRAY",
            "BINARY_DOUBLE",
            "BINARY_FLOAT",
            "BINARY_INTEGER",
            "PLS_INTEGER",
            "NVARCHAR2",
            "VARCHAR2",
            "CHARACTER",
            "NATIONAL CHARACTER",
            "NATIONAL CHAR",
            "TIMESTAMP",
            "INTERVAL",
            "BOOLEAN",
            "NUMBER",
            "NUMERIC",
            "DECIMAL",
            "DEC",
            "INTEGER",
            "INT",
            "SMALLINT",
            "FLOAT",
            "REAL",
            "DATE",
            "CHAR",
            "NCHAR",
            "VARCHAR",
            "CLOB",
            "NCLOB",
            "LONG",
            "BLOB",
            "BFILE",
            "RAW",
            "ROWID",
            "UROWID",
            "JSON",
            "XMLTYPE",
            "SDO_GEOMETRY",
            "SDO_TOPO_GEOMETRY",
            "SDO_GEORASTER",
            "VECTOR",
            "OBJECT",
            "REF",
            "ANYDATA",
            "ANYTYPE",
            "ANYDATASET",
            "URITYPE",
            "DBURITYPE",
            "XDBURITYPE",
            "HTTPURITYPE",
            "VARRAY",
        ).map { name -> OracleTypeName(name.split(' '), ParameterPolicy.OPTIONAL) }
            .sortedByDescending { typeName -> typeName.words.size }
}

private data class OracleTypeName(
    val words: List<String>,
    val parameterPolicy: ParameterPolicy,
)

private enum class ParameterPolicy {
    NONE,
    OPTIONAL,
}
