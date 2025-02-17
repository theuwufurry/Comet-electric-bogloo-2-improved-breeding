package gg.aquatic.comet.snowstorm.transpilation

import gg.aquatic.comet.snowstorm.transpilation.expression.*
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType

class ConstantResolver : Expr.Visitor<Expr> {
    fun resolve(exprs: List<Expr>): List<Expr> {
        return exprs.map { it.accept(this) }
    }

    override fun visitBinaryExpr(binaryExpr: BinaryExpr): Expr {
        val left = binaryExpr.left.accept(this)
        val right = binaryExpr.right.accept(this)
        if (left is LiteralExpr && left.value != null && left.value is Double
            && right is LiteralExpr && right.value != null && right.value is Double
        ) {
            return when (binaryExpr.operator.type) {
                TokenType.MINUS -> {
                    LiteralExpr(left.value - right.value)
                }

                TokenType.PLUS -> {
                    LiteralExpr(left.value + right.value)
                }

                TokenType.STAR -> {
                    LiteralExpr(left.value * right.value)
                }

                TokenType.SLASH -> {
                    LiteralExpr(left.value / right.value)
                }

                else -> binaryExpr
            }
        }

        return binaryExpr
    }

    override fun visitGroupingExpr(groupingExpr: GroupingExpr): Expr {
        return groupingExpr.expr.accept(this)
    }

    override fun visitLiteralExpr(literalExpr: LiteralExpr): Expr {
        return literalExpr
    }

    override fun visitUnaryExpr(unaryExpr: UnaryExpr): Expr {
        if (unaryExpr.right is LiteralExpr && unaryExpr.right.value != null) {
            if (unaryExpr.right.value is Double) {
                if (unaryExpr.operator.type == TokenType.MINUS) {
                    LiteralExpr(-1 * unaryExpr.right.value)
                }
            }
        }

        return unaryExpr
    }

    override fun visitMathExpr(mathExpr: MathExpr): Expr {
        return MathExpr(mathExpr.identifier, mathExpr.args.map { it.accept(this) })
    }

    override fun visitVar(variable: VarExpr): Expr {
        return variable
    }

    override fun visitSetVar(assignExpr: AssignExpr): Expr {
        return AssignExpr(assignExpr.identifier, assignExpr.expr.accept(this))
    }
}