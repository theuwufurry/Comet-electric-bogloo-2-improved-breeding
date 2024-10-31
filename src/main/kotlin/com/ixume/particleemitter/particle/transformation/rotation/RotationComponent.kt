package com.ixume.particleemitter.particle.transformation.rotation

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Matrix4f

interface RotationComponent {
    fun rotation(otherEmitterData: EmitterData, otherParticleData: ParticleData): Matrix4f
}