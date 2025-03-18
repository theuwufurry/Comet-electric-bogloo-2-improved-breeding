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
import gg.aquatic.comet.emitter.Emitter
import gg.aquatic.comet.emitter.UnrealizedEmitter
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
    constructor(backer: Emitter, runtime: VirtualRuntime) : this(
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

    val emitterData: EmitterData = backerEmitterData.clone().apply {
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

    private val path: CachedPath = CachedPath()
//    private val cache: MutableMap<UUID, Pair<MutableSet<Int>, MutableSet<Int>>> = mutableMapOf()
//    private val positionCache: MutableMap<UUID, MutableList<TimestampedVector3d>> = mutableMapOf()

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

    init {
        emitterComponents.forEach { it.init(emitterData) }
    }

    private fun spawnParticles() {
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData(random.uuid())
            val particle = Particle(particleData)
            particleData.particle = particle
            particleData.origin = location.toVector().toVector3d()
            particleData.billboardConstraints = billboardConstraints

            particleComponents.forEach { it.execute(emitterData, particleData) }

            path.hashes[particleData.id] =
                mutableSetOf<Int>().apply { add(particleData.locHash()) } to mutableSetOf<Int>().apply {
                    add(particleData.displayHash())
                }

            path.locations[particleData.id] = mutableListOf<TimestampedVector3d>().apply {
                add(TimestampedVector3d(0, particleData.pos))
            }

            particle.init()

            particles += particle
        }
    }

    override fun tick(): EmitterTickResult {
        parent?.pose?.let { setPose(it) }
        emitterRotation = calculateEmitterRotation()
        emitterComponents.forEach { it.execute(emitterData) }

        if (emitterData.dead || (parent != null && parent.dead)) {
            dead = true
            emitterComponents.forEach { it.die(emitterData) }
        }

        if (dead && particles.size == 0) {
            return EmitterTickResult(false)
        }

        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.emitter?.dead = true
            }

            particle.tick()

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            path.hashes[particle.data.id]!!.apply {
                first += particle.data.locHash()
                second += particle.data.displayHash()
            }

            path.locations[particle.data.id]!!.apply {
                add(TimestampedVector3d(particle.data.age.toInt(), particle.data.pos))
            }

            if (particle.data.dead) {
                particleComponents.forEach { it.die(emitterData, particle.data) }
                die()
                continue
            }
        }

        particles.removeAll(deadParticles)

        deadParticles.clear()

        if (!dead && emitterData.isActive) spawnParticles()

        return EmitterTickResult(true)
    }

    fun cachedPath(): CachedPath {
        return path.optimized()
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
        random: DeterministicRandom
    ) {
        (unrealizedEmitter as UnrealizedEmitter).virtualRealize(
            parent,
            location,
            environmentData,
            random,
            runtime,
        )
    }
}