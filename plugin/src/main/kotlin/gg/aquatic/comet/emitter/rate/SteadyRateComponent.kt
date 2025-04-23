package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript
import kotlin.math.floor
import kotlin.math.min

class SteadyRateComponent(
    private val spawnRate: CompiledScript,
    private val maxParticles: CompiledScript?,
    private val myEmitterData: EmitterData
) :
    RateComponent {
    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val evaluatedMaxParticles = maxParticles?.let {
            (it.eval() as Number).toInt()
        } ?: Integer.MAX_VALUE
        if (otherEmitterData.emitter!!.particles.size >= evaluatedMaxParticles) return 0

        val evaluatedSpawnRate = (spawnRate.eval() as Number).toDouble()
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

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SteadyRateComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return SteadyRateComponent(
                engine.compile(jsonObject.expression("spawn_rate") ?: return null, macros),
                jsonObject.expression("max_particles")?.let { engine.compile(it, macros) },
                emitterData
            )
        }

        fun default(): RateComponent {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return SteadyRateComponent(
                engine.compile("20", null),
                null,
                emitterData
            )
        }
    }
}