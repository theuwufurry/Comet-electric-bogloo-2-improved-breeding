package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.position.PositionComponent
import org.joml.Vector3d
import javax.script.CompiledScript

class InitialExpressionPositionComponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            otherParticleData.relativePosition.add(Vector3d(
                (xOffset.eval() as Number).toDouble(),
                (yOffset.eval() as Number).toDouble(),
                (zOffset.eval() as Number).toDouble()
            ).rotate(otherEmitterData.rotation))
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "initial_expression_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): InitialExpressionPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return InitialExpressionPositionComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                emitterData, particleData
            )
        }
    }
}