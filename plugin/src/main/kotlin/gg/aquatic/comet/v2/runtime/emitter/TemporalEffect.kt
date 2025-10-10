package gg.aquatic.comet.v2.runtime.emitter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EmitterRuntime
import gg.aquatic.comet.v2.runtime.particle.V2Particle
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicBoolean

/**
 * No fancy tricks, just executes everything in sequence. Unoptimized.
 */
class TemporalEffect(
    override val pose: Pose,
    val runtime: EmitterRuntime,
    override val api: JSEffectAPI,
) : Effect() {
    val particles = mutableListOf<V2Particle>()
    val particlesToAdd = mutableListOf<V2Particle>()

    private val blocked = AtomicBoolean(false)
    override val valid = AtomicBoolean(true)

    init {
        blocked.set(true)

        api.invokeEffectInit(this)

        blocked.set(false)
    }

    override fun tick() {
        blocked.set(true)

        particlesToAdd.clear()

        api.invokeEffectTick(this)

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        for (particle in particles) {
            val data = particle.data
            api.invokeParticleTick(data)

            dataPackets += particle.update()
            dataPackets += particle.getPositionPacket()
        }

        for (particleToAdd in particlesToAdd) {
            dataPackets += particleToAdd.getAddPacket()
            particles += particleToAdd
        }

        for (player in runtime.players) {
            val show =
                api.invokeShowPlayer(player) ?: (player.location.distance(pose.location) <= 32.0) //TODO: Culling!
            if (show) {
                for (packet in dataPackets) {
                    PacketEvents.getAPI().playerManager.sendPacketSilently(player, packet)
                }
            }
        }

        blocked.set(true)
    }

    @HostAccess.Export
    override fun createParticle(): V2ParticleData {
        val data = V2ParticleData(effect = this)
        data.origin = Vector3d(pose.pos)
        return data
    }

    override fun spawnParticle(data: V2ParticleData) {
        val particle = V2Particle(data)

        api.invokeParticleInit(data)
        particle.init()

        particlesToAdd += particle
    }

    override fun onKill() {
        valid.set(false)

        val ls = particles.flatMap { it.entityIDs }
        val ids = ls.toIntArray()
        val destroyPacket = WrapperPlayServerDestroyEntities(*ids)
        for (player in runtime.players) {
            PacketEvents.getAPI().playerManager.sendPacketSilently(player, destroyPacket)
        }
        
        particles.clear()
    }

    private val members = arrayOf("pos", "rot", "runtime", "createParticle")
    private val createParticleCallable = ProxyExecutable { createParticle() }

    override fun getMember(key: String?): Any? {
        return when (key) {
            "pos" -> pose.pos
            "rot" -> pose.rot
            "runtime" -> runtime
            "createParticle" -> createParticleCallable
            else -> null
        }
    }

    override fun getMemberKeys(): Any? {
        return members
    }

    override fun hasMember(key: String?): Boolean {
        return key == "pos" ||
               key == "rot" ||
               key == "runtime" ||
               key == "createParticle"

    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException()
    }
}