package gg.aquatic.comet

import gg.aquatic.comet.command.ClearParticlesCommand
import gg.aquatic.comet.command.CustomParticleCommand
import gg.aquatic.comet.command.ReloadParticleScriptsCommand
import gg.aquatic.comet.hook.IHook
import gg.aquatic.comet.hook.ModelEngineHook
import gg.aquatic.comet.hook.mythicmobs.MythicMobsHook
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.ResourcepackCreator
import gg.aquatic.comet.parsing.macro.Catmull
import gg.aquatic.comet.parsing.macro.CatmullEvaluator
import gg.aquatic.waves.command.AquaticBaseCommand
import gg.aquatic.waves.command.register
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Vector3d
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

    private fun initializeHooks() {
        val hooks = mutableListOf<IHook>()
        if (server.pluginManager.getPlugin("MythicMobs") != null) {
            hooks += MythicMobsHook
        }
        if (server.pluginManager.getPlugin("ModelEngine") != null) {
            hooks += ModelEngineHook
        }
        hooks.forEach { it.initialize() }
    }
}