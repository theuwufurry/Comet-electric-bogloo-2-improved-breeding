package gg.aquatic.comet.emitter.lifetime

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine

interface EmitterLifetimeComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            engine.compile("emitter.age - 100.0", null)

            return TimedEmitterLifetimeComponent(
                engine.compile(
                    "emitter.age - 100.0", null
                ), null, emitterData
            )
        }
    }
}