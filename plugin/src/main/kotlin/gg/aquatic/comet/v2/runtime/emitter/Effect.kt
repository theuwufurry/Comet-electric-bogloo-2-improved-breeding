package gg.aquatic.comet.v2.runtime.emitter

import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.atomic.AtomicBoolean

abstract class Effect : ProxyObject {
    abstract val api: JSEffectAPI
    abstract val pose: Pose

    abstract fun tick()

    abstract fun createParticle(): V2ParticleData

    /**
     * Must be called on same thread as effect
     */
    abstract fun onKill()

    abstract val valid: AtomicBoolean

    abstract fun spawnParticle(data: V2ParticleData)
}