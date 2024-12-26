package gg.aquatic.comet.particle.lifetime

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData

interface ParticleLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean
}