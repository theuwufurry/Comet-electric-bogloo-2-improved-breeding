package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.emitter.optimization.CachedPath
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
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
    private val emitterInitializations:  Queue<() -> AbstractEmitter> = ConcurrentLinkedQueue()

    private val emitters: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToAdd: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToKill: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private var task: BukkitTask? = null

    val emitterCache: MutableMap<UUID, CachedPath> = ConcurrentHashMap()

    fun init() {
        task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    fun addInitialization(initialization: () -> AbstractEmitter) {
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

//            println("Thread blocked")
            return
        }

        blocked.set(true)

//        println("/\\/\\/\\/\\/\\/\\ BEGAN TICK /\\/\\/\\/\\/\\")
        val deadEmitters = HashSet<AbstractEmitter>()
        val playerDeadParticleMap: MutableMap<Player, MutableList<Int>> = mutableMapOf()
        val t = measureNanoTime {
            for (emitter in emitters) {
//                println("Ticked ${(emitter as Emitter).unrealizedEmitter.id}")
                val result = emitter.tick()

                if (!result.alive) {
                    deadEmitters += emitter
                    emitterCache -= emitter.id
//                    println("Removing ${(emitter as Emitter).unrealizedEmitter.id}")
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

//        println("DeadEmitters: ${deadEmitters.size}")
//        println("Pre: ${emitters.size}")
        emitters.removeAll(deadEmitters)
//        println("Post: ${emitters.size}")

//        synchronized(emittersToAdd) {
//        println("ToAdd: ${emittersToAdd.size}")
        emitters.addAll(emittersToAdd)
//        println("Post: ${emitters.size}")
        emittersToAdd.clear()
//        }

        val start = System.currentTimeMillis()
        while(true) {
            val curr = emitterInitializations.poll() ?: break
            val r = curr()
//            println("Initialized: ${r.unrealizedEmitter.id}")
            emitters += r
            if (System.currentTimeMillis() - start > TIMEOUT_MS) break
        }

//        println("Post Init: ${emitters.size}")

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
    const val DEBUG = 0
}