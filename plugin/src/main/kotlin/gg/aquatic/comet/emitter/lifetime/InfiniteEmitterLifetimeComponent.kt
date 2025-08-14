package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro

class InfiniteEmitterLifetimeComponent : EmitterComponent,
    EmitterLifetimeComponent {
    override val priority = -1

    override fun init(otherEmitterData: EmitterData) {
    }

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
    }

    override fun die(otherEmitterData: EmitterData) {
    }

    companion object : BaseComponentParser {
        override val id: String = "infinite_emitter_lifetime"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<InfiniteEmitterLifetimeComponent> {
            return Result.success(InfiniteEmitterLifetimeComponent())
        }
    }
}