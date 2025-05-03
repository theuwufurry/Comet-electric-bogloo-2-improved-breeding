package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.*
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
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
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*

class VirtualEmitter(
    val parent: Parent? = null,
    val unrealizedEmitter: AbstractUnrealizedEmitter,
    val runtime: VirtualRuntime,
    val rateComponent: RateComponent,
    components: List<Component>,
    val billboardConstraints: BillboardConstraints,
    backerLocation: Location,
    backerEmitterData: EmitterData,
    backerForwardVector: Vector3d,
    backerEnvironmentData: EnvironmentData,
    seed: Int,
) : AbstractEmitter() {
    constructor(backer: OptimizedEmitter, runtime: VirtualRuntime) : this(
        parent = backer.parent,
        unrealizedEmitter = backer.unrealizedEmitter,
        runtime = runtime,
        rateComponent = backer.rateComponent,
        components = backer.components,
        billboardConstraints = backer.billboardConstraints,
        backerLocation = backer.location.clone(),
        backerEmitterData = backer.emitterData.clone(),
        backerForwardVector = Vector3d(backer.forwardVector),
        backerEnvironmentData = backer.environmentData.clone(),
        seed = backer.random.seed,
    )

    val emitterComponents: List<EmitterComponent> =
        components.filterIsInstance<EmitterComponent>().sortedBy { it.priority }
    val particleComponents: List<ParticleComponent> =
        components.filterIsInstance<ParticleComponent>().sortedBy { it.priority }

    var emitterData: EmitterData = backerEmitterData.clone().apply {
        emitter = this@VirtualEmitter
    }

    override val id: UUID = emitterData.id

    override val particles: MutableList<Particle> = mutableListOf()
    override val location: Location = backerLocation.clone()
    override val forwardVector: Vector3d = backerForwardVector
    override val environmentData: EnvironmentData = backerEnvironmentData.clone()
    override var emitterRotation: Quaterniond = calculateEmitterRotation()

    fun calculateEmitterRotation(): Quaterniond {
        return Quaterniond().rotateTo(forwardVector, pose.dir)
    }

    private val deadParticles: MutableList<Particle> = mutableListOf()

    override val random: DeterministicRandom = DeterministicRandom(seed)

    private val path: CachedPath = CachedPath(
        (environmentData.data["loc_tol"] as? Number)?.toDouble() ?: 0.1,
        (environmentData.data["disp_tol"] as? Number)?.toDouble() ?: 0.2,
        (environmentData.data["col_tol"] as? Number)?.toDouble() ?: 48.0,
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

    override val pose: Pose
        get() {
            return location.pose()
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
            path.emitterData.add(TimestampedEmitterData(0, false, emptyList()))
            path.emitterActions += TimestampedEmitterActions(0, emitterActionsBuffer)
            emitterActionsBuffer = mutableListOf()
        }
    }

    private val savedEmitterData: MutableMap<Int, EmitterData> = mutableMapOf()

    override fun tick(): EmitterTickResult {
        time++
        parent?.pose?.let { setPose(it) }
        emitterRotation = calculateEmitterRotation()
        emitterActionsBuffer = mutableListOf()

        val saved = savedEmitterData[time]
        if (saved != null) {
            emitterData = saved
        } else {
            emitterComponents.forEach { it.execute(emitterData) }
        }

        if (emitterData.dead || (parent != null && parent.dead)) {
            dead = true
            emitterComponents.forEach { it.die(emitterData) }
        }

        if (((runtime.catchupTime != null && time <= runtime.catchupTime!!) || runtime.catchupTime == null) && dead && particles.isEmpty()) {
            path.emitterData += TimestampedEmitterData(time, true, emptyList())
            savedEmitterData.clear()
            return EmitterTickResult(false)
        }

        particleActionsBuffer = mutableMapOf()
        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.emitter?.dead = true
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
                        WrappedPos(particle.data.pos, particle.data.age, timeCoefficient)
                    )
                )
            }

            path.internalTransformableData[particle.data.id]!!.apply {
                add(
                    TimestampedTransformableData(
                        particle.data.age.toInt(),
                        DisplayDataVector.create(particle, coefficients)
                    )
                )
            }

            path.coloredTextureData[particle.data.id]!!.apply {
                add(
                    TimestampedColoredTexture(
                        particle.data.age.toInt(),
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
            path.emitterActions += TimestampedEmitterActions(time, emitterActionsBuffer)
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

        savedEmitterData[time] = emitterData.clone()
        if (((runtime.catchupTime != null && time <= runtime.catchupTime!!) || runtime.catchupTime == null) && !dead && emitterData.isActive) {
            spawnParticles()
        }

        if (runtime.catchupTime != null && time >= runtime.catchupTime!! && particles.isEmpty()) {
            time = runtime.catchupTime!!
        }

        path.optimizeFinished()

        return EmitterTickResult(true)
    }

    private fun spawnParticles() {
        particleActionsBuffer = mutableMapOf()
        val spawns = mutableListOf<UUID>()
        repeat(rateComponent.toEmit(emitterData)) {
            val uuid = random.uuid()
            val particleData = ParticleData(uuid)
            spawns += particleData.id
            val particle = Particle(particleData)
            particleData.particle = particle
            particleData.origin = location.toVector().toVector3d()
            particleData.billboardConstraints = billboardConstraints

            particleComponents.forEach { it.execute(emitterData, particleData) }

            path.hashes[particleData.id] =
                mutableSetOf<Int>().apply { add(particleData.locHash()) } to mutableSetOf<Int>().apply {
                    add(particleData.displayHash())
                }

            path.internalLocations[particleData.id] = mutableListOf<TimestampedPos>().apply {
                add(TimestampedPos(0, WrappedPos(particleData.pos, 0.0, timeCoefficient)))
            }

            path.internalTransformableData[particleData.id] = mutableListOf<TimestampedTransformableData>().apply {
                add(TimestampedTransformableData(0, DisplayDataVector.create(particle, coefficients)))
            }

            path.coloredTextureData[particleData.id] = mutableListOf<TimestampedColoredTexture>().apply {
                add(
                    TimestampedColoredTexture(
                        0,
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

        path.emitterData.add(TimestampedEmitterData(time, false, spawns))
        for ((id, action) in particleActionsBuffer) {
            if (action.actions.isNotEmpty()) {
                path.particleActions[id] = mutableListOf(action)
            }
        }
    }

    override val players: List<Player> = emptyList()

    override fun setPose(pose: Pose) {
        location.x = pose.pos.x
        location.y = pose.pos.y
        location.z = pose.pos.z

        location.direction = Vector(
            pose.dir.x,
            pose.dir.y,
            pose.dir.z
        )
    }

    override fun kill() {
        savedEmitterData.clear()
        dead = true
        particles.clear()
    }

    override fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(input) else input
    }

    override fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID
    ) {
        (unrealizedEmitter as UnrealizedEmitter).virtualRealize(
            parent,
            location,
            environmentData,
            random,
            runtime,
            uuid
        )
    }
}

fun Int.alpha(): Double = ((this ushr 24) and 0xFF) / 255.0