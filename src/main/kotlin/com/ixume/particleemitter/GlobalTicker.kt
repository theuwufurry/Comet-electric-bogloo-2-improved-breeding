package com.ixume.particlesTesting

import com.ixume.particlesTesting.emitter.Emitter
import org.bukkit.Bukkit
import java.util.concurrent.CopyOnWriteArrayList

object GlobalTicker {
    val emitters: MutableList<Emitter> = CopyOnWriteArrayList<Emitter>().toMutableList()

    fun init() {
        Bukkit.getScheduler().runTaskTimer(ParticleEmitter.INSTANCE, Runnable {
            emitters.forEach(Emitter::tick)
        }, 1, 1)
    }
}