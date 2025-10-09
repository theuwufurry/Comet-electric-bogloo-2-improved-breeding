package gg.aquatic.comet.emitter.optimization

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.Mount
import gg.aquatic.comet.api.emitter.*
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.impl.OptimizedEmitter
import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVector
import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVectorCoefficients
import gg.aquatic.comet.emitter.optimization.vec.WrappedPos
import gg.aquatic.comet.particle.Particle
import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.entity.Player
import org.joml.Quaterniond
import org.joml.Quaternionf
import java.util.*
import java.util.function.Supplier

class VirtualEmitter(
    val parent: Parent? = null,
    override val unrealizedEmitter: AbstractUnrealizedEmitter,
    val runtime: VirtualRuntime,
    val rateComponent: RateComponent,
    components: List<Component>,
    val billboardConstraints: BillboardConstraints,
    backerPose: Pose,
    backerEmitterData: EmitterData,
    private val backerEnvironmentData: EnvironmentData,
    seed: Int,
    private val timeOffset: Int,
    override val mount: Mount?,
    override val yawpitchSupplier: Supplier<YawPitch>?,
) : AbstractEmitter() {
    constructor(backer: OptimizedEmitter, runtime: VirtualRuntime) : this(
        parent = backer.parent,
        unrealizedEmitter = backer.unrealizedEmitter,
        runtime = runtime,
        rateComponent = backer.rateComponent,
        components = backer.components,
        billboardConstraints = backer.billboardConstraints,
        backerPose = backer.pose,
        backerEmitterData = backer.emitterData.clone(),
        backerEnvironmentData = backer.environmentData.clone(),
        seed = backer.random.seed,
        timeOffset = 0,
        mount = backer.mount,
        yawpitchSupplier = backer.yawpitchSupplier,
    )

    val emitterComponents: List<EmitterComponent> =
        components.filterIsInstance<EmitterComponent>().sortedBy { it.priority }
    val particleComponents: List<ParticleComponent> =
        components.filterIsInstance<ParticleComponent>().sortedBy { it.priority }

    var emitterData: EmitterData = backerEmitterData.clone().apply {
        emitter = this@VirtualEmitter
    }

    override val id: UUID = emitterData.id

    var absoluteTime = timeOffset
        private set

    override val particles: MutableList<Particle> = mutableListOf()
    override var pose: Pose = backerPose.clone()
    override val environmentData: EnvironmentData
        get() = EnvironmentData(backerEnvironmentData.size, emitterData.variable as VariableMutableMap)

    private val deadParticles: MutableList<Particle> = mutableListOf()

    override val random: DeterministicRandom = DeterministicRandom(seed)

    private val path: CachedPath = CachedPath(
        (environmentData.data["loc_tol"] as? Number)?.toDouble() ?: 0.2,
        (environmentData.data["scale_tol"] as? Number)?.toDouble() ?: 0.1,
        (environmentData.data["rot_tol"] as? Number)?.toDouble() ?: 0.1,
        (environmentData.data["col_tol"] as? Number)?.toDouble() ?: 24.0,
        (environmentData.data["opacity_tol"] as? Number)?.toDouble() ?: 24.0,
        (environmentData.data["c_coltex"] as? Boolean) ?: true,
    )

    private val coefficients = DisplayDataVectorCoefficients(
        (environmentData.data["c_alpha"] as? Number)?.toDouble() ?: 1.0,
        (environmentData.data["c_rot"] as? Number)?.toFloat() ?: 1f,
        (environmentData.data["c_scale"] as? Number)?.toFloat() ?: 1f,
        (environmentData.data["c_tr"] as? Number)?.toFloat() ?: 1f,
        (environmentData.data["c_time"] as? Number)?.toDouble() ?: 0.1,
    )

    private val timeCoefficient = (environmentData.data["c_time"] as? Number)?.toDouble() ?: 0.1

    override val audience: AquaticAudience = object : AquaticAudience {
        override val uuids: Collection<UUID> = emptyList()

        override fun canBeApplied(player: Player): Boolean {
            return false
        }

    }

    override val isPregen: Boolean = true
    private var time = 0

    var emitterActionsBuffer = mutableListOf<(AbstractEmitter) -> Unit>()

    /**
     * Particle UUID -> Actions
     */
    var particleActionsBuffer = mutableMapOf<UUID, TimestampedParticleActions>()

    init {
        GlobalTicker.emitterCache[emitterData.id] = path
        emitterComponents.forEach { it.init(emitterData) }

        if (emitterActionsBuffer.isNotEmpty()) {
            path.emitterData.add(TimestampedEmitterData(0, absoluteTime, false, emptyList()))
            path.emitterActions += TimestampedEmitterActions(0, absoluteTime, emitterActionsBuffer)
            emitterActionsBuffer = mutableListOf()
        }
    }

    private val savedEmitterData: MutableMap<Int, EmitterData> = mutableMapOf()

    /*
    time staying at catchup time after caught up
        when parent dies, time is recorded as death, which is inaccurate since we're at catchup time
        really when we're done catching up, don't register anything?
            we can handle those changes on next catchup
     */

    override fun tick(): EmitterTickResult {
//        println("V.${unrealizedEmitter.id}.TICKED!!")
        /*
        process until catchuptime (inclusive)
        if time is already equal to catchup time, then we've already processed that tick
         */

        if (runtime.catchupTime != null && absoluteTime >= runtime.catchupTime!! && particles.isEmpty()) {
//            println("V.${unrealizedEmitter.id} DONE at:$absoluteTime")
            return EmitterTickResult(true)
        }

        time++
        absoluteTime++

//        println("V.${unrealizedEmitter.id}, TICK t:$time")
//        parent?.pose?.let { setPose(it) }

        if (parent != null) {
            if (parent is Particle) {
                val locs = (parent.data.emitter!! as VirtualEmitter).path.internalLocations[parent.data.id]
                if (locs != null) {
                    val matchingLoc = locs.firstOrNull { it.absoluteTime == absoluteTime }
//                    println("V.${unrealizedEmitter.id}, MATCHING PARENT LOC")
                    if (matchingLoc != null) {
                        pose = Pose(
                            pose.world,
                            matchingLoc.vec.vec,
                            Quaterniond()
                        )
                    }
                }
            } else {
                pose = parent.pose
            }
        }

        emitterActionsBuffer = mutableListOf()

        val saved = savedEmitterData[absoluteTime]
        if (saved != null) {
            emitterData = saved
        } else {
            emitterComponents.forEach { it.execute(emitterData) }
//            println("V.${unrealizedEmitter.id}, NEW TICK t:$time at:$absoluteTime")
        }

        if (parent != null && parent.dead.get()) {
            if (parent is Particle) {
                val lastAbsoluteTime =
                    (parent.data.emitter!! as VirtualEmitter).path.locations[parent.data.id]!!.last().absoluteTime
//                println("V.${unrealizedEmitter.id}, LAST PARENT TIME: $lastAbsoluteTime")
                if (lastAbsoluteTime <= absoluteTime) {
//                    println("V.${unrealizedEmitter.id}, PARENT DEAD t:$time at:$absoluteTime")
                    dead.set(true)
                    emitterComponents.forEach { it.die(emitterData) }
                } else {
                    dead.set(false)
                }
            } else if (parent is VirtualEmitter) {
                if (parent.path.emitterData.isNotEmpty()) {
                    if (parent.path.emitterData.last().dead && parent.path.emitterData.last().absoluteTime <= absoluteTime) {
//                        println("V.${unrealizedEmitter.id}, PARENT DEAD t:$time at:$absoluteTime")
                        dead.set(true)
                        emitterComponents.forEach { it.die(emitterData) }
                    }
                }
            } else {
//                println("V.${unrealizedEmitter.id}, PARENT DEAD t:$time at:$absoluteTime")
                dead.set(true)
                emitterComponents.forEach { it.die(emitterData) }
            }
        } else {
            if (emitterData.dead) {
//                println("V.${unrealizedEmitter.id}, DATA DEAD t:$time at:$absoluteTime")

                dead.set(true)
                emitterComponents.forEach { it.die(emitterData) }
            } else {
                dead.set(false)
            }
        }

        particleActionsBuffer = mutableMapOf()
        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.dead = true
                path.finishedParticles += particle.data.id
            }

            particle.tick()

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            path.hashes[particle.data.id]!!.apply {
                first += particle.data.locHash()
                second += particle.data.displayHash()
            }

            path.internalLocations[particle.data.id]!!.apply {
                add(
                    TimestampedPos(
                        particle.data.age.toInt(),
                        absoluteTime,
                        WrappedPos(particle.data.pos, particle.data.age, timeCoefficient)
                    )
                )
            }

            path.internalTransformableData[particle.data.id]!!.apply {
                add(
                    TimestampedTransformableData(
                        particle.data.age.toInt(),
                        absoluteTime,
                        DisplayDataVector.create(particle, coefficients)
                    )
                )
            }

            path.coloredTextureData[particle.data.id]!!.apply {
                add(
                    TimestampedColoredTexture(
                        particle.data.age.toInt(),
                        absoluteTime,
                        (particle.data.color ushr 16) and 0xFF,
                        (particle.data.color ushr 8) and 0xFF,
                        particle.data.color and 0xFF,
                        particle.data.displayData

                    )
                )
            }

            if (particle.data.dead) {
                particleComponents.forEach { it.die(emitterData, particle.data) }
                die()
                continue
            }
        }

        if (emitterActionsBuffer.isNotEmpty()) {
            path.emitterActions += TimestampedEmitterActions(time, absoluteTime, emitterActionsBuffer)
            emitterActionsBuffer = mutableListOf()
        }

        for ((id, action) in particleActionsBuffer) {
            if (action.actions.isNotEmpty()) {
                (path.particleActions[id] ?: run {
                    val a = mutableListOf<TimestampedParticleActions>()
                    path.particleActions[id] = a
                    a
                }) += action
            }
        }

        particles.removeAll(deadParticles)

        deadParticles.clear()

        savedEmitterData[absoluteTime] = emitterData.clone()
        if (((runtime.catchupTime != null && absoluteTime <= runtime.catchupTime!!) || runtime.catchupTime == null) && !dead.get() && emitterData.isActive) {
            spawnParticles()
        }

//        println("V.${unrealizedEmitter.id}, PREKILLATTEMPT, catchup: ${runtime.catchupTime}, dead:$dead particles:${particles.isEmpty()}, at:$absoluteTime")
        if (((runtime.catchupTime != null && absoluteTime <= runtime.catchupTime!!) || runtime.catchupTime == null) && dead.get() && particles.isEmpty()) {
//            println("V.${unrealizedEmitter.id}, KILLING t:$time at:$absoluteTime")
            path.emitterData += TimestampedEmitterData(time, absoluteTime, true, emptyList())
            savedEmitterData.clear()
            return EmitterTickResult(false)
        }

        if (runtime.catchupTime != null && absoluteTime > runtime.catchupTime!! && particles.isEmpty()) {
//            println("V.${unrealizedEmitter.id} CAUGHT UP t:$time at:$absoluteTime")
            time = runtime.catchupTime!! - timeOffset
            absoluteTime = runtime.catchupTime!!
        }

        path.optimizeFinished(emitterData.optimizationInterval)

        return EmitterTickResult(true)
    }

    private fun spawnParticles() {
//        println("|V.${unrealizedEmitter.id} SPAWNS t:$time at:$absoluteTime")
//        println("  - pose.pos: ${pose.pos}")
        particleActionsBuffer = mutableMapOf()
        val spawns = mutableListOf<UUID>()
        repeat(rateComponent.toEmit(emitterData)) {
            val uuid = random.uuid()
            val particleData = ParticleData(uuid)
            spawns += particleData.id
            val particle = Particle(particleData)
            particleData.emitter = this
            particleData.particle = particle
            particleData.origin = pose.pos
            particleData.billboardConstraints = billboardConstraints

            particleComponents.forEach { it.execute(emitterData, particleData) }

            path.hashes[particleData.id] =
                mutableSetOf<Int>().apply { add(particleData.locHash()) } to mutableSetOf<Int>().apply {
                    add(particleData.displayHash())
                }

            path.internalLocations[particleData.id] = mutableListOf<TimestampedPos>().apply {
                add(TimestampedPos(0, absoluteTime, WrappedPos(particleData.pos, 0.0, timeCoefficient)))
            }

            path.internalTransformableData[particleData.id] = mutableListOf<TimestampedTransformableData>().apply {
                add(TimestampedTransformableData(0, absoluteTime, DisplayDataVector.create(particle, coefficients)))
            }

            path.coloredTextureData[particleData.id] = mutableListOf<TimestampedColoredTexture>().apply {
                add(
                    TimestampedColoredTexture(
                        0,
                        absoluteTime,
                        (particle.data.color ushr 16) and 0xFF,
                        (particle.data.color ushr 8) and 0xFF,
                        particle.data.color and 0xFF,
                        particle.data.displayData
                    )
                )
            }

            particle.init()

            particles += particle
        }

        path.emitterData.add(TimestampedEmitterData(time, absoluteTime, false, spawns))
        for ((id, action) in particleActionsBuffer) {
            if (action.actions.isNotEmpty()) {
                path.particleActions[id] = mutableListOf(action)
            }
        }
    }

    override val players: List<Player> = emptyList()

    fun addParticleAction(data: ParticleData, action: (AbstractEmitter, AbstractParticle) -> Unit) {
        (particleActionsBuffer[data.id] ?: run {
                        val a = TimestampedParticleActions(
                            data.age.toInt(),
                            absoluteTime,
                            mutableListOf()
                        )
                        particleActionsBuffer[data.id] = a
                        a
                    }).actions += action
    }

    override fun kill() {
        dead.set(true)
        GlobalTicker._registerEmitterRemoval(this)
    }

    override fun onKill() {
        savedEmitterData.clear()
        particles.clear()
    }

    override fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(
            pose.rot.x,
            pose.rot.y,
            pose.rot.z,
            pose.rot.w
        ).mul(input) else input
    }

    override fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID,
    ) {
        (unrealizedEmitter as UnrealizedEmitter).virtualRealize(
            parent,
            pose,
            environmentData.clone(),
            random,
            runtime,
            uuid,
            absoluteTime,
            mount,
            yawpitchSupplier
        )
    }

    override fun getSpawnPackets(): List<PacketWrapper<*>> {
        return emptyList()
    }
}

fun Int.alpha(): Double = ((this ushr 24) and 0xFF) / 255.0