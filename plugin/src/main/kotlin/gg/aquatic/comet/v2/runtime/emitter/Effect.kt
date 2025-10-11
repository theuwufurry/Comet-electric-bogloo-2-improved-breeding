package gg.aquatic.comet.v2.runtime.emitter

import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.atomic.AtomicBoolean

interface Effect : ProxyObject {
    val api: JSEffectAPI
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