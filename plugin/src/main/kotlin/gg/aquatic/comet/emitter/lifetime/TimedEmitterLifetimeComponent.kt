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

class TimedEmitterLifetimeComponent(
    private val lifetimeScript: Expr<Number>?,
    private val maxLifeScript: Expr<Number>?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override val priority = -1
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
        myEmitterData.copyFrom(otherEmitterData)
        otherEmitterData.dead = !(lifetimeScript?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()?.let {
            it <= 0.0
        } ?: maxLifeScript?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()?.let {
            otherEmitterData.age <= it
        } ?: false)
    }

    override fun die(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        override val id: String = "timed_emitter_lifetime"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<TimedEmitterLifetimeComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return Result.success(TimedEmitterLifetimeComponent(
                jsonObject.getExprOrNull("expiration_expression")?.constructExpr<Number>(
                        engine, macros
                    )?.fold(
                        { it },
                        { return Result.failure(it) }
                    ),
                jsonObject.getExprOrNull("max_lifetime")?.constructExpr<Number>(
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