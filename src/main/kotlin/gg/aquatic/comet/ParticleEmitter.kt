package gg.aquatic.comet

import gg.aquatic.comet.command.*
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.ResourcepackCreator
import gg.aquatic.waves.command.AquaticBaseCommand
import gg.aquatic.waves.command.register
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory

//val debugDust: Particle.DustOptions = Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6F)

class ParticleEmitter : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: ParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory
    }

    override fun onEnable() {
        INSTANCE = this

        scriptEngineFactory = NashornScriptEngineFactory()
        ParticleJsonParser.init()

        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()

        ResourcepackCreator.genPack()

        AquaticBaseCommand(
            "comet", "Base command of Comet plugin", mutableListOf(), mutableMapOf(
                "reload" to ReloadParticleScriptsCommand,
                "spawn" to CustomParticleCommand,
                "clear" to ClearParticlesCommand
            ),
            listOf()
        ).register("comet")
    }
}