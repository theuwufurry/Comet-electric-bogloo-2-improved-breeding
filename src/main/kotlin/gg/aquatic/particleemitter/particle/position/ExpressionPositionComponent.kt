package gg.aquatic.particleemitter.particle.position

import com.google.gson.JsonElement
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.parsing.expression
import gg.aquatic.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ExpressionPositionComponent(private val xOffset: CompiledScript, private val yOffset: CompiledScript, private val zOffset: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : PositionComponent {
    companion object : ComponentParser<PositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "expression_relative_position" to this
        }

        override fun parse(jsonElement: JsonElement): PositionComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData()
            val particleData = ParticleData()
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
            val jsonObject = jsonElement.asJsonObject
            return ExpressionPositionComponent(
                (engine as Compilable).compile(jsonObject.expression("x") ?: return null),
                (engine as Compilable).compile(jsonObject.expression("y") ?: return null),
                (engine as Compilable).compile(jsonObject.expression("z") ?: return null),
                emitterData,
                particleData)
        }
    }

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double)
    }
}