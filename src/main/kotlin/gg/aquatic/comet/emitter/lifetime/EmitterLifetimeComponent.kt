package gg.aquatic.comet.emitter.lifetime

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.emitterEngine

interface EmitterLifetimeComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return TimedEmitterLifetimeComponent(
                engine.compile(
                    "emitter.age - 100.0", null
                ), null,emitterData
            )
        }
    }
}