package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class InstantRateComponent(
    private val amount: Expr<Number>,
    private val offset: Expr<Number>?,
    private val myEmitterData: EmitterData
) :
    RateComponent {
    companion object : ComponentParser<InstantRateComponent> {

        override val id: String = "emitter_rate_instant"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<InstantRateComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return Result.success(
                InstantRateComponent(
                    jsonObject.getExpr("amount").fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros).fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("offset")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    emitterData
                )
            )
        }
    }

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val offsetTicks = offset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0
        return if (otherEmitterData.age == 1.0 + offsetTicks) {
            amount.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: return 0
        } else 0
    }
}