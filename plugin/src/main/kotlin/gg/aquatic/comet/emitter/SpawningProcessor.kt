package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.waves.chunk.trackedByPlayers
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.toUser
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class SpawningProcessor(
    private val emitter: AbstractEmitter,
    private val distanceCullingComponent: DistanceCullingComponent,
) {
    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()
    private val deadParticles: MutableList<Particle> = mutableListOf()

    fun processDead(dataPackets: MutableList<PacketWrapper<*>>): MutableList<Pair<Player, MutableList<Int>>> {
        val playerManager = PacketEvents.getAPI().playerManager

        emitter.particles.removeAll(deadParticles)
        val rawDeadParticleIDs = deadParticles.flatMap { it.entityIDs }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()
        val particleIDs: MutableList<Int> by lazy {
            emitter.particles.flatMap { it.entityIDs }.toMutableList().also { it.addAll(rawDeadParticleIDs) }
        }
        val chunkViewers = emitter.location.chunk.trackedByPlayers()

        val playersToRemove = HashSet<Player>()
        for (currentViewer in currentViewers) {
            if (currentViewer !in chunkViewers || !currentViewer.isOnline) {
                playersToRemove += currentViewer
            }
        }

        for (player in playersToRemove) {
            currentViewers -= player
        }

        for (player in emitter.location.chunk.trackedByPlayers()) {
            if (player in playersToRemove) continue
            val distanceSquared = player.eyeLocation.distanceSquared(emitter.location)
            if (currentViewers.contains(player)) {
                if (distanceSquared > distanceCullingComponent.viewDistance || !emitter.audience.canBeApplied(player)) {
                    deadParticleIDs += player to particleIDs
                    currentViewers -= player
                }
            }

            if (!emitter.audience.canBeApplied(player) || player !in currentViewers) continue

            if (distanceSquared <= distanceCullingComponent.viewDistance) {
                deadParticleIDs += player to rawDeadParticleIDs
                for (packet in dataPackets) {
                    try {
                        playerManager.sendPacketSilently(player, packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }

        deadParticles.clear()
        return deadParticleIDs
    }

    fun die(particle: Particle) {
        deadParticles += particle
    }

    fun killParticles(particlesToKill: List<Particle>) {
        if (particlesToKill.isEmpty()) return
        val ids = particlesToKill.flatMap { it.entityIDs }.toIntArray()
        for (player in currentViewers) {
            try {
                player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids))
            } catch (ignored: NullPointerException) {
            }
        }
    }

    fun sendSpawns(bundle: MutableList<PacketWrapper<*>>) {
        for (player in emitter.location.chunk.trackedByPlayers()) {
            if (!emitter.audience.canBeApplied(player)) continue
            val distanceSquared = player.eyeLocation.distanceSquared(emitter.location)
            if (distanceSquared < distanceCullingComponent.viewDistance) {
                currentViewers += player
                for (packet in bundle) {
                    try {
                        player.toUser().sendPacketSilently(packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }
    }
    val players: List<Player>
        get() {
            val maxDistance = distanceCullingComponent.viewDistance
            return emitter.location.chunk.trackedByPlayers()
                .filter { emitter.audience.canBeApplied(it) }
                .filter { it.eyeLocation.distanceSquared(emitter.location) < maxDistance }
        }
}