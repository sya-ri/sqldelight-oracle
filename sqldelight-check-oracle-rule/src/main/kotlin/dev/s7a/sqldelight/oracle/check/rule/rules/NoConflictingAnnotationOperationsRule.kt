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
            .maskSqlCommentsAndQuotedTextPreservingOffsets(maskDoubleQuotedIdentifiers = false)
            .annotationClauses(originalContent = content)
            .flatMap { clause -> clause.conflictingAnnotationOperations() }
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

private fun String.annotationClauses(originalContent: String): List<List<AnnotationToken>> {
    val clauses = mutableListOf<List<AnnotationToken>>()
    var searchOffset = 0
    while (searchOffset < length) {
        val annotationsOffset = indexOfAnnotationKeyword(searchOffset)
        if (annotationsOffset == -1) break

        val listStart = skipAnnotationWhitespace(annotationsOffset + ANNOTATIONS.length)
        if (getOrNull(listStart) == '(') {
            val listEnd = matchingSqlParenthesis(listStart)
            if (listEnd != null) {
                clauses += originalContent.substring(listStart + 1, listEnd).annotationTokens(offset = listStart + 1)
                searchOffset = listEnd + 1
                continue
            }
        }

        searchOffset = annotationsOffset + ANNOTATIONS.length
    }
    return clauses
}

private fun List<AnnotationToken>.conflictingAnnotationOperations(): List<AnnotationConflict> {
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
            annotationName = nameToken.text.unquotedAnnotationName(),
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

private val annotationTokenPattern = Regex("'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"|[A-Za-z_][A-Za-z0-9_$#]*|;")

private const val ANNOTATIONS = "ANNOTATIONS"

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

private fun AnnotationToken?.annotationHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)

private fun AnnotationToken.isAnnotationOperationKeyword(): Boolean =
    annotationHasText("ADD") || annotationHasText("DROP") || annotationHasText("REPLACE")

private fun String.unquotedAnnotationName(): String =
    when {
        startsWith("\"") -> removeSurrounding("\"").replace("\"\"", "\"")
        startsWith("'") -> removeSurrounding("'").replace("''", "'")
        else -> this
    }

private fun String.indexOfAnnotationKeyword(startIndex: Int): Int {
    var index = startIndex
    while (index < length) {
        index = indexOf(ANNOTATIONS, startIndex = index, ignoreCase = true)
        if (index == -1) return -1
        if (isAnnotationKeywordBoundary(index - 1) && isAnnotationKeywordBoundary(index + ANNOTATIONS.length)) return index
        index += ANNOTATIONS.length
    }
    return -1
}

private fun String.isAnnotationKeywordBoundary(index: Int): Boolean =
    index !in indices || (!this[index].isLetterOrDigit() && this[index] != '_' && this[index] != '$' && this[index] != '#')

private fun String.skipAnnotationWhitespace(index: Int): Int {
    var current = index
    while (current < length && this[current].isWhitespace()) current++
    return current
}
