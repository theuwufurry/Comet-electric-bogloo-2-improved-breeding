package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.emitter.optimization.CachedPath
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.toUser
import io.ktor.util.collections.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashSet

object GlobalTicker {
    private val emitters: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToAdd: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private var task: BukkitTask? = null

    val emitterCache: MutableMap<UUID, CachedPath> = ConcurrentHashMap()

    fun init() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(AbstractParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    fun addEmitter(emitter: AbstractEmitter) {
        emittersToAdd += emitter
    }

    private fun tick() {
        val deadEmitters = HashSet<AbstractEmitter>()
        val playerDeadParticleMap: MutableMap<Player, MutableList<Int>> = mutableMapOf()
        for (emitter in emitters) {
            val result = emitter.tick()
            if (!result.alive) {
                deadEmitters += emitter
                emitterCache -= emitter.id
            }

            for ((player, ids) in result.deadParticles) {
                val entry = playerDeadParticleMap[player]
                if (entry == null) {
                    playerDeadParticleMap[player] = ids
                } else {
                    entry.addAll(ids)
                }
            }
        }

        for ((player, ids) in playerDeadParticleMap) {
            if (player.toUser() == null) continue
            player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids.toIntArray()))
        }

        emitters.removeAll(deadEmitters)

        synchronized(emittersToAdd) {
            emitters.addAll(emittersToAdd)
            emittersToAdd.clear()
        }
    }


    fun disable() {
        killInstances()
        task?.cancel()
    }

    fun killInstances() {
        emitters.forEach { it.kill() }
        emitterCache.clear()
    }
}