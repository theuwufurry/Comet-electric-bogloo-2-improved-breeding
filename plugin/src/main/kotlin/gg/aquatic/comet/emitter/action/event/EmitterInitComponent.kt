package gg.aquatic.comet.emitter.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.parsing.ParticleJsonParser

class EmitterInitComponent(
    private val action: Action
) : EmitterComponent, PostInit {
    override fun init(otherEmitterData: EmitterData) {
        action.execute(
            ActionContext(
                otherEmitterData, null,
                otherEmitterData.emitter!!.pose
            )
        )
    }

    override fun execute(otherEmitterData: EmitterData) {
    }

    override fun die(otherEmitterData: EmitterData) {}

    override fun realize() {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize() }
    }

    companion object : BaseComponentParser {
        override val id: String = "on_emitter_init"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return EmitterInitComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}