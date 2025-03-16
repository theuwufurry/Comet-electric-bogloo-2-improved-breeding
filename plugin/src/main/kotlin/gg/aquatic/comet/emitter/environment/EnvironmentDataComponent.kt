package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.environment.Datum
import gg.aquatic.comet.api.emitter.environment.DatumNum
import gg.aquatic.comet.api.emitter.environment.DatumStr
import gg.aquatic.comet.api.emitter.environment.tryParseAsColor
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.macro.Macro

class EnvironmentDataComponent(
    val data: Map<String, Datum<*, *>> = mapOf()
) : EmitterComponent {
    override val priority = 0
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
        override val id: String = "default_environment_data"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val root = jsonElement.asJsonObjectOrNull() ?: return null
            val data: MutableMap<String, Datum<*, *>> = mutableMapOf()

            for ((key, element) in root.entrySet()) {
                if (!tryParseAsColor(key, element, data)) {
                    if (!element.isJsonPrimitive) continue
                    element as JsonPrimitive

                    if (element.isString) {
                        data += key to DatumStr(element.asString)
                    }

                    if (element.isNumber) {
                        data += key to DatumNum(element.asNumber)
                    }
                }
            }

            return EnvironmentDataComponent(data)
        }
    }
}