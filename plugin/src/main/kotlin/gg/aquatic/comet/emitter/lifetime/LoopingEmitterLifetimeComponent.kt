package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.expression
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript

class LoopingEmitterLifetimeComponent(
    private val activeTime: CompiledScript?,
    private val sleepTime: CompiledScript?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override val priority = -1

    private val cachedTimesMap: MutableMap<UUID, CachedTimes> = ConcurrentHashMap()

    override fun init(otherEmitterData: EmitterData) {
        myEmitterData.copyFrom(otherEmitterData)
        cachedTimesMap[otherEmitterData.id] = CachedTimes(
            activeTime?.let { (it.eval() as Number).toInt() } ?: 10,
            sleepTime?.let { (it.eval() as Number).toInt() } ?: 0
        )
    }

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
        val cachedTimes = cachedTimesMap[otherEmitterData.id]!!
        if (otherEmitterData.isActive) {
            if (otherEmitterData.age > cachedTimes.activeTime) {
                cachedTimesMap[otherEmitterData.id] = CachedTimes(
                    activeTime?.let { (it.eval() as Number).toInt() } ?: 10,
                    sleepTime?.let { (it.eval() as Number).toInt() } ?: 0
                )

                if (cachedTimes.sleepTime > 0) {
                    otherEmitterData.isActive = false
                }

                otherEmitterData.age = 0.0
            }
        } else {
            if (otherEmitterData.age > cachedTimes.sleepTime) {
                otherEmitterData.isActive = true
                otherEmitterData.age = 0.0
            }
        }
    }

    override fun die(otherEmitterData: EmitterData) {}

    data class CachedTimes(
        val activeTime: Int,
        val sleepTime: Int
    )

    companion object : BaseComponentParser {
        override val id: String = "looping_emitter_lifetime"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): LoopingEmitterLifetimeComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return LoopingEmitterLifetimeComponent(
                jsonObject.expression("active_time")?.let { engine.compile(it, macros) },
                jsonObject.expression("sleep_time")?.let { engine.compile(it, macros) },
                emitterData
            )
        }
    }
}