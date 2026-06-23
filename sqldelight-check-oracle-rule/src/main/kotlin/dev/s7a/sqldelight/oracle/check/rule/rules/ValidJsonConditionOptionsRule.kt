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
 * Reports conflicting or misplaced static Oracle SQL/JSON condition options.
 */
public class ValidJsonConditionOptionsRule : Rule {
    override val id: RuleId = RuleId("valid-json-condition-options")
    override val defaultSeverity: Severity = Severity.Warning
    override val targetDialect: DialectId = OracleDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.maskJsonOptionCommentsAndQuotedTextPreservingOffsets().jsonOptionTokens()
        tokens
            .jsonConditionOptionConflicts()
            .forEach { conflict ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use valid Oracle SQL/JSON condition options: ${conflict.group}.",
                        file = context.file,
                        range = content.rangeAtOffsets(conflict.first.startOffset, conflict.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class JsonConditionOptionConflict(
    val group: String,
    val first: JsonConditionOptionOccurrence,
    val second: JsonConditionOptionOccurrence,
)

private data class JsonConditionOptionOccurrence(
    val group: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class JsonOptionToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<JsonOptionToken>.jsonConditionOptionConflicts(): List<JsonConditionOptionConflict> =
    isJsonConditionOptionConflicts() + jsonFunctionOptionConflicts()

private fun List<JsonOptionToken>.isJsonConditionOptionConflicts(): List<JsonConditionOptionConflict> =
    flatMapIndexed { index, token ->
        if (!token.jsonOptionHasText("IS")) return@flatMapIndexed emptyList()
        val jsonIndex =
            when {
                getOrNull(index + 1).jsonOptionHasText("JSON") -> index + 1
                getOrNull(index + 1).jsonOptionHasText("NOT") && getOrNull(index + 2).jsonOptionHasText("JSON") -> index + 2
                else -> return@flatMapIndexed emptyList()
            }
        val endIndex = indexOfNextJsonConditionBoundary(jsonIndex + 1)
        val conditionTokens = subList(jsonIndex + 1, endIndex)
        conditionTokens.jsonOptionConflicts(
            occurrenceAt = { optionIndex -> conditionTokens.jsonConditionOptionOccurrence(optionIndex) },
        )
    }

private fun List<JsonOptionToken>.jsonFunctionOptionConflicts(): List<JsonConditionOptionConflict> =
    flatMapIndexed { index, token ->
        when {
            token.jsonOptionHasText("JSON_EXISTS") -> jsonFunctionOptions(index, allowsOnEmpty = true)
            token.jsonOptionHasText("JSON_EQUAL") -> jsonFunctionOptions(index, allowsOnEmpty = false)
            token.jsonOptionHasText("JSON_TEXTCONTAINS") -> jsonFunctionOptions(index, allowsOnEmpty = false)
            else -> emptyList()
        }
    }

private fun List<JsonOptionToken>.jsonFunctionOptions(
    functionIndex: Int,
    allowsOnEmpty: Boolean,
): List<JsonConditionOptionConflict> {
    val openIndex = indexOfNextToken(functionIndex + 1, "(") ?: return emptyList()
    val closeIndex = matchingRightParenthesis(openIndex) ?: return emptyList()
    val functionTokens = subList(openIndex + 1, closeIndex)
    val conflicts =
        functionTokens.jsonOptionConflicts(
            occurrenceAt = { optionIndex -> functionTokens.jsonConditionOptionOccurrence(optionIndex) },
        )
    if (allowsOnEmpty) return conflicts
    return conflicts +
        functionTokens.mapIndexedNotNull { index, token ->
            if (token.jsonOptionHasText("ON") && functionTokens.getOrNull(index + 1).jsonOptionHasText("EMPTY")) {
                JsonConditionOptionConflict(
                    group = "ON EMPTY is not valid for this SQL/JSON condition",
                    first = functionTokens.jsonConditionOptionOccurrence(index),
                    second = functionTokens.jsonConditionOptionOccurrence(index + 1),
                )
            } else {
                null
            }
        }
}

private fun List<JsonOptionToken>.jsonOptionConflicts(
    occurrenceAt: (Int) -> JsonConditionOptionOccurrence,
): List<JsonConditionOptionConflict> {
    val firstByGroup = linkedMapOf<String, JsonConditionOptionOccurrence>()
    return mapIndexedNotNull { index, token ->
        val group =
            when {
                token.jsonOptionHasText("STRICT") || token.jsonOptionHasText("LAX") -> {
                    "STRICT/LAX"
                }

                token.jsonOptionHasText("WITH") && getOrNull(index + 1).jsonOptionHasText("UNIQUE") -> {
                    "WITH UNIQUE KEYS/WITHOUT UNIQUE KEYS"
                }

                token.jsonOptionHasText("WITHOUT") && getOrNull(index + 1).jsonOptionHasText("UNIQUE") -> {
                    "WITH UNIQUE KEYS/WITHOUT UNIQUE KEYS"
                }

                token.jsonOptionHasText("ON") && getOrNull(index + 1).jsonOptionHasText("ERROR") -> {
                    "ON ERROR"
                }

                token.jsonOptionHasText("ON") && getOrNull(index + 1).jsonOptionHasText("EMPTY") -> {
                    "ON EMPTY"
                }

                else -> {
                    return@mapIndexedNotNull null
                }
            }
        val occurrence = occurrenceAt(index)
        val first = firstByGroup.putIfAbsent(group, occurrence)
        first?.let {
            JsonConditionOptionConflict(
                group = group,
                first = it,
                second = occurrence,
            )
        }
    }
}

private fun List<JsonOptionToken>.jsonConditionOptionOccurrence(index: Int): JsonConditionOptionOccurrence =
    when {
        getOrNull(index).jsonOptionHasText("WITH") &&
            getOrNull(index + 1).jsonOptionHasText("UNIQUE") &&
            getOrNull(index + 2).jsonOptionHasText("KEYS") -> {
            jsonConditionOptionOccurrence("WITH UNIQUE KEYS/WITHOUT UNIQUE KEYS", index, index + 2)
        }

        getOrNull(index).jsonOptionHasText("WITHOUT") &&
            getOrNull(index + 1).jsonOptionHasText("UNIQUE") &&
            getOrNull(index + 2).jsonOptionHasText("KEYS") -> {
            jsonConditionOptionOccurrence("WITH UNIQUE KEYS/WITHOUT UNIQUE KEYS", index, index + 2)
        }

        getOrNull(index).jsonOptionHasText("ON") &&
            (getOrNull(index + 1).jsonOptionHasText("ERROR") || getOrNull(index + 1).jsonOptionHasText("EMPTY")) -> {
            jsonConditionOptionOccurrence("${this[index].text} ${this[index + 1].text}", index, index + 1)
        }

        else -> {
            jsonConditionOptionOccurrence(this[index].text.uppercase(), index, index)
        }
    }

private fun List<JsonOptionToken>.jsonConditionOptionOccurrence(
    group: String,
    startIndex: Int,
    endIndex: Int,
): JsonConditionOptionOccurrence =
    JsonConditionOptionOccurrence(
        group = group,
        startOffset = this[startIndex].startOffset,
        endOffset = this[endIndex].endOffset,
    )

private fun List<JsonOptionToken>.indexOfNextJsonConditionBoundary(startIndex: Int): Int {
    var index = startIndex
    var depth = 0
    while (index < size) {
        when {
            this[index].jsonOptionHasText("(") -> depth++
            this[index].jsonOptionHasText(")") -> if (depth == 0) return index else depth--
            depth == 0 && (this[index].jsonOptionHasText("AND") || this[index].jsonOptionHasText("OR")) -> return index
        }
        index++
    }
    return size
}

private fun List<JsonOptionToken>.indexOfNextToken(
    startIndex: Int,
    text: String,
): Int? = (startIndex until size).firstOrNull { index -> this[index].jsonOptionHasText(text) }

private fun List<JsonOptionToken>.matchingRightParenthesis(openIndex: Int): Int? {
    var depth = 0
    for (index in openIndex until size) {
        when {
            this[index].jsonOptionHasText("(") -> {
                depth++
            }

            this[index].jsonOptionHasText(")") -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private val jsonOptionTokenPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\(|\)""")

private fun String.jsonOptionTokens(): List<JsonOptionToken> =
    jsonOptionTokenPattern
        .findAll(this)
        .map { match ->
            JsonOptionToken(
                text = match.value,
                startOffset = match.range.first,
                endOffset = match.range.last + 1,
            )
        }.toList()

private fun String.maskJsonOptionCommentsAndQuotedTextPreservingOffsets(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> {
                    jsonOptionMaskRange(chars, index, skipJsonOptionLineComment(index))
                }

                startsWith("/*", index) -> {
                    jsonOptionMaskRange(chars, index, skipJsonOptionBlockComment(index))
                }

                chars[index] == '\'' -> {
                    jsonOptionMaskRange(chars, index, skipJsonOptionQuotedString(index))
                }

                else -> {
                    index + 1
                }
            }
    }
    return String(chars)
}

private fun String.skipJsonOptionLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipJsonOptionBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipJsonOptionQuotedString(start: Int): Int {
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

private fun jsonOptionMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}

private fun JsonOptionToken?.jsonOptionHasText(text: String): Boolean = this?.text.equals(text, ignoreCase = true)
