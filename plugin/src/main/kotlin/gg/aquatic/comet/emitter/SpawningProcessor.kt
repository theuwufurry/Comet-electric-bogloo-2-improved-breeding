package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.packet.PassengerManager
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

    private val removedViewers = HashSet<Player>()
    private val addedViewers = HashSet<Player>()

    fun tick() {
        removedViewers.clear()
        addedViewers.clear()

        val chunkViewers = emitter.pose.location.chunk.trackedByPlayers()

        for (currentViewer in currentViewers) {
            val distanceSquared = currentViewer.eyeLocation.distanceSquared(emitter.pose.location)
            if (!currentViewer.isOnline || distanceSquared > distanceCullingComponent.viewDistance || currentViewer !in chunkViewers || !emitter.audience.canBeApplied(
                    currentViewer
                )
            ) {
                removedViewers += currentViewer
                continue
            }
        }

        currentViewers -= removedViewers

        for (chunkViewer in chunkViewers) {
            val distanceSquared = chunkViewer.eyeLocation.distanceSquared(emitter.pose.location)
            if (distanceSquared < distanceCullingComponent.viewDistance && chunkViewer !in currentViewers && emitter.audience.canBeApplied(
                    chunkViewer
                )
            ) {
                addedViewers += chunkViewer
            }
        }

        currentViewers += addedViewers
    }

    fun process(dataPackets: MutableList<PacketWrapper<*>>): MutableList<Pair<Player, MutableList<Int>>> {
        val playerManager = PacketEvents.getAPI().playerManager

        emitter.particles.removeAll(deadParticles)
        val rawDeadParticleIDs = deadParticles.flatMap { it.entityIDs }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()
        val particleIDs: MutableList<Int> by lazy {
            emitter.particles.flatMap { it.entityIDs }.toMutableList().also { it.addAll(rawDeadParticleIDs) }
        }

        for (currentViewer in currentViewers) {
            deadParticleIDs += currentViewer to rawDeadParticleIDs
            for (packet in dataPackets) {
                try {
                    playerManager.sendPacketSilently(currentViewer, packet)
                } catch (ignored: NullPointerException) {
                }
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
                    try {
                        viewer.toUser().sendPacketSilently(spawnPacket)
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
        val ls = particlesToKill.flatMap { it.entityIDs }
        val ids = ls.toIntArray()
        for (player in currentViewers) {
            PassengerManager.passengerMap[player.entityId]?.removeAll(ls)
            try {
                player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids))
            } catch (ignored: NullPointerException) {
            }
        }
    }

    fun sendSpawns(bundle: MutableList<PacketWrapper<*>>) {
        for (player in currentViewers) {
            for (packet in bundle) {
                try {
                    player.toUser().sendPacketSilently(packet)
                } catch (ignored: NullPointerException) {
                }
            }
        }
    }

    val players: List<Player>
        get() {
            val maxDistance = distanceCullingComponent.viewDistance
            return emitter.pose.location.chunk.trackedByPlayers()
                .filter { emitter.audience.canBeApplied(it) }
                .filter { it.eyeLocation.distanceSquared(emitter.pose.location) < maxDistance }
        }
}