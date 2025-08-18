package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ExpressionEmitterLifetimeComponent(
    private val expirationScript: Expr<Number>?,
    private val activationScript: Expr<Number>?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override val priority = -1
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
        myEmitterData.copyFrom(otherEmitterData)

        otherEmitterData.dead = expirationScript?.run {
            eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()?.let {
                it <= 0.0
            }
        } ?: false

        otherEmitterData.isActive = activationScript?.run {
            eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()?.let {
                it > 0.0
            }
        } ?: true
    }

    override fun die(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        override val id: String = "expression_emitter_lifetime"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<ExpressionEmitterLifetimeComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return Result.success(
                ExpressionEmitterLifetimeComponent(
                    jsonObject.getExprOrNull("expiration_expression")?.constructExpr<Number>(
                        engine, macros
                    )?.fold(
                        { it },
                        { return Result.failure(it) }
                    ),
                    jsonObject.getExprOrNull("activation_expression")?.constructExpr<Number>(
                        engine, macros
                    )?.fold(
                        { it },
                        { return Result.failure(it) }
                    ),
                    emitterData
                ))
        }
    }
}