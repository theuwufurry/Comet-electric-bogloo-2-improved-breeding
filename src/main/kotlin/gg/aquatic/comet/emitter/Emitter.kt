package gg.aquatic.comet.emitter

import gg.aquatic.comet.emitter.bundle.BundledEmitterComponent
import gg.aquatic.comet.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.recursive.RecursiveEmitterComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.UpdateFlags
import gg.aquatic.comet.particle.color.ColorComponent
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.transformation.rotation.RotationComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
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

/*
take in component list
    component list with preset fields for the stuff that's needed
    mandatory components with preset hooks
 */
class Emitter(
    private val rateComponent: RateComponent,
    private val particleLifetimeComponent: ParticleLifetimeComponent,
    private val shapeComponent: ShapeComponent,
    private val displayComponent: DisplayComponent,
    private val colorComponent: ColorComponent,
    private val emitterLifetimeComponent: EmitterLifetimeComponent,
    private val positionComponent: PositionComponent,
    private val scaleComponent: ScaleComponent,
    private val rotationComponent: RotationComponent,
    private val recursiveEmitterComponent: RecursiveEmitterComponent?,
    private val bundledEmitterComponent: BundledEmitterComponent?, //KEEP THIS AROUND! Might be needed for future variable stuff.
    private val distanceCullingComponent: DistanceCullingComponent,
    private val updateFrequencyComponent: UpdateFrequencyComponent,
    private val billboardConstraints: BillboardConstraints,
    location: Location,
    private val emitterData: EmitterData,
    private val unrealizedHolder: UnrealizedEmitter,
    private val audience: AquaticAudience
) {
    var location = location
        private set

    //origin can change, rotation can change
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false
    private val emitterRotation =
        Quaternionf().rotateTo(Vector3f(0f, 0f, 1f), location.direction.normalize().toVector3f())

    val currentViewers = ConcurrentHashMap.newKeySet<Player>()

    fun tick(): EmitterTickResult {
        if (blocked) {
            return EmitterTickResult(true)
        }

        blocked = true

        emitterData.age++

        if (!dead && !emitterLifetimeComponent.keepAlive(emitterData)) {
            dead = true
        }

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
            recursiveEmitterComponent?.updateEmitter(emitterData, particle.data)

            if (!particleLifetimeComponent.keepAlive(emitterData, particle.data)) {
                die()
                continue
            }

            var updateParticle = false

            val newDisplay = displayComponent.display(emitterData, particle.data)
            if (newDisplay != particle.data.displayData) {
                updateParticle = true
                particle.data.displayData = newDisplay
            }

            val newColor = colorComponent.color(emitterData, particle.data)
            if (newColor != particle.data.color) {
                updateParticle = true
                particle.data.color = newColor
            }

            val newPos = positionComponent.pos(emitterData, particle.data)

            if (particle.data.dead) {
                die()
                continue
            }

            val shouldUpdate = updateFrequencyComponent.shouldSendUpdate(emitterData, particle.data)

            if (newPos.data != particle.data.relativePosition) {
                particle.data.relativePosition = newPos.data
                if (shouldUpdate) {
                    particle.getMovementPacket().let { dataPackets += it }
                }
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val newRotation = rotationComponent.rotation(emitterData, particle.data).applyEmitterRotation()
            if (particle.data.scale != newScale) {
                updateParticle = true
                particle.data.scale = newScale
            }

            if (particle.data.rotation != newRotation) {
                updateParticle = true
                particle.data.rotation = newRotation
            }

            if (updateParticle && shouldUpdate) {
                particle.updatePacket(unrealizedHolder.myEntityDataBuilder)?.let { dataPackets += it }
            }
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
            val scale = scaleComponent.scale(emitterData, particleData)
            val rotation = rotationComponent.rotation(emitterData, particleData)
            particleData.relativePosition = positionComponent.pos(emitterData, particleData).data
            particleData.origin =
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z)
            particleLifetimeComponent.keepAlive(emitterData, particleData)
            particleData.displayData = displayComponent.display(emitterData, particleData)
            particleData.color = colorComponent.color(emitterData, particleData)
            particleData.billboardConstraints = billboardConstraints
            particleData.scale = scale
            particleData.rotation = rotation.applyEmitterRotation()
            particleData.interpolationDelay = emitterData.interpolationDelay
            particleData.interpolationDuration = emitterData.interpolationDuration
            recursiveEmitterComponent?.run { updateEmitter(emitterData, particleData) }
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

    private fun Quaternionf.applyEmitterRotation(): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(this) else this
    }
}

class EmitterTickResult(val alive: Boolean, val deadParticles: List<Pair<Player, MutableList<Int>>> = listOf())