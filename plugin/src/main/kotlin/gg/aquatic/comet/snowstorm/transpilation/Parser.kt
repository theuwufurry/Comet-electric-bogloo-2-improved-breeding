package gg.aquatic.comet.snowstorm.transpilation

import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType.*
import gg.aquatic.comet.snowstorm.transpilation.expression.*

/*
 */

class Parser(
    val tokens: List<Token>,
    val deserializedParticleEffect: DeserializedParticleEffect
) {
    private var current = 0

    fun parse(): Expr {
        return expression()
    }

    private fun expression(): Expr {
        return equality()
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(1, NOT_EQUAL, EQUAL_EQUAL)) {
            val operator = tokens[current]
            current++
            val right = comparison()
            expr = BinaryExpr(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(1, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            val operator = tokens[current]
            current++
            val right = term()
            expr = BinaryExpr(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(1, MINUS, PLUS)) {
            val operator = tokens[current]
            current++
            val right = factor()
            expr = BinaryExpr(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(1, STAR, SLASH)) {
            val operator = tokens[current]
            current++
            val right = unary()
            expr = BinaryExpr(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        while (match(0, BANG, MINUS)) {
            val operator = tokens[current]
            current++
            val right = unary()
            return UnaryExpr(operator, right)
        }

        return math()
    }

    private fun math(): Expr {
        if (match(0, MATH) && match(1, DOT) && match(1, IDENTIFIER)) {
            val identifier = tokens[current]
            if (match(1, LEFT_PAREN)) {
                val args: MutableList<Expr> = mutableListOf()
                if (!match(1, RIGHT_PAREN)) {
                    do {
                        current++
                        args += expression()
                    } while (match(1, COMMA))

                    current++
                }

                if (!match(0, RIGHT_PAREN)) {
                    throw IllegalStateException("Missing a right paren!")
                }

                val (result, error) = MathExpr.mathExpr(identifier, args)
                if (error != null) error(error)
                return result!!
            }
        }

        return primary()
    }

    private fun primary(): Expr {
        if (match(0, FALSE)) return LiteralExpr(false)
        if (match(0, TRUE)) return LiteralExpr(true)

        if (match(0, NUMBER, STRING)) {
            return LiteralExpr(tokens[current].literal)
        }

        if (match(0, LEFT_PAREN)) {
            current++
            val expr = expression()
            current++
            assert(tokens[current].type == RIGHT_PAREN)
            return GroupingExpr(expr)
        }

        if (match(0, IDENTIFIER)) {
            val identifier = tokens[current]
            val variable = Var.createVar(identifier, deserializedParticleEffect)
            return variable
        }

        throw IllegalStateException("Something went wrong at token ${tokens[current]}!")
    }

    private fun match(
        offset: Int,
        vararg types: TokenType
    ): Boolean {
        for (type in types) {
            if (current + offset >= tokens.size) return false
            if (tokens[current + offset].type == type) {
                current += offset
                return true
            }
        }

        return false
    }

    private fun outsideTokens(): Boolean {
        return current >= tokens.size
    }
}