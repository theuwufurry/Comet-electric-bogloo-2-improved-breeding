package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.emitter.optimization.CachedPath
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.map
import gg.aquatic.waves.util.toUser
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime

object GlobalTicker {
    private val emitterInitializations:  Queue<() -> Emitter> = ConcurrentLinkedQueue()

    private val emitters: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToAdd: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private var task: BukkitTask? = null

    val emitterCache: MutableMap<UUID, CachedPath> = ConcurrentHashMap()

    fun init() {
        task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    fun addInitialization(initialization: () -> Emitter) {
        emitterInitializations += initialization
    }

    fun addEmitter(emitter: AbstractEmitter) {
        emittersToAdd += emitter
    }

    val blocked = AtomicBoolean(false)
    private fun tick() {
        if (blocked.get()) {
//            if (DEBUG >= 1 && emitters.size > 0) {
//                println("--- TICK ---")
//                println("  | eI: ${emitterInitializations.size}")
//                println("  | e: ${emitters.size}")
//                println("  | eTA: ${emittersToAdd.size}")
//                println("  | eC: ${emitterCache.size}")
//            }

            println("Thread blocked")
            return
        }

        blocked.set(true)

        val start = System.currentTimeMillis()
        while(true) {
            val curr = emitterInitializations.poll() ?: break
            emitters += curr()
            if (System.currentTimeMillis() - start > TIMEOUT_MS) break
        }

        val deadEmitters = HashSet<AbstractEmitter>()
        val playerDeadParticleMap: MutableMap<Player, MutableList<Int>> = mutableMapOf()
        val t = measureNanoTime {
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
        }

        if (DEBUG >= 1 && emitters.size > 0) {
            println("Tick Took ${t.toDouble() / 1_000_000.0}ms")
        }

        for ((player, ids) in playerDeadParticleMap) {
            if (player.toUser() == null) continue
            player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids.toIntArray()))
        }

        emitters.removeAll(deadEmitters)

//        synchronized(emittersToAdd) {
        emitters.addAll(emittersToAdd)
        emittersToAdd.clear()
//        }

        blocked.set(false)
    }


    fun disable() {
        killInstances()
        task?.cancel()
    }

    fun killInstances() {
        emitters.forEach { it.kill() }
        emitterCache.clear()
    }

    const val TIMEOUT_MS = 50
    const val DEBUG = 1
}