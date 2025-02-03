package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.ParticleJsonParser

class InfiniteEmitterLifetimeComponent : EmitterComponent,
    EmitterLifetimeComponent {
    override fun init(otherEmitterData: EmitterData) {
    }

    override fun execute(otherEmitterData: EmitterData) {
    }

    override fun die(otherEmitterData: EmitterData) {
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "infinite_emitter_lifetime" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): InfiniteEmitterLifetimeComponent {
            return InfiniteEmitterLifetimeComponent()
        }
    }
}