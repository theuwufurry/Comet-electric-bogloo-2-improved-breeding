package com.ixume.particleemitter

import com.ixume.particleemitter.command.ExplosionParticleSpawnCommand
import com.ixume.particleemitter.command.FireParticleSpawnCommand
import com.ixume.particleemitter.command.OffsetTest
import com.ixume.particleemitter.command.ReloadParticleScriptsCommand
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.ResourcepackCreator
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import sun.misc.Unsafe

//val debugDust: Particle.DustOptions = Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6F)

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

        ResourcepackCreator.genPack()

        FireParticleSpawnCommand
        ExplosionParticleSpawnCommand
        OffsetTest
        ReloadParticleScriptsCommand
    }

    override fun onDisable() {
        GlobalEmitterTicker.kill()
    }
}