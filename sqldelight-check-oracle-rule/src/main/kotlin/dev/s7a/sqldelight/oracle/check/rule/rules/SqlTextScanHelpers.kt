package dev.s7a.sqldelight.oracle.check.rule.rules

internal fun String.matchingSqlParenthesis(openOffset: Int): Int? {
    var depth = 0
    for (index in openOffset until length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

            ')' -> {
                depth--
                if (depth == 0) return index
            }
        }
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
    for (index in startOffset until endOffset) {
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
    }
    return if (hasContent) count else 0
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

internal fun String.skipSqlQuotedString(start: Int): Int {
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

private fun String.skipSqlDoubleQuotedIdentifier(start: Int): Int =
    indexOf('"', startIndex = start + 1).let { if (it == -1) length else it + 1 }

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
