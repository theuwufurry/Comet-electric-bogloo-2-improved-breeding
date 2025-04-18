package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.environment.*
import gg.aquatic.comet.api.parsing.PreInitComponentParser
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.macro.Macro

class EnvironmentDataComponent(
    val data: Map<String, Datum<*, *>> = mapOf()
) : PreInitComponent {
    override fun init(otherEmitterData: EmitterData, environmentData: EnvironmentData) {
        for ((key, value) in data) {
            environmentData.data.putIfAbsent(key, value)
        }
    }

    companion object : PreInitComponentParser {
        override val id: String = "default_environment_data"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): PreInitComponent? {
            val root = jsonElement.asJsonObjectOrNull() ?: return null
            val data: MutableMap<String, Datum<*, *>> = mutableMapOf()

            for ((key, element) in root.entrySet()) {
                if (!tryParseAsColor(key, element, data)) {
                    if (!element.isJsonPrimitive) continue
                    element as JsonPrimitive

                    if (element.isBoolean) {
                        data += key to DatumBool(element.asBoolean)
                    }

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