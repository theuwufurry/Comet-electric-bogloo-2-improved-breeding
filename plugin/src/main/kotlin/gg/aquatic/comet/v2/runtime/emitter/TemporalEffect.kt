package gg.aquatic.comet.v2.runtime.emitter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.particle.V2Particle
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.joml.Vector3d
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * No fancy tricks, just executes everything in sequence. Unoptimized.
 */
class TemporalEffect(
    override val relPose: Pose,
    override val runtime: EffectRuntime,
    override val api: JSEffectAPI,
    override var parent: Parent?,
) : Effect {
    val particles = mutableListOf<V2Particle>()
    val particlesToAdd = mutableListOf<V2Particle>()

    private val onKill = CopyOnWriteArrayList<() -> Unit>()

    private val blocked = AtomicBoolean(false)
    override val valid = AtomicBoolean(true)

    override val proxy = V2EffectProxy(this)

    override val pose: Pose
        get() = relPose.let {
            val p = parent
            if (p != null) {
                Pose(
                    world = it.world,
                    pos = Vector3d(it.pos).add(p.pose.pos),
                    rot = it.rot
                )
            } else {
                it
            }
        }

    init {
        blocked.set(true)

        api.invokeEffectInit(proxy)

        blocked.set(false)
    }

    override fun tick() {
        if (!valid.get()) return

        blocked.set(true)

        particlesToAdd.clear()

        api.invokeEffectTick(proxy)

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        val killedIDs = IntArrayList()

        for (particle in particles) {
            val data = particle.data
            api.invokeParticleTick(data)

            if (data.dead) {
                api.invokeParticleDeath(data)
                killedIDs.addAll(particle.entityIDs)
                continue
            }

            dataPackets += particle.update()
            dataPackets += particle.getPositionPacket()
        }

        if (proxy.dead) {
            api.invokeEffectDeath(proxy)
            valid.set(false)
            runtime.remove(this)
            return
        }

        for (particleToAdd in particlesToAdd) {
            dataPackets += particleToAdd.getAddPacket()
            particles += particleToAdd
        }

        val destroyPacket = if (killedIDs.isEmpty()) null else {
            WrapperPlayServerDestroyEntities(*killedIDs.toIntArray())
        }

        for (player in runtime.players) {
            val show =
                api.invokeShowPlayer(player) ?: (player.location.distance(pose.location) <= 128.0) //TODO: Culling!
            if (show) {
                for (packet in dataPackets) {
                    PacketEvents.getAPI().playerManager.sendPacketSilently(player, packet)
                }

                if (destroyPacket != null) {
                    PacketEvents.getAPI().playerManager.sendPacketSilently(player, destroyPacket)
                }
            }
        }

        blocked.set(true)
    }

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
        onKill.forEach { it() }
        onKill.clear()

        val ls = particles.flatMap { it.entityIDs }
        val ids = ls.toIntArray()
        val destroyPacket = WrapperPlayServerDestroyEntities(*ids)
        for (player in runtime.players) {
            PacketEvents.getAPI().playerManager.sendPacketSilently(player, destroyPacket)
        }

        particles.clear()
    }

    override fun registerOnKill(callable: () -> Unit) {
        onKill += callable
    }

}