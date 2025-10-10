package gg.aquatic.comet.v2.runtime.emitter

import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import org.graalvm.polyglot.HostAccess
import java.util.concurrent.atomic.AtomicBoolean

interface Effect {
    val api: JSEffectAPI

    fun tick()

    @HostAccess.Export
    fun createParticle(): V2ParticleData

    /**
     * Must be called on same thread as effect
     */
    fun onKill()
    
    val valid: AtomicBoolean

    fun spawnParticle(data: V2ParticleData)
}