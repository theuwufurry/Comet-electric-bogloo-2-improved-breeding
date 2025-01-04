package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.ComponentResult
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d
import java.util.*
import javax.script.CompiledScript

class ExpressionPositionComponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    private val oldOutputMap: MutableMap<UUID, Vector3d> = mutableMapOf()

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d> {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val oldResult = oldOutputMap.getOrPut(otherParticleData.id) { Vector3d() }
        val newResult = Vector3d(
            (xOffset.eval() as Number).toDouble(),
            (yOffset.eval() as Number).toDouble(),
            (zOffset.eval() as Number).toDouble()
        )
        oldOutputMap[otherParticleData.id] = newResult
        val newPos = Vector3d(otherParticleData.relativePosition).add(Vector3d(newResult).sub(oldResult))
        return ComponentResult(newPos.rotate(myEmitterData.rotation))
    }

    companion object : ComponentParser<ExpressionPositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "expression_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionPositionComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                emitterData, particleData
            )
        }
    }
}