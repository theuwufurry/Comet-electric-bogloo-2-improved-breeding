package gg.aquatic.comet.particle.position

import gg.aquatic.comet.emitter.ComponentResult
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

interface PositionComponent {
    fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d>
}