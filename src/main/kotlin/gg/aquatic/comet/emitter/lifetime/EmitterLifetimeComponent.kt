package gg.aquatic.comet.emitter.lifetime

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.emitterEngine

interface EmitterLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData): Boolean

    companion object {
        fun default(): EmitterLifetimeComponent {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return TimedEmitterLifetimeComponent(
                engine.compile(
                    "emitter.age - 100.0", null
                ), emitterData
            )
        }
    }
}