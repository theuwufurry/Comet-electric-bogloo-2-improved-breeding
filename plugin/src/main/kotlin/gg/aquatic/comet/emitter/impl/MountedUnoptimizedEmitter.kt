package gg.aquatic.comet.emitter.impl

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.Mount
import gg.aquatic.comet.api.emitter.*
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.emitter.SpawningProcessor
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.Color
import org.bukkit.entity.Player
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.random.Random

/**
 * TODO: Handle restrictions for mounted emitters and share remaining logic between this and regular emitters
 */
class MountedUnoptimizedEmitter(
    val parent: Parent? = null,
    val components: List<Component>,
    val rateComponent: RateComponent,
    distanceCullingComponent: DistanceCullingComponent,
    private val updateFrequencyComponent: UpdateFrequencyComponent,
    val billboardConstraints: BillboardConstraints,
    override var pose: Pose,
    val emitterData: EmitterData,
    override val unrealizedEmitter: AbstractUnrealizedEmitter,
    override val environmentData: EnvironmentData,
    override val audience: AquaticAudience,
    val seed: Int = Random.nextInt(),
    override val mount: Mount,
    override val yawpitchSupplier: Supplier<YawPitch>?,
) : AbstractEmitter() {
    private val dustOptions =
        org.bukkit.Particle.DustOptions(
            Color.fromRGB(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255)),
            0.5f
        )

    override val id: UUID = emitterData.id
    val emitterComponents: List<EmitterComponent> =
        components.filterIsInstance<EmitterComponent>().sortedBy { it.priority }
    val particleComponents: List<ParticleComponent> =
        components.filterIsInstance<ParticleComponent>().sortedBy { it.priority }

    override val random: DeterministicRandom = DeterministicRandom(seed)

    //origin can change, rotation can change
    override val particles: MutableList<Particle> = mutableListOf()
    private val spawningProcessor = SpawningProcessor(this, distanceCullingComponent)
    private val blocked = AtomicBoolean(false)

    init {
        blocked.set(true)

        emitterData.emitter = this
        emitterComponents.forEach { it.init(emitterData) }

        blocked.set(false)
    }

    private var locMisses = 0
    private var displayMisses = 0
    private var particleMisses = 0
    private var emitterMisses = 0

    override fun tick(): EmitterTickResult {
        if (!blocked.compareAndSet(false, true)) {
            println("${unrealizedEmitter.id} blocked!")
            return EmitterTickResult(true)
        }

        spawningProcessor.tick()

        parent?.pose?.let { pose = it }
        emitterComponents.forEach { it.execute(emitterData) }

        if (emitterData.dead || (parent != null && parent.dead.get())) {
            dead.set(true)
            emitterComponents.forEach { it.die(emitterData) }
        }

        if (dead.get() && particles.isEmpty()) {
            if (locMisses > 0 || particleMisses > 0 || emitterMisses > 0 || displayMisses > 0) {
                println(
                    """
                -- ${unrealizedEmitter.id} --
                $locMisses loc misses
                $displayMisses display misses
                $particleMisses particle misses
                $emitterMisses emitter misses
            """.trimIndent()
                )
            }

            blocked.set(false)
            return EmitterTickResult(false)
        }

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        for (particle in particles) {
            fun die() {
                spawningProcessor.die(particle)
                particle.data.dead = true
            }

            particle.tick()

            val shouldUpdate = updateFrequencyComponent.shouldSendUpdate(emitterData, particle.data)
            shouldUpdate.interpolationDuration?.let { particle.data.transformationInterpolationDuration = it }

            val initialPos = Vector3d(particle.data.origin).add(particle.data.relativePosition)

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            if (initialPos != particle.data.relativePosition) {
                if (shouldUpdate.shouldUpdate) {
                    particle.getMovementPacket().let { dataPackets += it }
                }
            }

            dataPackets += particle.updatePackets(
                entityDataBuilder = EntityDataBuilder,
                shouldUpdate = shouldUpdate.shouldUpdate,
                flagOverride = null
            )
//            ).let { dataPackets += it }

            if (particle.data.dead) {
                particleComponents.forEach { it.die(emitterData, particle.data) }
                die()
                continue
            }
        }

        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = spawningProcessor.process(dataPackets)

        if (!dead.get() && emitterData.isActive) spawnParticles()

        blocked.set(false)

        return EmitterTickResult(true, deadParticleIDs)
    }

    private fun spawnParticles() {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData(random.uuid())
            val particle = Particle(particleData)
            particleData.emitter = this
            particleData.particle = particle
            particleData.origin = pose.pos
            particleData.billboardConstraints = billboardConstraints
            particleData.interpolationDelay = updateFrequencyComponent.interpolationDelay
            particleData.transformationInterpolationDuration = updateFrequencyComponent.initialInterpolationDuration

            particleComponents.forEach { it.execute(emitterData, particleData) }

            particle.init()

            fun end(d: ParticleData = particle.data) {
                val packets = particle.getAddPacket(d)
                bundle.addAll(packets)

                particles += particle
            }

            end()
            return@repeat
        }

        spawningProcessor.sendSpawns(bundle)
    }

    override val players: List<Player>
        get() = spawningProcessor.players

    override val isPregen: Boolean = false

    override fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID
    ) {
        unrealizedEmitter.internalRealize(
            parent,
            pose,
            environmentData,
            audience,
            random,
            uuid,
            mount,
            yawpitchSupplier
        )
    }

    override fun getSpawnPackets(): List<PacketWrapper<*>> {
        return particles.flatMap { it.getAddPacket() }
    }

    override fun kill() {
        dead.set(true)
        GlobalTicker._registerEmitterRemoval(this)
    }

    override fun onKill() {
        spawningProcessor.killParticles(particles)
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
}