package gg.aquatic.comet.emitter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.packet.PassengerManager
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import org.bukkit.Chunk
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class SpawningProcessor(
    private val emitter: AbstractEmitter,
    private val distanceCullingComponent: DistanceCullingComponent,
) {
    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()
    private val deadParticles: MutableList<Particle> = mutableListOf()

    private val removedViewers = HashSet<Player>()
    private val addedViewers = HashSet<Player>()

    fun tick() {
        removedViewers.clear()
        addedViewers.clear()

        val chunkViewers = getChunkViewers(emitter.pose.location.chunk)

        for (currentViewer in currentViewers) {
            val distanceSquared = currentViewer.eyeLocation.distanceSquared(emitter.pose.location)
            if (
                !currentViewer.isOnline ||
                distanceSquared > distanceCullingComponent.viewDistance ||
                currentViewer !in chunkViewers ||
                !emitter.audience.canBeApplied(currentViewer)
            ) {
                removedViewers += currentViewer
            }
        }

        currentViewers -= removedViewers

        for (chunkViewer in chunkViewers) {
            val distanceSquared = chunkViewer.eyeLocation.distanceSquared(emitter.pose.location)
            if (
                distanceSquared < distanceCullingComponent.viewDistance &&
                chunkViewer !in currentViewers &&
                emitter.audience.canBeApplied(chunkViewer)
            ) {
                addedViewers += chunkViewer
            }
        }

        currentViewers += addedViewers
    }

    fun process(dataPackets: MutableList<PacketWrapper<*>>): MutableList<Pair<Player, MutableList<Int>>> {
        emitter.particles.removeAll(deadParticles)

        val rawDeadParticleIDs = deadParticles.flatMap { it.entityIDs }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()

        val particleIDs: MutableList<Int> by lazy {
            emitter.particles.flatMap { it.entityIDs }
                .toMutableList()
                .also { it.addAll(rawDeadParticleIDs) }
        }

        for (currentViewer in currentViewers) {
            deadParticleIDs += currentViewer to rawDeadParticleIDs
            for (packet in dataPackets) {
                PacketEvents.getAPI().playerManager.sendPacketSilently(currentViewer, packet)
            }
        }

        for (viewer in removedViewers) {
            deadParticleIDs += viewer to particleIDs
        }

        if (emitter.unrealizedEmitter.persistent) {
            val spawnPackets: List<PacketWrapper<*>> by lazy {
                emitter.getSpawnPackets()
            }

            for (viewer in addedViewers) {
                for (spawnPacket in spawnPackets) {
                    PacketEvents.getAPI().playerManager.sendPacketSilently(viewer, spawnPacket)
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
        val destroyPacket = WrapperPlayServerDestroyEntities(*ids)

        for (player in currentViewers) {
            PassengerManager.passengerMap[player.entityId]?.removeAll(ids.toList())
            PacketEvents.getAPI().playerManager.sendPacketSilently(player, destroyPacket)
        }
    }

    fun sendSpawns(bundle: MutableList<PacketWrapper<*>>) {
        for (player in currentViewers) {
            for (packet in bundle) {
                PacketEvents.getAPI().playerManager.sendPacketSilently(player, packet)
            }
        }
    }

    val players: List<Player>
        get() {
            val maxDistance = distanceCullingComponent.viewDistance
            return getChunkViewers(emitter.pose.location.chunk)
                .filter { emitter.audience.canBeApplied(it) }
                .filter { it.eyeLocation.distanceSquared(emitter.pose.location) < maxDistance }
        }

    /**
     * Replacement for Waves.NMS_HANDLER.chunkViewers(...)
     * Matches vanilla client view distance behavior.
     */
    private fun getChunkViewers(chunk: Chunk): Collection<Player> {
        val world = chunk.world
        val cx = chunk.x
        val cz = chunk.z

        return world.players.filter { player ->
            if (!player.isOnline) return@filter false

            val pcx = player.location.chunk.x
            val pcz = player.location.chunk.z
            val view = player.clientViewDistance

            abs(pcx - cx) <= view && abs(pcz - cz) <= view
        }
    }
}
