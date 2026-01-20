package gg.aquatic.comet

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.CometRegistry
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.command.*
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.hook.IHook
//import gg.aquatic.comet.hook.modelengine.ModelEngineHook
//import gg.aquatic.comet.hook.mythicmobs.MythicMobsHook
import gg.aquatic.comet.parsing.ConfigLoader
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.api.parsing.resourcepack.packages.PackageManager
import gg.aquatic.comet.particle.macro.CatmullParser
import gg.aquatic.comet.particle.macro.HermiteParser
import gg.aquatic.comet.particle.macro.LinearParser
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit.getCommandMap
import org.joml.Vector3f
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory

//val debugDust: Particle.DustOptions = Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6F)

class ParticleEmitter : AbstractParticleEmitter() {
    override fun onEnable() {
        INSTANCE = this
        MINIMESSAGE = MiniMessage.miniMessage()

        scriptEngineFactory = NashornScriptEngineFactory()
        initializeMacros()

        ConfigLoader.init()
        PackageManager.compile()
        ParticleJsonParser.init()
        CometRegistry.jsonParser = ParticleJsonParser

        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()

        ResourcepackCreator.reload()

        GlobalTicker.init()
        registerCometCommands()
    }


    fun registerCometCommands() {
        // Create the base command
        val cometCommand = BaseCommand(
            name = "comet",
            description = "Base command of Comet plugin"
        )

        // Add subcommands
        cometCommand.subCommands["reload"] = ReloadParticleScriptsCommand
        cometCommand.subCommands["spawn"] = SpawnCommand
        cometCommand.subCommands["clear"] = ClearParticlesCommand
        cometCommand.subCommands["help"] = HelpCommand
        cometCommand.subCommands["at"] = AtCommand
        cometCommand.subCommands["mount"] = MountedSpawnCommand
        cometCommand.subCommands["info"] = InfoCommand
        cometCommand.subCommands["kill"] = KillCommand

        // Register the command with Bukkit
        getCommandMap().register("comet", cometCommand)

        // Initialize hooks
        initializeHooks()

        // Fancy console banner
        println(
            """
   (                          )  
   )\           )      (   ( /(  
 (((_)   (     (      ))\  )\()) 
 )\___   )\    )\  ' /((_)(_))/  
((/ __| ((_) _((_)) (_))  | |_   
 | (__ / _ \| '  \()/ -_) |  _|  
  \___|\___/|_|_|_| \___|  \__|  
        """.trimIndent()
        )
    }

    override fun onDisable() {
        ParticleJsonParser.onDisable()
        GlobalTicker.disable()
    }

    private fun initializeHooks() {
        val hooks = mutableListOf<IHook>()
//        if (server.pluginManager.getPlugin("MythicMobs") != null) {
//            hooks += MythicMobsHook
//        }
//        if (server.pluginManager.getPlugin("ModelEngine") != null) {
//            hooks += ModelEngineHook
//        }
        hooks.forEach {
            try {
                it.initialize()
            } catch (e: Exception) {
                logger.warning("Failed to initialize hook: ${it.javaClass.name}")
            }
        }
    }

    private fun initializeMacros() {
        CometRegistry.macroParsers += "hermite" to HermiteParser
        CometRegistry.macroParsers += "catmull" to CatmullParser
        CometRegistry.macroParsers += "linear" to LinearParser
    }
}

fun <T> T.applyIf(condition: Boolean, action: T.() -> Unit): T {
    if (condition) action()
    return this
}

data class Result<out R, out E>(
    val result: R,
    val errors: List<E>
) {
    companion object {
        infix fun <R, E> R.with(other: List<E>): Result<R, E> {
            return Result(this, other)
        }
    }
}

/**
 * The 'direction' that an emitter faces initially; should be replaced with emitter rotation component, since this cannot represent the full quaternion
 * TODO: Create emitter rotation component
 */
val DEFAULT_EMITTER_DIRECTION = Vector3f(0f, 0f, -1f)