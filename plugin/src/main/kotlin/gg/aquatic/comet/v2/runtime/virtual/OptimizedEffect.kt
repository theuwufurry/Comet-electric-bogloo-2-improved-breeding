package gg.aquatic.comet.v2.runtime.virtual

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.V2AudienceProcessor
import gg.aquatic.comet.v2.runtime.audience.Audience
import gg.aquatic.comet.v2.runtime.emitter.Effect
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs an optimized effect via the following procedure
 * - Creates virtual runtime
 * - Run a virtual effect inside that runtime
 *   - Tick the effect in sync with optimized effect (this)
 *   - Simulate particles fully at conception
 *     - This means particles can't depend on emitter/world state past their conception
 *     - Particles submit runnables to runtime to execute real world actions
 *       - Runtime keeps track of the time for that particle, and stores that until it's actually that time
 *       - This means we need to keep track of time since effect conception
 *   - Store data and optimize
 *     - Store particle stories in queue
 */
class OptimizedEffect(
    override val relPose: Pose,
    override val runtime: EffectRuntime,
    override val api: JSEffectAPI,
    override var parent: Parent?,
    override val audience: Audience,
    val data: JsonElement,
) : Effect {
    override val uuid: UUID = UUID.randomUUID()
    private val blocked = AtomicBoolean(false)
    private var time = 0
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

    override var valid: Boolean
        get() = validAtomic.get()
        set(value) {
            validAtomic.set(value)
        }

    private val virtualRuntime = VirtualRuntime(uuid, relPose, api, parent, audience, data)
    private val ongoingStories = mutableListOf<ParticleStory>()

    private val audienceProcessor = V2AudienceProcessor(this)

    override fun tick() {
        if (!blocked.compareAndSet(false, true)) return

        virtualRuntime.tick()
        val dead = virtualRuntime.effect.proxy.dead

        val executables = virtualRuntime.timestampedExecutables.remove(time)
        if (executables != null) {
            for (executable in executables) {
                runtime.submitExecutable(BoundExecutableWrapper(this, executable))
            }
        }

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()
        val killedIDs = IntArrayList()

        val newStories = virtualRuntime.effect.stories
        for (story in newStories) {
            dataPackets += story.initializeEntity()
        }

        val storiesToRemove = mutableListOf<ParticleStory>()

        for (story in ongoingStories) {
            val result = story.updateEntity(time)
            when (result) {
                is ParticleStory.EntityUpdateResult.Update -> dataPackets += result.packets
                is ParticleStory.EntityUpdateResult.Dead -> {
                    killedIDs.add(story.id)
                    storiesToRemove += story
                }
            }
        }

        ongoingStories -= storiesToRemove
        ongoingStories += newStories
        newStories.clear()

        audienceProcessor.update(
            currentParticles = ongoingStories,
            dataPackets = dataPackets,
            deadIDs = killedIDs,
        )

        if (dead) {
            valid = false
            runtime.remove(this)
            audienceProcessor.kill(ongoingStories)
            ongoingStories.clear()
        }

        time++

        blocked.set(false)
    }

    override fun onKill() {
        valid = false
        virtualRuntime.onKill()
        audienceProcessor.kill(ongoingStories)
        ongoingStories.clear()
    }

    private val validAtomic = AtomicBoolean(true)

    override fun createParticle(): V2ParticleData {
        return virtualRuntime.effect.createParticle()
    }

    override fun registerOnKill(callable: () -> Unit) {
        virtualRuntime.effect.registerOnKill(callable)
    }

    override fun spawnParticle(data: V2ParticleData) {
        virtualRuntime.effect.spawnParticle(data)
    }

    override val proxy: V2EffectProxy
        get() = virtualRuntime.effect.proxy
}