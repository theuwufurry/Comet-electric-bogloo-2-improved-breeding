package gg.aquatic.comet.particle.lifetime

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.particleEngine

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