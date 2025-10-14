package gg.aquatic.comet.v2.runtime.emitter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.V2AudienceProcessor
import gg.aquatic.comet.v2.runtime.audience.Audience
import gg.aquatic.comet.v2.runtime.particle.RealParticle
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.joml.Vector3d
import java.util.UUID
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
    override val audience: Audience,
    val data: JsonElement,
    extraData: MutableMap<String, Any?>,
) : Effect {
    override val uuid: UUID = UUID.randomUUID()
    val particles = mutableListOf<RealParticle>()
    val particlesToAdd = mutableListOf<RealParticle>()

    private val onKill = CopyOnWriteArrayList<() -> Unit>()

    private val blocked = AtomicBoolean(false)
    private val validAtomic = AtomicBoolean(true)
    override var valid: Boolean
        get() = validAtomic.get()
        set(value) {
            validAtomic.set(value)
        }

    override val proxy = V2EffectProxy(this, data, extraData)

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

    private val audienceProcessor = V2AudienceProcessor(this)

    override fun tick() {
        if (!valid) return

        if (!blocked.compareAndSet(false, true)) return


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
            valid = false
            runtime.remove(this)
            audienceProcessor.kill(particles)
            return
        }

        for (particleToAdd in particlesToAdd) {
            dataPackets += particleToAdd.getAddPacket()
            particles += particleToAdd
        }

        particlesToAdd.clear()

        audienceProcessor.update(
            currentParticles = particles,
            dataPackets = dataPackets,
            deadIDs = killedIDs,
        )

        blocked.set(false)
    }

    override fun createParticle(): V2ParticleData {
        val data = V2ParticleData(effect = this)
        data.origin = Vector3d(pose.pos)
        return data
    }

    override fun spawnParticle(data: V2ParticleData) {
        val particle = RealParticle(data)

        api.invokeParticleInit(data)
        particle.init()

        particlesToAdd += particle
    }

    override fun onKill() {
        valid = false
        onKill.forEach { it() }
        onKill.clear()
        audienceProcessor.kill(particles)
        particles.clear()
    }

    override fun registerOnKill(callable: () -> Unit) {
        onKill += callable
    }
}