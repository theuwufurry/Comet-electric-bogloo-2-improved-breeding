package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.*
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.chunk.trackedByPlayers
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.toUser
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet
import kotlin.random.Random
import kotlin.system.measureNanoTime

/*
components intertwine w/ eachother, so need to do full simulation
    components provide path in pre-realization
    path is optimized, but original data is kept
    after realization
        component still has access to cached data, basically the same run-through happens
                store hash?
            other components need to know if there's a divergence, so need to do true run-through

    tracked things:
        position (tele. packets)
        display data
            scale
            color
            these deal w/ same interpolation duration so their updates must come at the same time or at multiples of interpolation duration
    emitter uses , to see if path deviations has happened
        hash for position
        hash for display
    components hash all available data
        keep available hashes PER particle id; if any hash isn't contained, then we have fucked up

    location dependency?
        no - can pregenerate effects no problemo
        yes - {
            pregenerating takes some time, Tp
            tick = Td

            each tich, pregenerate AT MINIMUM particles for next tick, keep generating until tick ends
        }
    must also tick emitter components
        should generate all at once
        fuck
            particle id's must be deterministic
                all components must be deterministic
            starting conditions are the same for emitters
        during this time all subemitters must also be instant emitters
            don't want to make every single component have to handle it separately
            emitter.subEmitter method?
    data stored for each UnrealizedEmitter, map from emitter id to cached data
        when emitter dies, clear that cache
    subemitters cannot be treated the same;
        report all the caches once everyone is done, but we need to basically spawn another ticker
    launch ticker on external emitter spawn
        subemitters get added to stack
 */

class Emitter(
    val parent: Parent? = null,
    val components: List<Component>,
    val rateComponent: RateComponent,
    private val distanceCullingComponent: DistanceCullingComponent,
    private val updateFrequencyComponent: UpdateFrequencyComponent,
    val billboardConstraints: BillboardConstraints,
    location: Location,
    val emitterData: EmitterData,
    val unrealizedEmitter: AbstractUnrealizedEmitter,
    override val forwardVector: Vector3d,
    override val environmentData: EnvironmentData,
    override val audience: AquaticAudience,
    internal: Boolean,
    val seed: Int = Random.nextInt(),
) : AbstractEmitter() {
    override val id: UUID = emitterData.id
    val emitterComponents: List<EmitterComponent> =
        components.filterIsInstance<EmitterComponent>().sortedBy { it.priority }
    val particleComponents: List<ParticleComponent> =
        components.filterIsInstance<ParticleComponent>().sortedBy { it.priority }

    override val random: DeterministicRandom = DeterministicRandom(seed)

    override var location = location
        private set

    //origin can change, rotation can change
    override val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    override var emitterRotation: Quaterniond = calculateEmitterRotation()

    fun calculateEmitterRotation(): Quaterniond {
        return Quaterniond().rotateTo(forwardVector, pose.dir)
    }

    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()

    init {
        emitterData.emitter = this

        if (!internal) {
            VirtualRuntime(this).generateCaches()
        }

        emitterComponents.forEach { it.init(emitterData) }
    }

    private var locMisses = 0
    private var particleMisses = 0
    private var emitterMisses = 0

    override fun tick(): EmitterTickResult {
        if (blocked) {
            return EmitterTickResult(true)
        }

        blocked = true

        parent?.pose?.let { setPose(it) }
        emitterRotation = calculateEmitterRotation()
        emitterComponents.forEach { it.execute(emitterData) }

        if (emitterData.dead || (parent != null && parent.dead)) {
            dead = true
            emitterComponents.forEach { it.die(emitterData) }
        }

        if (dead && particles.size == 0) {
            if (locMisses > 0 || particleMisses > 0 || emitterMisses > 0) {
                println(
                    """
                -- ${unrealizedEmitter.id} --
                $locMisses loc misses
                $particleMisses particle misses
                $emitterMisses emitter misses
            """.trimIndent()
                )
            }

            return EmitterTickResult(false)
        }

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.emitter?.dead = true
            }

            particle.tick()

            val initialPos = Vector3d(particle.data.relativePosition)
            val shouldUpdate = updateFrequencyComponent.shouldSendUpdate(emitterData, particle.data)
            shouldUpdate.interpolationDuration?.let { particle.data.interpolationDuration = it }

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            val c = GlobalTicker.emitterCache[emitterData.id]
            if (c != null) {
                val p = c[particle.data.id]
                if (p != null) {
                    if (particle.data.locHash() !in p.first) {
                        locMisses++
                    }
                } else {
                    particleMisses++
                }
            } else {
                emitterMisses++
            }

            if (particle.data.dead) {
                particleComponents.forEach { it.die(emitterData, particle.data) }
                die()
                continue
            }

            if (initialPos != particle.data.relativePosition) {
                if (shouldUpdate.shouldUpdate) {
                    particle.getMovementPacket().let { dataPackets += it }
                }
            }

            particle.updatePacket(EntityDataBuilder, shouldUpdate.shouldUpdate)
                ?.let { dataPackets += it }
        }

        val playerManager = PacketEvents.getAPI().playerManager

        particles.removeAll(deadParticles)
        val rawDeadParticleIDs = deadParticles.map { it.id }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()
        val particleIDs: MutableList<Int> by lazy {
            particles.map { it.id }.toMutableList().also { it.addAll(rawDeadParticleIDs) }
        }
        val chunkViewers = location.chunk.trackedByPlayers()

        val playersToRemove = HashSet<Player>()
        for (currentViewer in currentViewers) {
            if (currentViewer !in chunkViewers || !currentViewer.isOnline) {
                playersToRemove += currentViewer
            }
        }

        for (player in playersToRemove) {
            currentViewers -= player
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (player in playersToRemove) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (currentViewers.contains(player)) {
                if (distanceSquared > distanceCullingComponent.viewDistance || !audience.canBeApplied(player)) {
                    deadParticleIDs += player to particleIDs
                    currentViewers -= player
                }
            }

            if (!audience.canBeApplied(player) || player !in currentViewers) continue

            if (distanceSquared <= distanceCullingComponent.viewDistance) {
                deadParticleIDs += player to rawDeadParticleIDs
                for (packet in dataPackets) {
                    playerManager.sendPacketSilently(player, packet)
                }
            }
        }

        deadParticles.clear()

        if (!dead && emitterData.isActive) spawnParticles()

        blocked = false
        return EmitterTickResult(true, deadParticleIDs)
    }

    private fun killParticles(particlesToKill: List<Particle>) {
        if (particlesToKill.isEmpty()) return
        val ids = particlesToKill.map { it.id }.toIntArray()
        for (player in currentViewers) {
            player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids))
        }
    }

    private fun spawnParticles() {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData(random.uuid())
            val particle = Particle(particleData)
            particleData.particle = particle
            particleData.origin = location.toVector().toVector3d()
            particleData.billboardConstraints = billboardConstraints
            particleData.interpolationDelay = updateFrequencyComponent.interpolationDelay
            particleData.interpolationDuration = updateFrequencyComponent.initialInterpolationDuration

            particleComponents.forEach { it.execute(emitterData, particleData) }

            particle.init()

            val packets = particle.getAddPacket()
            bundle.addAll(packets)

            particles += particle
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (!audience.canBeApplied(player)) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < distanceCullingComponent.viewDistance) {
                currentViewers += player
                for (packet in bundle) {
                    player.toUser().sendPacketSilently(packet)
                }
            }
        }
    }

    override val players: List<Player>
        get() {
            val maxDistance = distanceCullingComponent.viewDistance
            return location.chunk.trackedByPlayers()
                .filter { audience.canBeApplied(it) }
                .filter { it.eyeLocation.distanceSquared(location) < maxDistance }
        }

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

    override val pose: Pose
        get() {
            return location.pose()
        }

    override val isPregen: Boolean = false
    override fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom
    ) {
        unrealizedEmitter.internalRealize(
            parent,
            location,
            environmentData,
            audience,
            random,
        )
    }

    override fun kill() {
        dead = true
        killParticles(particles)
        particles.clear()
    }

    override fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(input) else input
    }
}