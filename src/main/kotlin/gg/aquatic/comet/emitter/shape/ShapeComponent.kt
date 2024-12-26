package gg.aquatic.comet.emitter.shape

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}