package gg.aquatic.comet.snowstorm.transpilation

import gg.aquatic.comet.snowstorm.transpilation.expression.*

object JavascriptPrinter : Expr.Visitor<String> {
    override fun visitBinaryExpr(binaryExpr: BinaryExpr): String {
        return "${binaryExpr.left.accept(this)} ${binaryExpr.operator.lexeme} ${binaryExpr.right.accept(this)}"
    }

    override fun visitGroupingExpr(groupingExpr: GroupingExpr): String {
        return "(${groupingExpr.expr.accept(this)})"
    }

    override fun visitLiteralExpr(literalExpr: LiteralExpr): String {
        return literalExpr.value!!.toString()
    }

    override fun visitUnaryExpr(unaryExpr: UnaryExpr): String {
        return "${unaryExpr.operator.lexeme}${unaryExpr.right.accept(this)})"
    }

    override fun visitMathExpr(mathExpr: MathExpr): String {
        return MathExpr.funcs[mathExpr.identifier.lexeme]!!.stringifier(mathExpr.args.joinToString { it.accept(this) })
    }

    override fun visitVar(implicitVar: Var): String {
        return implicitVar.name
    }

    fun print(expr: Expr): String {
        return expr.accept(this)
    }
}