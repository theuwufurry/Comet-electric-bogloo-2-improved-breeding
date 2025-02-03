package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.macro.Macro

class EmitterDieSubAction : SubAction {
    override fun execute(context: ActionContext) {
        context.otherEmitterData.dead = true
    }

    companion object : ComponentParser<EmitterDieSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): EmitterDieSubAction? {
            if (jsonElement.isJsonPrimitive && jsonElement.asString == "emitter_die") {
                return EmitterDieSubAction()
            }

            return null
        }
    }
}