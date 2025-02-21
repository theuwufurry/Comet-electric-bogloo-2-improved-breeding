package gg.aquatic.comet.snowstorm.transpilation.expression

import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.token.Token

sealed interface Expr {
    fun <R> accept(visitor: Visitor<R>): R

    interface Visitor<R> {
        fun visitBinaryExpr(binaryExpr: BinaryExpr): R
        fun visitGroupingExpr(groupingExpr: GroupingExpr): R
        fun visitLiteralExpr(literalExpr: LiteralExpr): R
        fun visitUnaryExpr(unaryExpr: UnaryExpr): R
        fun visitMathExpr(mathExpr: MathExpr): R
        fun visitVar(variable: VarExpr): R
        fun visitSetVar(assignExpr: AssignExpr): R
    }
}

class BinaryExpr(
    val left: Expr,
    val operator: Token,
    val right: Expr
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitBinaryExpr(this)
    }
}

class GroupingExpr(
    val expr: Expr
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitGroupingExpr(this)
    }
}

class LiteralExpr(
    val value: Any?
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitLiteralExpr(this)
    }
}

class UnaryExpr(
    val operator: Token,
    val right: Expr
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitUnaryExpr(this)
    }
}

/**
 * Use factory function when parsing!
 */
class MathExpr(
    val identifier: Token,
    val args: List<Expr>
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitMathExpr(this)
    }

    companion object {
        /**
         * identifier : arity
         */
        val funcs = mapOf(
            "sin" to MathFunction(1) { args -> "Math.sin((${args.first()}) * Math.PI / 180.0)" },
            "cos" to MathFunction(1) { args -> "Math.cos((${args.first()}) * Math.PI / 180.0)" },
            "random" to MathFunction(2) { args -> "(Math.random() * (${args[1]} - ${args[0]}) + ${args[0]})" }
        )

        fun mathExpr(identifier: Token, args: List<Expr>): Pair<MathExpr?, String?> {
            val func = funcs[identifier.lexeme] ?: return null to "${identifier.lexeme} is not a math function!"

            if (args.size != func.arity) {
                return null to "${identifier.lexeme} requires $func arguments!"
            }

            return MathExpr(identifier, args) to null
        }
    }

    class MathFunction(
        val arity: Int,
        val stringifier: (args: List<String>) -> String
    )
}

class VarExpr private constructor(
    var name: Token
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitVar(this)
    }

    companion object {
        val fields: Map<String, String> = mapOf(
            "variable.emitter_age" to "(emitter.age / 20.0)",
            "variable.emitter_random_1" to "emitter_variable.random",
            "variable.emitter_random_2" to "emitter_variable.random2",
            "variable.emitter_random_3" to "emitter_variable.random3",
            "variable.emitter_random_4" to "emitter_variable.random4",
            "variable.particle_age" to "(particle.age / 20.0)",
            "variable.particle_lifetime" to "(particle.maxLife / 20.0)",
            "variable.particle_random_1" to "particle_variable.random",
            "variable.particle_random_2" to "particle_variable.random2",
            "variable.particle_random_3" to "particle_variable.random3",
            "variable.particle_random_4" to "particle_variable.random4",
        )

        fun createVar(identifier: Token, deserializedParticleEffect: DeserializedParticleEffect): VarExpr {
            if (fields[identifier.lexeme] == null) {
                return VarExpr(identifier)
            }

            val emitterRandoms = when (identifier.lexeme) {
                "variable.emitter_random_1" -> 1
                "variable.emitter_random_2" -> 2
                "variable.emitter_random_3" -> 3
                "variable.emitter_random_4" -> 4
                else -> 0
            }

            val particleRandoms = when (identifier.lexeme) {
                "variable.particle_random_1" -> 1
                "variable.particle_random_2" -> 2
                "variable.particle_random_3" -> 3
                "variable.particle_random_4" -> 4
                else -> 0
            }

            deserializedParticleEffect.specifyRandoms(emitterRandoms, particleRandoms)

            return VarExpr(identifier)
        }
    }
}

class AssignExpr(
    val identifier: Token,
    val expr: Expr
) : Expr {
    override fun <R> accept(visitor: Expr.Visitor<R>): R {
        return visitor.visitSetVar(this)
    }
}