package gg.aquatic.comet.v2.runtime

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.particle.V2Particle
import gg.aquatic.waves.Waves
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Utility for managing audience and packet sending
 * - New viewers need to be informed of all exising particles
 * - Old viewers need to be sent death packets for all existing particles
 * - Unchanged viewers just need to receive updates like normal
 */
class V2AudienceProcessor(val effect: Effect) {
    private val currentViewers = mutableSetOf<Player>()
    private val addedViewers = mutableSetOf<Player>()
    private val removedViewers = mutableSetOf<Player>()

    fun update(
        currentParticles: Collection<V2Particle>,
        dataPackets: Collection<PacketWrapper<*>>,
        deadIDs: IntArrayList,
    ) {
        removedViewers.clear()
        addedViewers.clear()

        var calculatedSpawnPackets = false
        val spawnPackets = mutableListOf<PacketWrapper<*>>()
        var allDestroyPacket: WrapperPlayServerDestroyEntities? = null
        val destroyPacket = WrapperPlayServerDestroyEntities(*deadIDs.toIntArray())

        val p = effect.pose.pos
        val loc = Location(effect.pose.world, p.x, p.y, p.z)

        val chunkViewers = Waves.NMS_HANDLER.chunkViewers(loc.chunk)

        for (currentViewer in currentViewers) {
            if (
                !currentViewer.isOnline ||
                !effect.audience.includes(currentViewer) ||
                currentViewer !in chunkViewers
            ) {
                removedViewers += currentViewer
                println("removing ${currentViewer.name}")

                if (allDestroyPacket == null) {
                    val arr = IntArray(currentParticles.size + deadIDs.size)
                    var i = 0
                    for (particle in currentParticles) {
                        arr[i] = particle.id
                        i++
                    }

                    while (i < currentParticles.size + deadIDs.size) {
                        arr[i] = deadIDs.getInt(i - currentParticles.size)
                        i++
                    }

                    allDestroyPacket = WrapperPlayServerDestroyEntities(*arr)
                }

                PacketEvents.getAPI().playerManager.sendPacketSilently(currentViewer, allDestroyPacket)

                continue
            }
        }

        currentViewers -= removedViewers

        for (chunkViewer in chunkViewers) {
            if (
                effect.audience.includes(chunkViewer) &&
                chunkViewer !in currentViewers
            ) {
                addedViewers += chunkViewer
                println("adding ${chunkViewer.name}")

                if (!calculatedSpawnPackets) {
                    calculatedSpawnPackets = true
                    for (particle in currentParticles) {
                        spawnPackets += particle.getAddPacket()
                    }
                }

                for (packet in spawnPackets) {
                    PacketEvents.getAPI().playerManager.sendPacketSilently(chunkViewer, packet)
                }
            }
        }

        currentViewers += addedViewers

        for (viewer in currentViewers) {
            for (packet in dataPackets) {
                PacketEvents.getAPI().playerManager.sendPacketSilently(viewer, packet)
            }

            PacketEvents.getAPI().playerManager.sendPacketSilently(viewer, destroyPacket)
        }
    }

    fun kill(
        currentParticles: Collection<V2Particle>,
    ) {
        val arr = IntArray(currentParticles.size)
        var i = 0
        for (particle in currentParticles) {
            arr[i] = particle.id
            i++
        }

        val destroyPacket = WrapperPlayServerDestroyEntities(*arr)

        for (viewer in currentViewers) {
            PacketEvents.getAPI().playerManager.sendPacketSilently(viewer, destroyPacket)
        }
    }
}