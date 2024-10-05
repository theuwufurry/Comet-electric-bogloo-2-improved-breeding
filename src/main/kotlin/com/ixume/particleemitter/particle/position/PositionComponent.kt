package com.ixume.particleemitter.particle.position

import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface PositionComponent {
    fun pos(otherParticleData: ParticleData): Vector3d
}