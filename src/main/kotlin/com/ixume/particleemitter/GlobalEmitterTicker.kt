package com.ixume.particleemitter

import com.ixume.particleemitter.emitter.Emitter
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentLinkedQueue

object GlobalEmitterTicker {
    val emitters: ConcurrentLinkedQueue<Emitter> = ConcurrentLinkedQueue()
    private var task: BukkitTask

    init {
        task = Bukkit.getScheduler().runTaskTimer(ParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    fun kill() {
        task.cancel()
    }

    private fun tick() {
        for (emitter in emitters) {
            emitter.tick()
        }
    }
}