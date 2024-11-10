package gg.aquatic.particleemitter.particle.lifetime

import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.particle.ParticleData


interface ParticleLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean
}