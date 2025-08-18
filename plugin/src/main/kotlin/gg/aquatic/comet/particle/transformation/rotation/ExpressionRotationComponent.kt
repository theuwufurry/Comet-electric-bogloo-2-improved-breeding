package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
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
        ): Result<ExpressionRotationComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val rotations: MutableList<Quaternionf.() -> Unit> = mutableListOf()

            for ((id, _) in jsonObject.entrySet()) {
                if ("x_local" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateLocalX(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }

                if ("y_local" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateLocalY(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }

                if ("z_local" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateLocalZ(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }

                if ("x" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateX(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }

                if ("y" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateY(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }

                if ("z" in id) {
                    val sc = jsonObject.getExprOrNull(id)!!
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) })
                    rotations += rot@{
                        rotateZ(sc.eval().getOrPrint(emitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return@rot)
                    }

                    continue
                }
            }

            return Result.success(
                ExpressionRotationComponent(
                    rotations,
                    particleData, emitterData
                )
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