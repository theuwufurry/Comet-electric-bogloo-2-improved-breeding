package gg.aquatic.comet.particle.lifetime

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData

interface ParticleLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean

    companion object {
        fun default(): ParticleLifetimeComponent {
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