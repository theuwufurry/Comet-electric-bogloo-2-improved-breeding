package gg.aquatic.comet.v2.runtime.virtual

import com.google.gson.JsonElement
import com.ixume.optimization.LocalPacketOptimizer
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.audience.Audience
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.joml.Vector3d
import java.util.UUID
import kotlin.system.measureNanoTime

class VirtualEffect(
    override val uuid: UUID,
    override val relPose: Pose,
    override val runtime: EffectRuntime,
    override val api: JSEffectAPI,
    override var parent: Parent?,
    override val audience: Audience,
    val data: JsonElement,
    val extraData: MutableMap<String, Any?>,
) : Effect {
    private val optimizer = LocalPacketOptimizer()
    private var time = 1
    var status: Status = Status.Other
    val stories = mutableListOf<ParticleStory>()
    val particlesToAdd = mutableListOf<V2ParticleData>()
    private val onKill = mutableListOf<() -> Unit>()

    override var valid = true

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

    fun init() {
        status = Status.Effect(time)
        api.invokeEffectInit(proxy)
        status = Status.Other
    }

    override fun tick() {
        if (!valid) return

        status = Status.Effect(time)

        api.invokeEffectTick(proxy)

        if (proxy.dead) {
            api.invokeEffectDeath(proxy)
            valid = false
            runtime.remove(this)
            status = Status.Other
            return
        }

        status = Status.Other

        for (data in particlesToAdd) {
            val story = ParticleStory(
                startTime = time,
                lightData = data.light,
                billboard = data.billboardConstraints,
                seeThrough = data.seeThrough,
                shadow = data.shadow,
                sensitiveCentering = data.sensitiveCentering,
            )
            story.update(time, data)
            //TODO: chunked generation
            val simulationDuration = measureNanoTime {
                var iterations = 1
                while (iterations < 1_000) {
                    status = Status.Particle(time + iterations)
                    api.invokeParticleTick(data)
                    status = Status.Other

                    story.update(time + iterations, data)

                    if (data.dead) {
                        status = Status.Particle(time + iterations)
                        api.invokeParticleDeath(data)
                        status = Status.Other
                        break
                    }

                    iterations++
                }
            }


            val optimizationDuration = measureNanoTime {
                story.optimize(
                    optimizer = optimizer,
                    settings = api.optimization,
                )
            }

//            println("    simulated in ${simulationDuration.toDuration(DurationUnit.NANOSECONDS)}")
//            println("    optimized in ${optimizationDuration.toDuration(DurationUnit.NANOSECONDS)}")

            stories += story
        }

        particlesToAdd.clear()

        time++
    }

    override fun createParticle(): V2ParticleData {
        val data = V2ParticleData(effect = this)
        data.origin = Vector3d(pose.pos)
        return data
    }

    override fun spawnParticle(data: V2ParticleData) {
        status = Status.Particle(time)
        api.invokeParticleInit(data)
        status = Status.Other

        particlesToAdd += data
    }

    override fun onKill() {
        valid = false
        onKill.forEach { it() }
        onKill.clear()
    }

    override fun registerOnKill(callable: () -> Unit) {
        onKill += callable
    }
}

sealed interface Status {
    @JvmInline
    value class Particle(val time: Int) : Status

    @JvmInline
    value class Effect(val time: Int) : Status
    object Other : Status
}