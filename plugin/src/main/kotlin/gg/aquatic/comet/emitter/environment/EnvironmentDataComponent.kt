package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.macro.Macro

class EnvironmentDataComponent(
    val data: Map<String, Any> = mapOf()
) : EmitterComponent{
    override fun init(otherEmitterData: EmitterData) {
        for ((key, value) in data) {
            otherEmitterData.variable.putIfAbsent(key, value)
        }
    }

    override fun execute(otherEmitterData: EmitterData) {
    }

    override fun die(otherEmitterData: EmitterData) {
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "default_environment_data" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val root = jsonElement.asJsonObjectOrNull() ?: return null
            val data: MutableMap<String, Any> = mutableMapOf()

            for ((key, element) in root.entrySet()) {
                if (!tryParseAsColor(key, element, data)) {
                    if (!element.isJsonPrimitive) continue
                    element as JsonPrimitive

                    if (element.isString) {
                        data += key to element.asString
                    }

                    if (element.isNumber) {
                        data += key to element.asNumber
                    }
                }
            }

            return EnvironmentDataComponent(data)
        }
    }
}