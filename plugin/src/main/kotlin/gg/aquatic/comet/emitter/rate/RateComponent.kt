package gg.aquatic.comet.emitter.rate

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine


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