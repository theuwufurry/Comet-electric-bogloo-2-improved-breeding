package gg.aquatic.comet.emitter.bundle

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro

class BundledEmitterComponent(private val unrealizedEmitterIDs: List<String>) : EmitterComponent, PostInit {
    private lateinit var unrealizedEmitters: List<UnrealizedEmitter>

    override fun realize() {
        unrealizedEmitters = unrealizedEmitterIDs.map {
            ParticleJsonParser.jsonUnrealizedEmitters[it]
                ?: throw NullPointerException("$it is not a valid emitter ID!")
        }
    }

    override fun init(otherEmitterData: EmitterData) {
        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.realize(otherEmitterData.location)
        }
    }

    override fun execute(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "bundled_emitters" to BundledEmitterComponent
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): BundledEmitterComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val array = jsonObject.getAsJsonArray("emitters") ?: return null

            val unrealizedEmitterIDs: MutableList<String> = mutableListOf()
            for (element in array) {
                if (!(element.isJsonPrimitive && element.asJsonPrimitive.isString)) continue
                val evaluatedID = engine.compile(element.asString, macros, true).eval() as String
                unrealizedEmitterIDs += evaluatedID
            }

            return BundledEmitterComponent(
                unrealizedEmitterIDs
            )
        }
    }
}