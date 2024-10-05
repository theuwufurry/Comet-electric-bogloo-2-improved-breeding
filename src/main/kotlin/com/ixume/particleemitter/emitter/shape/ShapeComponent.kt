package com.ixume.particleemitter.emitter.shape

import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(otherParticleData: ParticleData): Vector3d
}