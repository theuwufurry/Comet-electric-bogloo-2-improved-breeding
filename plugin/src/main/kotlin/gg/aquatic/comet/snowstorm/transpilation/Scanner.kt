package gg.aquatic.comet.snowstorm.transpilation

import gg.aquatic.comet.Result
import gg.aquatic.comet.Result.Companion.with
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType.*

class Scanner(
    private val source: String
) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): Result<List<Token>, Error> {
        val errors: MutableList<Error> = mutableListOf()

        while (!outsideSource()) {
            start = current

            val char = source[current]

            if (char == ' ' || char == '\r' || char == '\t') {
                current++
                continue
            }

            if (char == '\n') {
                line++
                continue
            }

            when (char) {
                '(' -> tokens += token(LEFT_PAREN)
                ')' -> tokens += token(RIGHT_PAREN)
                '{' -> tokens += token(LEFT_BRACE)
                '}' -> tokens += token(RIGHT_BRACE)
                ',' -> tokens += token(COMMA)
                '.' -> tokens += token(DOT)
                '-' -> tokens += token(MINUS)
                '+' -> tokens += token(PLUS)
                ';' -> tokens += token(SEMICOLON)
                '*' -> tokens += token(STAR)
                '/' -> tokens += token(SLASH)

                '!' -> tokens += token(if (matchNext('=')) NOT_EQUAL else BANG)
                '=' -> tokens += token(if (matchNext('=')) EQUAL_EQUAL else EQUAL)
                '>' -> tokens += token(if (matchNext('=')) GREATER_EQUAL else GREATER)
                '<' -> tokens += token(if (matchNext('=')) LESS_EQUAL else LESS)

                '"' -> {
                    val (token, error) = string()
                    if (token != null) tokens += token
                    else errors += error!!
                }

                else -> {
                    if (char.isDigit()) {
                        digit()?.let { tokens += it }
                    } else if (char.isAlpha()) {
                        identifier().let { tokens += it }
                    } else {
                        errors += UnexpectedCharacterError(line, char)
                    }
                }
            }

            current++
        }

        tokens += Token(EOF, "", null, line)

        return tokens with errors
    }

    /**
     * Creates a token start..current
     */
    private fun token(
        type: TokenType,
        literal: Any? = null
    ): Token {
        return Token(
            type,
            source.substring(start, current + 1),
            literal,
            line
        )
    }

    private fun matchNext(expected: Char): Boolean {
        if (current + 1 >= source.length) return false
        if (source[current + 1] != expected) return false

        current++
        return true
    }

    private fun string(): Pair<Token?, UnterminatedStringError?> {
        do {
            if (source[current] == '\n') line++
            current++
        } while (!outsideSource() && source[current] != '"')

        if (outsideSource()) return null to UnterminatedStringError(line, source.substring(start))

        //now at '"' character
        return token(STRING, source.substring(start + 1, current)) to null
    }

    private fun digit(): Token? {
        do {
            current++
        } while (!outsideSource() && source[current].isDigit())

        if (current + 1 < source.length && source[current] == '.' && source[current + 1].isDigit()) {
            current++

            do {
                current++
            } while (!outsideSource() && source[current].isDigit())
        }

        //current is not a number, shift back to number so that the NEXT character is a start of a lexeme
        //going to be here: 1.243_ <- current, need to be ON number
        current--

        return token(NUMBER, source.substring(start, current + 1).toDoubleOrNull() ?: return null)
    }

    private fun identifier(): Token {
        while (true) {
            do {
                current++
            } while (current + 1 < source.length && source[current + 1].isAlphanumeric())

            if (!(current + 1 < source.length && source[current + 1] == '.')) {
                break
            } else if (source.substring(start, current + 1) in keywords.keys) {
                break
            } else {
                current++
            }
        }

        val str = source.substring(start, current + 1)
        val type = keywords[str] ?: IDENTIFIER

        return token(type)
    }

    private fun outsideSource(): Boolean {
        return current >= source.length
    }

    class UnexpectedCharacterError(
        val line: Int,
        val offender: Char
    ) : Error() {
        override fun toString(): String {
            return "Offending character '$offender' at line $line!"
        }
    }

    class UnterminatedStringError(
        val line: Int,
        val str: String
    ) : Error() {
        override fun toString(): String {
            return "Unterminated string $str at line $line"
        }
    }

    companion object {
        val keywords = mapOf(
            "if" to IF,
            "else" to ELSE,
            "true" to TRUE,
            "false" to FALSE,
            "math" to MATH,
            "Math" to MATH
        )
    }
}

fun Char.isAlpha(): Boolean {
    return (this in 'a'..'z') ||
            (this in 'A'..'Z') ||
            this == '_'
}

fun Char.isAlphanumeric(): Boolean {
    return isAlpha() || isDigit()
}
