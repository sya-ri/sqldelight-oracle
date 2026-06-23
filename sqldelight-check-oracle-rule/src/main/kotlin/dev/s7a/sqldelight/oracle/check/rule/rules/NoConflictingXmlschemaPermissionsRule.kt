package dev.s7a.sqldelight.oracle.check.rule.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.oracle.check.dialect.OracleDialectId

/**
 * Reports conflicting Oracle XMLSchema permission clauses.
 */
public class NoConflictingXmlschemaPermissionsRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-xmlschema-permissions")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskXmlschemaCommentsAndQuotedTextPreservingOffsets()
            .xmlschemaStatements()
            .flatMap { statement -> statement.conflictingXmlschemaPermissions() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle XMLSchema permission clauses.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class XmlschemaToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class XmlschemaPermission(
    val startOffset: Int,
    val endOffset: Int,
)

private data class XmlschemaPermissionConflict(
    val first: XmlschemaPermission,
    val second: XmlschemaPermission,
)

private fun String.xmlschemaStatements(): List<List<XmlschemaToken>> {
    val statements = mutableListOf<List<XmlschemaToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).xmlschemaTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<XmlschemaToken>.conflictingXmlschemaPermissions(): List<XmlschemaPermissionConflict> {
    if (none { token -> token.xmlschemaHasText("XMLSCHEMA") }) return emptyList()
    val permissions = xmlschemaPermissions()
    val first = permissions.firstOrNull() ?: return emptyList()
    return permissions.drop(1).map { permission ->
        XmlschemaPermissionConflict(
            first = first,
            second = permission,
        )
    }
}

private fun List<XmlschemaToken>.xmlschemaPermissions(): List<XmlschemaPermission> =
    mapIndexedNotNull { index, token ->
        val next = getOrNull(index + 1)
        if ((token.xmlschemaHasText("ALLOW") || token.xmlschemaHasText("DISALLOW")) &&
            (next.xmlschemaHasText("NONSCHEMA") || next.xmlschemaHasText("ANYSCHEMA"))
        ) {
            XmlschemaPermission(
                startOffset = token.startOffset,
                endOffset = next?.endOffset ?: token.endOffset,
            )
        } else {
            null
        }
    }

private val xmlschemaTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.xmlschemaTokens(offset: Int): List<XmlschemaToken> =
    xmlschemaTokenPattern
        .findAll(this)
        .map { match ->
            XmlschemaToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskXmlschemaCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> xmlschemaMaskRange(chars, index, skipXmlschemaLineComment(index))
                startsWith("/*", index) -> xmlschemaMaskRange(chars, index, skipXmlschemaBlockComment(index))
                chars[index] == '\'' -> xmlschemaMaskRange(chars, index, skipXmlschemaQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipXmlschemaLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipXmlschemaBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipXmlschemaQuotedString(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == '\'') {
            if (index + 1 < length && this[index + 1] == '\'') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun xmlschemaMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun XmlschemaToken?.xmlschemaHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
