package gg.aquatic.comet.emitter.action

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

class ActionContext(
    val otherParticleData: ParticleData?,
    val otherEmitterData: EmitterData,
    val pos: Vector3d?,
    val dir: Vector3d?
)