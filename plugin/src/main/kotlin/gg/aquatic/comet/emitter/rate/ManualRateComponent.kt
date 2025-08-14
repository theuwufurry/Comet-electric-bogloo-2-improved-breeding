package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import java.util.*
import kotlin.math.min

class ManualRateComponent(
    private val spawningMap: Map<Int, Expr<Number>>,
    private val maxParticles: Expr<Number>?,
    private val myEmitterData: EmitterData
) :
    RateComponent {

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val evaluatedMaxParticles = maxParticles?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: Integer.MAX_VALUE
        if (otherEmitterData.emitter!!.particles.size >= evaluatedMaxParticles) return 0

        val entry = spawningMap[otherEmitterData.age.toInt()]
        return if (entry != null) {
            min(entry.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0, evaluatedMaxParticles - otherEmitterData.emitter!!.particles.size)
        } else 0
    }

    companion object : ComponentParser<ManualRateComponent> {
        private val timesInput = Regex("^\\d+\\.\\.\\d+$")

        override val id: String = "emitter_rate_manual"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ManualRateComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val spawningMap: MutableMap<Int, Expr<Number>> = TreeMap()
            for ((timeString, _) in jsonObject.entrySet()) {
                timeString.toIntOrNull()?.let l@{ i ->
                    val amountScript = jsonObject.getExpr(timeString).fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros).fold({ it }, { return Result.failure(it) })
                    spawningMap[i] = amountScript
                } ?: run l@{
                    if (!timesInput.matches(timeString)) return@l

                    val parts = timeString.split("")

                    val start = parts[0].toInt()
                    val finish = parts[1].toInt()

                    val amountScript = jsonObject.getExpr(timeString).fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros).fold({ it }, { return Result.failure(it) })
                    for (time in start..finish) {
                        spawningMap[time] = amountScript
                    }
                }
            }

            return Result.success(ManualRateComponent(
                spawningMap,
                jsonObject.getExprOrNull("max_particles")
                    ?.constructExpr<Number>(engine, macros)
                    ?.fold({ it }, { return Result.failure(it) }),
                emitterData
            ))
        }
    }
}