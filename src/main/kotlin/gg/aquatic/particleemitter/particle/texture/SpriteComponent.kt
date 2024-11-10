package gg.aquatic.particleemitter.particle.texture

import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.particle.ParticleData

interface SpriteComponent {
    fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String
}