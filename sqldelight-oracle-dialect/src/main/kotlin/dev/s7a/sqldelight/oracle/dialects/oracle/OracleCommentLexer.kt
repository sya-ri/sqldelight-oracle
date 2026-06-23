package dev.s7a.sqldelight.oracle.dialects.oracle

import com.alecstrong.sql.psi.core.SqlLexerAdapter
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lexer.LexerBase
import com.intellij.lexer.LexerPosition
import com.intellij.psi.tree.IElementType

internal class OracleCommentLexer : LexerBase() {
    private val delegate = SqlLexerAdapter()
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
    ) {
        this.buffer = buffer
        this.endOffset = endOffset
        locateToken(startOffset)
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        locateToken(tokenEnd)
    }

    override fun getCurrentPosition(): LexerPosition =
        OracleLexerPosition(
            offset = tokenStart,
            state = state,
        )

    override fun restore(position: LexerPosition) {
        locateToken(position.offset)
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun locateToken(offset: Int) {
        if (offset >= endOffset) {
            tokenStart = endOffset
            tokenEnd = endOffset
            tokenType = null
            return
        }

        if (isBlockCommentStart(offset)) {
            tokenStart = offset
            tokenEnd = blockCommentEnd(offset)
            tokenType = SqlTypes.COMMENT
            return
        }

        delegate.start(buffer, offset, endOffset, 0)
        tokenStart = delegate.tokenStart
        tokenEnd = delegate.tokenEnd
        tokenType = delegate.tokenType
    }

    private fun isBlockCommentStart(offset: Int): Boolean =
        offset + 1 < endOffset &&
            buffer[offset] == '/' &&
            buffer[offset + 1] == '*'

    private fun blockCommentEnd(offset: Int): Int {
        var index = offset + 2
        while (index + 1 < endOffset) {
            if (buffer[index] == '*' && buffer[index + 1] == '/') {
                return index + 2
            }
            index += 1
        }
        return endOffset
    }

    private data class OracleLexerPosition(
        private val offset: Int,
        private val state: Int,
    ) : LexerPosition {
        override fun getOffset(): Int = offset

        override fun getState(): Int = state
    }
}
