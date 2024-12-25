package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

interface DirectionSubcomponent {
    fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}