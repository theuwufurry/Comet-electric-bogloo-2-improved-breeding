package gg.aquatic.comet.snowstorm.transpilation

import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.*

class JavascriptPrinter(
    private val deserializedEffect: DeserializedParticleEffect
) : Expr.Visitor<String> {
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
        return "${unaryExpr.operator.lexeme}${unaryExpr.right.accept(this)}"
    }

    override fun visitMathExpr(mathExpr: MathExpr): String {
        return MathExpr.funcs[mathExpr.identifier.lexeme]!!.stringifier(mathExpr.args.map { it.accept(this) })
    }

    override fun visitVar(variable: VarExpr): String {
        val implicit = VarExpr.fields.firstNotNullOfOrNull { (r, s) -> if (r.matches(variable.name.lexeme)) s else null }

        if (implicit != null) {
            return implicit
        }

        if (deserializedEffect.curves.any { it.name == variable.name.lexeme }) {
            return "__${variable.name.lexeme.replace('.', '_')}__"
        }

        return if (variable.name.lexeme.startsWith("variable")) {
            variable.name.lexeme.replaceFirst("variable", "emitter_variable")
        } else {
            "emitter_variable.${variable.name.lexeme}"
        }
    }

    override fun visitSetVar(assignExpr: AssignExpr): String {
        return if (assignExpr.identifier.lexeme.startsWith("variable")) {
            "${
                assignExpr.identifier.lexeme.replaceFirst(
                    "variable",
                    "emitter_variable"
                )
            }=${assignExpr.expr.accept(this)}"
        } else {
            "emitter_variable.${assignExpr.identifier.lexeme}=${assignExpr.expr.accept(this)}"
        }
    }

    fun print(exprs: List<Expr>): String {
        return exprs.joinToString(separator = ";") {
            it.accept(this)
        }
    }
}