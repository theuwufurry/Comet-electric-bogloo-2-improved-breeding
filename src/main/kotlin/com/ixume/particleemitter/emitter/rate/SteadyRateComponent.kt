package com.ixume.particleemitter.emitter.rate

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.emitter.lifetime.TimedEmitterLifetimeComponent
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import javax.script.CompiledScript
import kotlin.math.floor

class SteadyRateComponent(private val spawnRate: CompiledScript, private val myEmitterData: EmitterData) : RateComponent {
    companion object : ComponentParser<SteadyRateComponent> {
        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_steady" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<SteadyRateComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedSteadyRateComponent(jsonObject.expression("spawn_rate") ?: return null, macros)
        }
    }

    override fun toEmit(): Int {
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

class UnrealizedSteadyRateComponent(private val spawnRate: String, private val macros: Map<String, Macro>?): UnrealizedComponent<SteadyRateComponent>  {
    override fun realizeComponent(emitterData: EmitterData): SteadyRateComponent {
        val engine = emitterEngine(emitterData)
        return SteadyRateComponent(engine.compile(spawnRate, macros), emitterData)
    }
}