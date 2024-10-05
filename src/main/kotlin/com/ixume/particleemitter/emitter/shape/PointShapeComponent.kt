package com.ixume.particleemitter.emitter.shape

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.emitter.lifetime.TimedEmitterLifetimeComponent
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class PointShapeComponent(private val xOffset: CompiledScript,
                          private val yOffset: CompiledScript,
                          private val zOffset: CompiledScript,
                          private val myEmitterData: EmitterData,
                          private val myParticleData: ParticleData) : ShapeComponent {
    companion object : ComponentParser<PointShapeComponent> {
        init {
            ParticleJsonParser.shapeComponentParsers += "emitter_shape_point" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<PointShapeComponent>? {
            val offsetVector = jsonElement.asJsonObject?.get("offset")?.asJsonArray ?: return null
            return UnrealizedPointShapeComponent(
                offsetVector.get(0)?.asString ?: return null,
                offsetVector.get(1)?.asString ?: return null,
                offsetVector.get(2)?.asString ?: return null,
                macros)
        }
    }

    override fun offset(otherParticleData: ParticleData): Vector3d {
        myParticleData.copyFrom(otherParticleData)
        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double).rotate(myEmitterData.rotation)
    }
}

class UnrealizedPointShapeComponent(private val xOffset: String,
                                    private val yOffset: String,
                                    private val zOffset: String,
                                    private val macros: Map<String, Macro>?) : UnrealizedComponent<PointShapeComponent> {
    override fun realizeComponent(emitterData: EmitterData): PointShapeComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return PointShapeComponent(
            engine.compile(xOffset, macros),
            engine.compile(yOffset, macros),
            engine.compile(zOffset, macros),
            emitterData,
            particleData)
    }
}