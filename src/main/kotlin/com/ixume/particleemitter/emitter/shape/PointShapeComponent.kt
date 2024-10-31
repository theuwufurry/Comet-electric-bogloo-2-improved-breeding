package com.ixume.particleemitter.emitter.shape

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.compile
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.parsing.particleEngine
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class PointShapeComponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ShapeComponent {
    companion object : ComponentParser<PointShapeComponent> {
        init {
            ParticleJsonParser.shapeComponentParsers += "emitter_shape_point" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): PointShapeComponent? {
            val offsetVector = jsonElement.asJsonObject?.get("offset")?.asJsonArray ?: return null
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            return PointShapeComponent(
                engine.compile(offsetVector.get(0)?.asString ?: return null, macros),
                engine.compile(offsetVector.get(1)?.asString ?: return null, macros),
                engine.compile(offsetVector.get(2)?.asString ?: return null, macros),
                emitterData, particleData
            )
        }
    }

    override fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double).rotate(
            myEmitterData.rotation
        )
    }
}