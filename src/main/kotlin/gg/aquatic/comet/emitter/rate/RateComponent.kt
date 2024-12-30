package gg.aquatic.comet.emitter.rate

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.emitterEngine

interface RateComponent {
    fun toEmit(otherEmitterData: EmitterData): Int

    companion object {
        fun default(): RateComponent {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return SteadyRateComponent(
                engine.compile("20", null),
                emitterData
            )
        }
    }
}