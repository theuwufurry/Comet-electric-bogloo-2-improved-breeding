package gg.aquatic.comet.emitter.recursive

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.Emitter
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.ParticleJsonParser.recursiveEmitterComponentParser
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.bukkit.Location
import org.joml.Vector3d

//async between sub emitters means data is being edited weirdly

//add all unrealized emitters with unrealized recursive emitter components
class RecursiveEmitterComponent(private val unrealizedEmitterID: String, private val myEmitterData: EmitterData) :
    PostInit {
    companion object : ComponentParser<RecursiveEmitterComponent> {
        init {
            recursiveEmitterComponentParser = "sub_emitter" to RecursiveEmitterComponent
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): RecursiveEmitterComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val unrealizedEmitterID = engine.compile(jsonObject.expression("emitter") ?: return null).eval() as String

            return RecursiveEmitterComponent(
                unrealizedEmitterID, emitterData
            )
        }
    }

    private lateinit var unrealizedEmitter: UnrealizedEmitter

    override fun realize() {
        unrealizedEmitter = ParticleJsonParser.jsonUnrealizedEmitters[unrealizedEmitterID]!!
    }

    fun updateEmitter(otherEmitterData: EmitterData, otherParticleData: ParticleData): Emitter {
        myEmitterData.copyFrom(otherEmitterData)
        if (otherParticleData.age == 0.0) {
            val v = Vector3d(otherParticleData.origin).add(otherParticleData.relativePosition)
            otherParticleData.emitter = unrealizedEmitter.realize(Location(myEmitterData.world, v.x, v.y, v.z))
            return otherParticleData.emitter!!
        }

        val newPos = Vector3d(otherParticleData.origin).add(otherParticleData.relativePosition)
        otherParticleData.emitter!!.setPos(newPos.x, newPos.y, newPos.z)
        return otherParticleData.emitter!!
    }
}