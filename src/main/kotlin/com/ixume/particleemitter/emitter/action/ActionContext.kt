package com.ixume.particleemitter.emitter.action

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d

class ActionContext(
    val otherParticleData: ParticleData?,
    val otherEmitterData: EmitterData,
    val pos: Vector3d?,
    val dir: Vector3d?
)