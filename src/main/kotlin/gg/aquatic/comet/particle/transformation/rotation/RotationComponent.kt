package gg.aquatic.comet.particle.transformation.rotation

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Quaternionf

interface RotationComponent {
    fun rotation(otherEmitterData: EmitterData, otherParticleData: ParticleData): Quaternionf
}