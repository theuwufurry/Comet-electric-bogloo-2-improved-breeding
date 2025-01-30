package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonElement
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
            val jsonObject = jsonElement.asJsonObjectOrNull() ?: return null
            val data: MutableMap<String, Any> = mutableMapOf()

            for ((key, element) in jsonObject.entrySet()) {
                if (!(element.isJsonPrimitive && element.asJsonPrimitive.isString)) continue
                val color = element.asString?.toRGBA() ?: continue

                data += key to color
            }

            return EnvironmentDataComponent(data)
        }
    }
}