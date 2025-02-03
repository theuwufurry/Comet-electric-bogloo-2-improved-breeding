package gg.aquatic.comet.api

import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory


abstract class AbstractParticleEmitter: JavaPlugin() {
    companion object {
        lateinit var INSTANCE: AbstractParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory
    }
}