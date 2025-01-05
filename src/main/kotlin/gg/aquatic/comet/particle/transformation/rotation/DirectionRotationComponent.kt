package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.joml.Quaternionf
import org.joml.Vector3f
import javax.script.CompiledScript

class DirectionRotationComponent(
    private val axisXScript: CompiledScript,
    private val axisYScript: CompiledScript,
    private val axisZScript: CompiledScript,
    private val angleScript: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : RotationComponent {
    companion object : ComponentParser<DirectionRotationComponent> {
        init {
            ParticleJsonParser.rotationComponentParsers += "direction_rotation" to this
        }

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): DirectionRotationComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return DirectionRotationComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                engine.compile(jsonObject.expression("angle") ?: return null, macros),
                particleData, emitterData
            )
        }
    }

    override fun rotation(otherEmitterData: EmitterData, otherParticleData: ParticleData): Quaternionf {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val dir = Vector3f(
            (axisXScript.eval() as Number).toFloat(),
            (axisYScript.eval() as Number).toFloat(),
            (axisZScript.eval() as Number).toFloat(),
        ).normalize()

        return Quaternionf()
            .rotateTo(Vector3f(0f, 0f, 1f), dir)
            .rotateZ((angleScript.eval() as Number).toFloat())
    }
}