package gg.aquatic.comet.emitter

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.waves.chunk.trackedByPlayers
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.toUser
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap

class Emitter(
    private val components: List<Component>,
    private val rateComponent: RateComponent,
    private val shapeComponent: ShapeComponent, //DEPRECATED
    private val distanceCullingComponent: DistanceCullingComponent,
    private val updateFrequencyComponent: UpdateFrequencyComponent,
    private val billboardConstraints: BillboardConstraints,
    location: Location,
    private val emitterData: EmitterData,
    private val unrealizedHolder: UnrealizedEmitter,
    private val audience: AquaticAudience
) {
    private val emitterComponents: List<EmitterComponent> = components.filterIsInstance<EmitterComponent>()
    private val particleComponents: List<ParticleComponent> = components.filterIsInstance<ParticleComponent>()

    var location = location
        private set

    //origin can change, rotation can change
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false
    private val emitterRotation =
        Quaternionf().rotateTo(Vector3f(0f, 0f, 1f), location.direction.normalize().toVector3f())

    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()

    init {
        emitterData.emitter = this
        emitterComponents.forEach { it.init(emitterData) }
    }

    fun tick(): EmitterTickResult {
        if (blocked) return EmitterTickResult(true)

        blocked = true

        emitterData.age++

        emitterComponents.forEach { it.execute(emitterData) }

        if (emitterData.dead) dead = true

        if (dead && particles.size == 0) {
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
            if (shouldUpdate.interpolationDuration != null) {
                particle.data.interpolationDuration = shouldUpdate.interpolationDuration
            }

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            if (particle.data.dead) {
                die()
                continue
            }

            if (initialPos != particle.data.relativePosition) {
                if (shouldUpdate.shouldUpdate) {
                    particle.getMovementPacket().let { dataPackets += it }
                }
            }

            particle.updatePacket(unrealizedHolder.myEntityDataBuilder, shouldUpdate.shouldUpdate)
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
                    playerManager.sendPacket(player, packet)
                }
            }
        }

        deadParticles.clear()

        if (!dead) spawnParticles()

        blocked = false
        return EmitterTickResult(true, deadParticleIDs)
    }

    private fun killParticles(particlesToKill: List<Particle>) {
        if (particlesToKill.isEmpty()) return
        val ids = particlesToKill.map { it.id }.toIntArray()
        for (player in currentViewers) {
            player.toUser().sendPacket(WrapperPlayServerDestroyEntities(*ids))
        }
    }

    private fun spawnParticles() {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData()
            val spawnOffset = shapeComponent.offset(emitterData, particleData)

            particleData.origin =
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z)
            particleData.billboardConstraints = billboardConstraints
            particleData.interpolationDelay = updateFrequencyComponent.interpolationDelay
            particleData.interpolationDuration = updateFrequencyComponent.initialInterpolationDuration

            particleComponents.forEach { it.execute(emitterData, particleData) }

            val particle = Particle(particleData)
            val packets = particle.getAddPacket(unrealizedHolder.myEntityDataBuilder)
            bundle.addAll(packets)

            particles += particle
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (!audience.canBeApplied(player)) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < distanceCullingComponent.viewDistance) {
                currentViewers += player
                for (packet in bundle) {
                    player.toUser().sendPacket(packet)
                }
            }
        }
    }

    fun setPos(x: Double, y: Double, z: Double) {
        location.x = x
        location.y = y
        location.z = z
    }

    fun kill() {
        dead = true
        killParticles(particles)
        particles.clear()
    }

    fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(input) else input
    }
}

class EmitterTickResult(val alive: Boolean, val deadParticles: List<Pair<Player, MutableList<Int>>> = listOf())