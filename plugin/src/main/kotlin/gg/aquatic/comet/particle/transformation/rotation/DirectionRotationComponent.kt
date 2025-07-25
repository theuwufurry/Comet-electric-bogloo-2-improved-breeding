package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
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
) : ParticleComponent, RotationComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "direction_rotation"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): DirectionRotationComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return DirectionRotationComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros) ?: return null,
                engine.compile(jsonObject.expression("y") ?: return null, macros) ?: return null,
                engine.compile(jsonObject.expression("z") ?: return null, macros) ?: return null,
                engine.compile(jsonObject.expression("angle") ?: return null, macros) ?: return null,
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val dir = Vector3f(
            (axisXScript.eval() as Number).toFloat(),
            (axisYScript.eval() as Number).toFloat(),
            (axisZScript.eval() as Number).toFloat(),
        ).normalize()

        otherParticleData.rotation = otherEmitterData.emitter!!.applyEmitterRotation(
            Quaternionf()
                .rotateTo(Vector3f(0f, 0f, 1f), dir)
                .rotateZ((angleScript.eval() as Number).toFloat())
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}