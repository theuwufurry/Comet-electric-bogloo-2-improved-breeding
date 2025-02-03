package gg.aquatic.comet.api.emitter.action

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.particle.ParticleData

class ActionContext(
    val otherEmitterData: EmitterData,
    val otherParticleData: ParticleData?,
    val pose: Pose?,
)