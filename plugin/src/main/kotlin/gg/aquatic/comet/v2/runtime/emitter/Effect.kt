package gg.aquatic.comet.v2.runtime.emitter

import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EffectRuntime
import java.util.concurrent.atomic.AtomicBoolean

interface Effect {
    val runtime: EffectRuntime
    val api: JSEffectAPI
    val proxy: V2EffectProxy
    var parent: Parent?
    val relPose: Pose
    val pose: Pose

    fun tick()

    fun createParticle(): V2ParticleData

    /**
     * Must be called on same thread as effect
     */
    fun onKill()

    fun registerOnKill(callable: () -> Unit)

    val valid: AtomicBoolean

    fun spawnParticle(data: V2ParticleData)
}