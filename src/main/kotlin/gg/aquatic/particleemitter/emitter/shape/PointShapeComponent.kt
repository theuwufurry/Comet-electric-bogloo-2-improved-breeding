package gg.aquatic.particleemitter.emitter.shape

import com.google.gson.JsonElement
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class PointShapeComponent(private val xOffset: CompiledScript, private val yOffset: CompiledScript, private val zOffset: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ShapeComponent {
    companion object : ComponentParser<ShapeComponent> {
        init {
            ParticleJsonParser.shapeComponentParsers += "emitter_shape_point" to this
        }

        override fun parse(jsonElement: JsonElement): PointShapeComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData()
            val particleData = ParticleData()
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
            val offsetVector = jsonElement.asJsonObject?.get("offset")?.asJsonArray ?: return null
            return PointShapeComponent(
                (engine as Compilable).compile(offsetVector.get(0)?.asString ?: return null),
                engine.compile(offsetVector.get(1)?.asString ?: return null),
                engine.compile(offsetVector.get(2)?.asString ?: return null),
                emitterData,
                particleData)
        }
    }

    override fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double)
    }
}