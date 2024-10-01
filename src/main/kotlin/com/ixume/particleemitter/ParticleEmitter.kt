package com.ixume.particleemitter

import com.ixume.particleemitter.command.CustomParticleSpawnCommand
import com.ixume.particleemitter.command.ReloadParticleScriptsCommand
import com.ixume.particleemitter.parsing.ParticleJsonParser
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import sun.misc.Unsafe

class ParticleEmitter : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: ParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory

        lateinit var unsafe: Unsafe
    }

    override fun onEnable() {
        INSTANCE = this

        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        unsafe = field.get("null") as Unsafe

        scriptEngineFactory = NashornScriptEngineFactory()
        ParticleJsonParser.init()

        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()

        CustomParticleSpawnCommand
        ReloadParticleScriptsCommand
    }
}