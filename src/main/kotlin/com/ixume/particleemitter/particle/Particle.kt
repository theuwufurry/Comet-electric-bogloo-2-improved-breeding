package com.ixume.particlesTesting.particle

import org.bukkit.entity.TextDisplay

class Particle(val entity: TextDisplay?) {
    val data: ParticleMochaData = ParticleMochaData(0.0)

    fun tick() {
        data.age++
    }
}