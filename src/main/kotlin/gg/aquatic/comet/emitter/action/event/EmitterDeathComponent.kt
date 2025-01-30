package gg.aquatic.comet.emitter.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.PostInit
import gg.aquatic.comet.parsing.macro.Macro

class EmitterDeathComponent(
    private val action: Action
) : EmitterComponent, PostInit {
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {}

    override fun die(otherEmitterData: EmitterData) {
        action.execute(
            ActionContext(
                otherEmitterData, null,
                otherEmitterData.emitter!!.pose(),
            )
        )
    }

    override fun realize() {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize() }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_emitter_death" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return EmitterDeathComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}