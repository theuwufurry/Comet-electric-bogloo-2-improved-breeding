package gg.aquatic.particleemitter.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.parsing.expression
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext
import kotlin.math.floor

class SteadyRateComponent(private val spawnRate: CompiledScript, private val myEmitterData: EmitterData) : RateComponent {
    companion object : ComponentParser<RateComponent> {
        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_steady" to this
        }

        override fun parse(jsonElement: JsonElement): SteadyRateComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData(0.0)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return SteadyRateComponent((engine as Compilable).compile(jsonObject.expression("spawn_rate") ?: return null), emitterData)
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
            if (otherEmitterData.age % (20.0 / leftovers).toInt() == 0.0) 1 else 0
        } else {
            0
        }

        return floor(evaluatedSpawnRate / 20.0).toInt() + leftoversBonus
    }
}