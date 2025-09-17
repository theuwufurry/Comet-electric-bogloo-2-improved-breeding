package gg.aquatic.comet.parsing

import gg.aquatic.comet.api.AbstractParticleEmitter

object ConfigLoader {
    fun init() {
        AbstractParticleEmitter.INSTANCE.saveResource("config.yml", false)
    }
}