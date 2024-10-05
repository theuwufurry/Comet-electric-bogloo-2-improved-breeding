package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface DirectionSubcomponent {
    fun dir(otherParticleData: ParticleData): Vector3d
}