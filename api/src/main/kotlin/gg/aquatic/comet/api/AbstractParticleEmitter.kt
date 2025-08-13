package gg.aquatic.comet.api

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory


abstract class AbstractParticleEmitter: JavaPlugin() {
    companion object {
        lateinit var INSTANCE: AbstractParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory
        lateinit var MINIMESSAGE: MiniMessage
    }
}