package com.ixume.particleemitter.emitter.rate

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.expression
import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript
import kotlin.math.floor

class SteadyRateComponent(private val spawnRate: CompiledScript, private val maxParticles: CompiledScript?) : RateComponent {
    companion object {
        fun parse(jsonElement: JsonElement): SteadyRateComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine as Compilable
            val jsonObject = jsonElement.asJsonObject
            return SteadyRateComponent(engine.compile(jsonObject.expression("spawn_rate") ?: return null), engine.compile(jsonObject.expression("max_particles") ?: return null))
        }
    }

    override fun toEmit(emitterData: EmitterData, bindings: Bindings): Int {
        val evaluatedSpawnRate = spawnRate.eval(bindings) as Double
        println(evaluatedSpawnRate)
        //evaluatedSpawnRate is per second, we need per tick.
        //tick spawn rate is floor(evaluatedSpawnRate / 20) + leftovers
        //leftovers = (evaluatedSpawnRate % 20). leftovers are every few ticks. should be evenly spaced throughout 20 tick interval.
        val leftovers = evaluatedSpawnRate % 20.0
        val leftoversBonus = if (leftovers != 0.0) {
            if (emitterData.age % (20.0 / leftovers).toInt() == 0.0) 1 else 0
        } else {
            0
        }

        return floor(evaluatedSpawnRate / 20.0).toInt() + leftoversBonus
    }
}