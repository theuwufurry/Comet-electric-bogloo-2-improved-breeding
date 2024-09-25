package com.ixume.particleemitter

import com.ixume.particleemitter.command.CustomParticleSpawnCommand
import com.ixume.particleemitter.parsing.ParticleJsonParser
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory

class ParticleEmitter : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: ParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory
    }

    override fun onEnable() {
        INSTANCE = this
        scriptEngineFactory = NashornScriptEngineFactory()
        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()
        GlobalTicker.init()

        CustomParticleSpawnCommand
    }
}