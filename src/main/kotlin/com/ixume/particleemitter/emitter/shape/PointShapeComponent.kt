package com.ixume.particleemitter.emitter.shape

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import org.joml.Vector3d
import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript

class PointShapeComponent(private val xOffset: CompiledScript, private val yOffset: CompiledScript, private val zOffset: CompiledScript) : ShapeComponent {
    companion object {
        fun parse(jsonElement: JsonElement): PointShapeComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine as Compilable
            val offsetVector = jsonElement.asJsonObject?.get("offset")?.asJsonArray ?: return null
            return PointShapeComponent(
                engine.compile(offsetVector.get(0)?.asString ?: return null),
                engine.compile(offsetVector.get(1)?.asString ?: return null),
                engine.compile(offsetVector.get(2)?.asString ?: return null))
        }
    }

    override fun offset(emitterData: EmitterData, emitterBindings: Bindings): Vector3d {
        return Vector3d(xOffset.eval(emitterBindings) as Double, yOffset.eval(emitterBindings) as Double, zOffset.eval(emitterBindings) as Double)
    }
}