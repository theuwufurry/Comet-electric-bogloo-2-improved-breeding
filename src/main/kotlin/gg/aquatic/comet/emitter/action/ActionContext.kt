package gg.aquatic.comet.emitter.action

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.parent.Pose
import gg.aquatic.comet.particle.ParticleData

class ActionContext(
    val otherEmitterData: EmitterData,
    val otherParticleData: ParticleData?,
    val pose: Pose?,
)