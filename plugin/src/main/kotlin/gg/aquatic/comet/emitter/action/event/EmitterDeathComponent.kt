package gg.aquatic.comet.emitter.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.AbstractAction
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.action.Action

class EmitterDeathComponent(
    private val action: AbstractAction
) : EmitterComponent, PostInit {
    override val priority = 0
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {}

    override fun die(otherEmitterData: EmitterData) {
        action.execute(
            ActionContext(
                otherEmitterData, null,
                otherEmitterData.emitter!!.pose,
            )
        )
    }

    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize(unrealizedEmitter) }
    }

    companion object : BaseComponentParser {
        override val id: String = "on_emitter_death"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return EmitterDeathComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}