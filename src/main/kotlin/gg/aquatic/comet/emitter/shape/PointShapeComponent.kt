package gg.aquatic.comet.emitter.shape

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
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
        return Vector3d(
            (xOffset.eval() as Number).toDouble(),
            (yOffset.eval() as Number).toDouble(),
            (zOffset.eval() as Number).toDouble()
        ).rotate(
            myEmitterData.rotation
        )
    }
}