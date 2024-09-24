package com.ixume.particlesTesting.emitter.rate

import com.google.gson.JsonElement
import com.ixume.particlesTesting.emitter.EmitterMochaData
import com.ixume.particlesTesting.emitter.EmitterMochaFunction
import com.ixume.particlesTesting.parsing.expression
import javassist.LoaderClassPath
import team.unnamed.mocha.MochaEngine
import kotlin.math.floor

class SteadyRateComponent(private val spawnRate: EmitterMochaFunction, private val maxParticles: EmitterMochaFunction?) : RateComponent {
    companion object {
        fun parse(jsonElement: JsonElement): SteadyRateComponent? {
            val mochaEngine = MochaEngine.createStandard()
            mochaEngine.classPool().appendClassPath(LoaderClassPath(EmitterMochaFunction::class.java.classLoader))
            val jsonObject = jsonElement.asJsonObject
            return SteadyRateComponent(mochaEngine.compile(jsonObject.expression("spawn_rate") ?: return null, EmitterMochaFunction::class.java), mochaEngine.compile(jsonObject.expression("max_particles") ?: return null, EmitterMochaFunction::class.java))
        }
    }

    override fun toEmit(emitterData: EmitterMochaData): Int {
        val evaluatedSpawnRate = spawnRate.eval(emitterData.age)
        //evaluatedSpawnRate is per second, we need per tick.
        //tick spawn rate is floor(evaluatedSpawnRate / 20) + leftovers
        //leftovers = (evaluatedSpawnRate % 20). leftovers are every few ticks. should be evenly spaced throughout 20 tick interval.
        val leftovers = evaluatedSpawnRate % 20
        val leftoversBonus = if (leftovers != 0.0) {
            if (emitterData.age % (20.0 / leftovers) == 0.0) 1 else 0
        } else {
            0
        }

        return floor(evaluatedSpawnRate / 20.0).toInt() + leftoversBonus
    }
}