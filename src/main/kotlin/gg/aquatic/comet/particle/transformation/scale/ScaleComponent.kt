package gg.aquatic.comet.particle.transformation.scale

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3f

interface ScaleComponent {
    fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f
}