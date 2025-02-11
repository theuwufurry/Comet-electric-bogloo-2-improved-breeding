package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.expression
import java.util.*
import javax.script.CompiledScript
import kotlin.math.min

class ManualRateComponent(
    private val spawningMap: Map<Int, CompiledScript>,
    private val maxParticles: CompiledScript?,
    private val myEmitterData: EmitterData) :
    RateComponent {

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val evaluatedMaxParticles = maxParticles?.let {
            (it.eval() as Number).toInt()
        } ?: Integer.MAX_VALUE
        if (otherEmitterData.emitter!!.particles.size >= evaluatedMaxParticles) return 0

        val entry = spawningMap[otherEmitterData.age.toInt()]
        return if (entry != null) {
            min((entry.eval() as Number).toInt(), evaluatedMaxParticles - otherEmitterData.emitter!!.particles.size)
        } else 0
    }

    companion object : ComponentParser<ManualRateComponent> {
        private val timesInput = Regex("^\\d+\\.\\.\\d+$")

        override val id: String = "emitter_rate_manual"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ManualRateComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val spawningMap: MutableMap<Int, CompiledScript> = TreeMap()
            for ((timeString, _) in jsonObject.entrySet()) {
                timeString.toIntOrNull()?.let l@{
                    val amountScript = engine.compile(jsonObject.expression(timeString) ?: return@l, macros)
                    spawningMap[it] = amountScript
                } ?: run l@{
                    if (!timesInput.matches(timeString)) return@l

                    val parts = timeString.split("")

                    val start = parts[0].toInt()
                    val finish = parts[1].toInt()

                    val amountScript = engine.compile(jsonObject.expression(timeString) ?: return@l, macros)
                    for (time in start..finish) {
                        spawningMap[time] = amountScript
                    }
                }
            }

            return ManualRateComponent(
                spawningMap,
                jsonObject.expression("max_particles")?.let { engine.compile(it, macros) },
                emitterData
            )
        }
    }
}