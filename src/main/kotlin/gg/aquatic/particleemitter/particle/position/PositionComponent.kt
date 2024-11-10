package gg.aquatic.particleemitter.particle.position

import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface PositionComponent {
    fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}