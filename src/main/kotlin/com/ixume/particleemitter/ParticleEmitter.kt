package com.ixume.particlesTesting

import com.ixume.particlesTesting.command.CustomParticleSpawnCommand
import com.ixume.particlesTesting.parsing.ParticleJsonParser
import org.bukkit.plugin.java.JavaPlugin

class ParticleEmitter : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: ParticleEmitter
    }

    override fun onEnable() {
        INSTANCE = this
        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()
        GlobalTicker.init()

        CustomParticleSpawnCommand
    }
}