package gg.aquatic.comet

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
    }
}

fun <T> T.applyIf(condition: Boolean, action: T.() -> Unit): T {
    if (condition) action()
    return this
}