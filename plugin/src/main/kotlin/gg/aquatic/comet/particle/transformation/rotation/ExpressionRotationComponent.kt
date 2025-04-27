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

class ExpressionRotationComponent(
    private val rotations: List<Quaternionf.() -> Unit>,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, RotationComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "expression_rotation"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): ExpressionRotationComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val rotations: MutableList<Quaternionf.() -> Unit> = mutableListOf()

            for ((id, _) in jsonObject.entrySet()) {
                if ("x_local" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateLocalX((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }

                if ("y_local" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateLocalY((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }

                if ("z_local" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateLocalZ((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }

                if ("x" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateX((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }

                if ("y" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateY((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }

                if ("z" in id) {
                    val compiledScript = engine.compile(jsonObject.expression(id) ?: return null, macros)
                    rotations += {
                        rotateZ((compiledScript.eval() as Number).toFloat())
                    }

                    continue
                }
            }

            return ExpressionRotationComponent(
                rotations,
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val identity = Quaternionf()

        for (rotation in rotations) {
            identity.rotation()
        }

        otherParticleData.rotation = otherEmitterData.emitter!!.applyEmitterRotation(identity)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}