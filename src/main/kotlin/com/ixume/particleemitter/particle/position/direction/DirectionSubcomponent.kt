package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface DirectionSubcomponent {
    fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}