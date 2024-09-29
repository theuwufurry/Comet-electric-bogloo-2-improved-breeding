package com.ixume.particleemitter.emitter.shape

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d
}