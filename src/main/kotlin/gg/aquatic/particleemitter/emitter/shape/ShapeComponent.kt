package gg.aquatic.particleemitter.emitter.shape

import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}