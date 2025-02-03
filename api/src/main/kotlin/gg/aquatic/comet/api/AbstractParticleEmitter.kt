package gg.aquatic.comet.api

import gg.aquatic.comet.api.parsing.AbstractParticleJsonParser
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory


abstract class AbstractParticleEmitter: JavaPlugin() {
    companion object {
        lateinit var INSTANCE: AbstractParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory
        lateinit var particleJsonParser: AbstractParticleJsonParser
    }
}