package dev.s7a.sqldelight.oracle.check.rule.rules

internal fun String.matchingSqlParenthesis(openOffset: Int): Int? {
    var depth = 0
    var index = openOffset
    while (index < length) {
        if (index != openOffset) {
            val skipped = skipSqlDelimitedText(index)
            if (skipped != null) {
                index = skipped
                continue
            }
        }

        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
                if (depth == 0) return index
            }
        }
        index++
    }
    return null
}

internal fun String.countTopLevelSqlItems(
    startOffset: Int,
    endOffset: Int,
): Int {
    var count = 1
    var depth = 0
    var hasContent = false
    var index = startOffset
    while (index < endOffset) {
        val skipped = skipSqlDelimitedText(index)
        if (skipped != null) {
            hasContent = true
            index = skipped
            continue
        }

        when (this[index]) {
            '(' -> {
                depth++
                hasContent = true
            }

            ')' -> {
                depth--
                hasContent = true
            }

            ',' -> {
                if (depth == 0) count++ else hasContent = true
            }

            else -> {
                if (!this[index].isWhitespace()) hasContent = true
            }
        }
        index++
    }
    return if (hasContent) count else 0
}

internal fun String.skipSqlDelimitedText(start: Int): Int? =
    when {
        startsSqlAlternativeQuotedString(start) -> skipSqlAlternativeQuotedString(start)
        getOrNull(start) == '\'' -> skipSqlQuotedString(start)
        getOrNull(start) == '"' -> skipSqlDoubleQuotedIdentifier(start)
        else -> null
    }

internal fun String.maskSqlCommentsAndQuotedTextPreservingOffsets(
    maskQuoteDelimiters: Boolean = true,
    maskDoubleQuotedIdentifiers: Boolean = true,
): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> {
                    sqlMaskRange(chars, index, skipSqlLineComment(index))
                }

                startsWith("/*", index) -> {
                    sqlMaskRange(chars, index, skipSqlBlockComment(index))
                }

                startsSqlAlternativeQuotedString(index) -> {
                    sqlMaskRange(chars, index, skipSqlAlternativeQuotedString(index))
                }

                chars[index] == '\'' -> {
                    val end = skipSqlQuotedString(index)
                    if (maskQuoteDelimiters) {
                        sqlMaskRange(chars, index, end)
                    } else {
                        sqlMaskRange(chars, index + 1, end - 1)
                    }
                }

                maskDoubleQuotedIdentifiers && chars[index] == '"' -> {
                    sqlMaskRange(chars, index, skipSqlDoubleQuotedIdentifier(index))
                }

                else -> {
                    index + 1
                }
            }
    }
    return String(chars)
}

internal fun String.skipSqlLineComment(start: Int): Int = indexOf('\n', startIndex = start).let { if (it == -1) length else it }

internal fun String.skipSqlBlockComment(start: Int): Int = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

internal fun String.startsSqlAlternativeQuotedString(start: Int): Boolean {
    val qIndex =
        when {
            getOrNull(start)?.equals('q', ignoreCase = true) == true -> start

            getOrNull(start)?.equals('n', ignoreCase = true) == true &&
                getOrNull(start + 1)?.equals('q', ignoreCase = true) == true -> start + 1

            else -> return false
        }
    return getOrNull(qIndex + 1) == '\'' && getOrNull(qIndex + 2) != null
}

internal fun String.skipSqlAlternativeQuotedString(start: Int): Int {
    val qIndex = if (getOrNull(start)?.equals('q', ignoreCase = true) == true) start else start + 1
    val openDelimiter = getOrNull(qIndex + 2) ?: return start + 1
    val closeDelimiter =
        when (openDelimiter) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '<' -> '>'
            else -> openDelimiter
        }
    var index = qIndex + 3
    while (index < length - 1) {
        if (this[index] == closeDelimiter && this[index + 1] == '\'') return index + 2
        index++
    }
    return length
}

internal fun String.skipSqlQuotedString(start: Int): Int = skipSqlDelimitedString(start, '\'')

internal fun String.skipSqlDoubleQuotedIdentifier(start: Int): Int = skipSqlDelimitedString(start, '"')

internal fun String.sqlIdentifierEnd(
    startOffset: Int,
    endOffset: Int,
): Int {
    if (startOffset >= endOffset) return startOffset
    if (this[startOffset] == '"') {
        val end = skipSqlDoubleQuotedIdentifier(startOffset)
        return if (end <= endOffset) end else startOffset
    }

    var index = startOffset
    while (index < endOffset && this[index].isSqlIdentifierPart()) {
        index++
    }
    return index
}

internal fun String.innermostSqlParenthesisStart(offset: Int): Int? {
    var depth = 0
    (offset - 1 downTo 0).forEach { index ->
        when (this[index]) {
            ')' -> {
                depth++
            }

            '(' -> {
                if (depth == 0) return index
                depth--
            }
        }
    }
    return null
}

internal data class SqlWord(
    val text: String,
    val startOffset: Int,
    val range: IntRange,
)

internal fun String.previousSqlWord(offset: Int): SqlWord? {
    var index = offset - 1
    while (index >= 0 && !this[index].isSqlIdentifierPart()) index--
    val end = index + 1
    while (index >= 0 && this[index].isSqlIdentifierPart()) index--
    val start = index + 1
    return if (end > start) {
        SqlWord(
            text = substring(start, end).uppercase(),
            startOffset = start,
            range = start until end,
        )
    } else {
        null
    }
}

private fun String.skipSqlDelimitedString(
    start: Int,
    delimiter: Char,
): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == delimiter) {
            if (index + 1 < length && this[index + 1] == delimiter) {
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

private fun Char.isSqlIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$' || this == '#'

internal data class SqlStaticStringLiteral(
    val value: String,
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.staticSqlStringLiteral(
    startOffset: Int,
    endOffset: Int,
): SqlStaticStringLiteral? {
    var start = startOffset
    while (start < endOffset && this[start].isWhitespace()) start++
    var end = endOffset
    while (end > start && this[end - 1].isWhitespace()) end--
    if (start >= end) return null
    if (startsSqlAlternativeQuotedString(start)) {
        val qIndex = if (this[start].equals('q', ignoreCase = true)) start else start + 1
        val literalEnd = skipSqlAlternativeQuotedString(start)
        if (literalEnd != end) return null
        return SqlStaticStringLiteral(
            value = substring(qIndex + 3, literalEnd - 2),
            startOffset = start,
            endOffset = literalEnd,
        )
    }
    if (this[start].equals('N', ignoreCase = true)) start++
    if (start >= end || this[start] != '\'') return null
    val literalEnd = skipSqlQuotedString(start)
    if (literalEnd != end) return null
    return SqlStaticStringLiteral(
        value = substring(start + 1, literalEnd - 1).replace("''", "'"),
        startOffset = start,
        endOffset = literalEnd,
    )
}

private fun sqlMaskRange(
    chars: CharArray,
    start: Int,
    end: Int,
): Int {
    for (index in start until end) {
        chars[index] = ' '
    }
    return end
}
