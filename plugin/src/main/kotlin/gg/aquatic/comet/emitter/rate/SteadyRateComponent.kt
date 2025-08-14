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
import kotlin.math.floor
import kotlin.math.min

class SteadyRateComponent(
    private val spawnRate: Expr<Number>,
    private val maxParticles: Expr<Number>?,
    private val myEmitterData: EmitterData
) :
    RateComponent {
    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val evaluatedMaxParticles = maxParticles?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: Integer.MAX_VALUE
        if (otherEmitterData.emitter!!.particles.size >= evaluatedMaxParticles) return 0

        val evaluatedSpawnRate = spawnRate.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0
        //evaluatedSpawnRate is per second, we need per tick.
        //tick spawn rate is floor(evaluatedSpawnRate / 20) + leftovers
        //leftovers = (evaluatedSpawnRate % 20). leftovers are every few ticks. should be evenly spaced throughout 20 tick interval.
        val leftovers = evaluatedSpawnRate % 20.0
        val leftoversBonus = if (leftovers != 0.0) {
            if (myEmitterData.age % (20.0 / leftovers).toInt() == 0.0) 1 else 0
        } else {
            0
        }

        return min(
            floor(evaluatedSpawnRate / 20.0).toInt() + leftoversBonus,
            evaluatedMaxParticles - otherEmitterData.emitter!!.particles.size
        )
    }

    companion object : ComponentParser<SteadyRateComponent> {
        override val id: String = "emitter_rate_steady"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<SteadyRateComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return Result.success(
                SteadyRateComponent(
                    jsonObject.getExpr("spawn_rate")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros).fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("max_particles")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    emitterData
                )
            )
        }

        fun default(): RateComponent {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return SteadyRateComponent(
                "20".constructExpr<Number>(engine, null).getOrThrow(),
                null,
                emitterData
            )
        }
    }
}