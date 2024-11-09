package com.ixume.particleemitter.particle.position

import com.ixume.particleemitter.emitter.ComponentResult
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

interface PositionComponent {
    fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d>
}