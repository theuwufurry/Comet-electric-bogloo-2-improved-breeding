package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import javax.script.CompiledScript
import kotlin.math.floor

class SteadyRateComponent(private val spawnRate: CompiledScript, private val myEmitterData: EmitterData) :
    RateComponent {
    companion object : ComponentParser<SteadyRateComponent> {
        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_steady" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SteadyRateComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return SteadyRateComponent(
                engine.compile(jsonObject.expression("spawn_rate") ?: return null, macros),
                emitterData
            )
        }
    }

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val evaluatedSpawnRate = spawnRate.eval() as Double
        //evaluatedSpawnRate is per second, we need per tick.
        //tick spawn rate is floor(evaluatedSpawnRate / 20) + leftovers
        //leftovers = (evaluatedSpawnRate % 20). leftovers are every few ticks. should be evenly spaced throughout 20 tick interval.
        val leftovers = evaluatedSpawnRate % 20.0
        val leftoversBonus = if (leftovers != 0.0) {
            if (myEmitterData.age % (20.0 / leftovers).toInt() == 0.0) 1 else 0
        } else {
            0
        }

        return floor(evaluatedSpawnRate / 20.0).toInt() + leftoversBonus
    }
}