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
 * Reports conflicting Oracle annotation operations for the same annotation name.
 */
public class NoConflictingAnnotationOperationsRule : Rule {
    override val id: RuleId = RuleId("no-conflicting-annotation-operations")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .maskAnnotationCommentsAndQuotedTextPreservingOffsets()
            .annotationStatements()
            .flatMap { statement -> statement.conflictingAnnotationOperations() }
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid conflicting Oracle annotation operations for ${conflict.annotationName}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class AnnotationToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class AnnotationOperation(
    val annotationName: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class AnnotationConflict(
    val annotationName: String,
    val first: AnnotationOperation,
    val second: AnnotationOperation,
)

private fun String.annotationStatements(): List<List<AnnotationToken>> {
    val statements = mutableListOf<List<AnnotationToken>>()
    var start = 0
    while (start < length) {
        val end = indexOf(';', startIndex = start).let { if (it == -1) length else it + 1 }
        statements += substring(start, end).annotationTokens(offset = start)
        start = end
    }
    return statements
}

private fun List<AnnotationToken>.conflictingAnnotationOperations(): List<AnnotationConflict> {
    if (none { token -> token.annotationHasText("ANNOTATIONS") }) return emptyList()
    val firstByAnnotation = linkedMapOf<String, AnnotationOperation>()
    return annotationOperations().mapNotNull { operation ->
        val first = firstByAnnotation.putIfAbsent(operation.annotationName.lowercase(), operation)
        first?.let {
            AnnotationConflict(
                annotationName = operation.annotationName,
                first = it,
                second = operation,
            )
        }
    }
}

private fun List<AnnotationToken>.annotationOperations(): List<AnnotationOperation> =
    mapIndexedNotNull { index, token ->
        if (!token.isAnnotationOperationKeyword()) return@mapIndexedNotNull null
        val nameToken = annotationNameTokenAfter(index) ?: return@mapIndexedNotNull null
        AnnotationOperation(
            annotationName = nameToken.text,
            startOffset = token.startOffset,
            endOffset = nameToken.endOffset,
        )
    }

private fun List<AnnotationToken>.annotationNameTokenAfter(operationIndex: Int): AnnotationToken? {
    var index = operationIndex + 1
    if (getOrNull(index).annotationHasText("IF")) {
        index++
        if (getOrNull(index).annotationHasText("NOT")) {
            index++
        }
        if (getOrNull(index).annotationHasText("EXISTS")) {
            index++
        }
    }
    return getOrNull(index)
}

private val annotationTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|;""")

private fun String.annotationTokens(offset: Int): List<AnnotationToken> =
    annotationTokenPattern
        .findAll(this)
        .map { match ->
            AnnotationToken(
                text = match.value,
                startOffset = offset + match.range.first,
                endOffset = offset + match.range.last + 1,
            )
        }.toList()

private fun String.maskAnnotationCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> annotationMaskRange(chars, index, skipAnnotationLineComment(index))
                startsWith("/*", index) -> annotationMaskRange(chars, index, skipAnnotationBlockComment(index))
                chars[index] == '\'' -> annotationMaskRange(chars, index, skipAnnotationQuotedString(index))
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.skipAnnotationLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipAnnotationBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipAnnotationQuotedString(start: Int): Int {
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

private fun annotationMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun AnnotationToken?.annotationHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private fun AnnotationToken.isAnnotationOperationKeyword(): Boolean =
    annotationHasText("ADD") || annotationHasText("DROP") || annotationHasText("REPLACE")
