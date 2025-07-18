package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.packet.PassengerManager
import gg.aquatic.comet.emitter.optimization.CachedPath
import gg.aquatic.waves.Waves
import gg.aquatic.waves.util.sendPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime

object GlobalTicker {
    private val emitterInitializations: Queue<() -> AbstractEmitter> = ConcurrentLinkedQueue()

    internal val emitters: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToAdd: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private val emittersToKill: Queue<AbstractEmitter> = ConcurrentLinkedQueue()
    private var task: BukkitTask? = null

    val emitterCache: MutableMap<UUID, CachedPath> = ConcurrentHashMap()

    fun init() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(AbstractParticleEmitter.INSTANCE, Runnable {
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
    private var tickTime = 0
    private fun tick() {
        if (blocked.get()) {
            return
        }

        blocked.set(true)

        tickTime++

        val deadEmitters = HashSet<AbstractEmitter>()
        val playerDeadParticleMap: MutableMap<Player, MutableList<Int>> = mutableMapOf()
//        println("emitters: ${emitters.size}")
        val t = measureNanoTime {
            for (emitter in emitters) {
                try {
//                    println("tryna tick at $tickTime")
                    val result = emitter.tick()
                    if (!result.alive) {
//                        println("GLOBAL.EMITTERDEAD!")
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
                } catch (e: Exception) {
                    e.printStackTrace()
                    deadEmitters += emitter
                    emitterCache -= emitter.id
                }
            }
        }

        if (DEBUG >= 1 && emitters.size > 0) {
            println("Tick Took ${t.toDouble() / 1_000_000.0}ms")
        }

        for ((player, ids) in playerDeadParticleMap) {
            PassengerManager.passengerMap[player.entityId]?.removeAll(ids)
            val destroyPacket = Waves.NMS_HANDLER.createDestroyEntitiesPacket(*ids.toIntArray())
            player.sendPacket(destroyPacket, true)
        }

        emitters.removeAll(deadEmitters)

        emitters.addAll(emittersToAdd)
        emittersToAdd.clear()

        val start = System.currentTimeMillis()
        while (true) {
            val curr = emitterInitializations.poll() ?: break
//            println("initializing at $tickTime")
            val r = curr()
            emitters += r
            if (System.currentTimeMillis() - start > TIMEOUT_MS) break
        }

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