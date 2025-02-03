package gg.aquatic.comet.particle.lifetime

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.particleEngine


interface ParticleLifetimeComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ParticleLifetimeExpressionComponent(
                null,
                engine.compile("20", null),
                particleData, emitterData
            )
        }
    }
}