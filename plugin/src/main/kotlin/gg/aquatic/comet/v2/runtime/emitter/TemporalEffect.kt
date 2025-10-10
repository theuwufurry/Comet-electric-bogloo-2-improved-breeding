package gg.aquatic.comet.v2.runtime.emitter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EmitterRuntime
import gg.aquatic.comet.v2.runtime.particle.V2Particle
import org.graalvm.polyglot.HostAccess
import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicBoolean

/**
 * No fancy tricks, just executes everything in sequence. Unoptimized.
 */
class TemporalEffect(
    val pose: Pose,
    val runtime: EmitterRuntime,
    override val api: JSEffectAPI,
) : Effect {
    val particles = mutableListOf<V2Particle>()
    val particlesToAdd = mutableListOf<V2Particle>()

    private val blocked = AtomicBoolean(false)
    override val valid = AtomicBoolean(true)

    init {
        blocked.set(true)

        blocked.set(false)
    }

    override fun tick() {
        blocked.set(true)

        particlesToAdd.clear()

        api.invokeEmitterTick(this as Effect)

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
            val show = api.invokeShowPlayer(player) ?: (player.location.distance(pose.location) <= 32.0) //TODO: Culling!
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
        particles.clear()
    }
}