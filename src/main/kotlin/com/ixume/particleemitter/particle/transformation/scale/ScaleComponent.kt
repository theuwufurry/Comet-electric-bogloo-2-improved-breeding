package com.ixume.particleemitter.particle.transformation.scale

import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3f

interface ScaleComponent {
    fun scale(otherParticleData: ParticleData): Vector3f
}