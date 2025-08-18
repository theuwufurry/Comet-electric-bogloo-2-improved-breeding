package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LoopingEmitterLifetimeComponent(
    private val activeTime: Expr<Number>?,
    private val sleepTime: Expr<Number>?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override val priority = -1

    private val cachedTimesMap: MutableMap<UUID, CachedTimes> = ConcurrentHashMap()

    override fun init(otherEmitterData: EmitterData) {
        myEmitterData.copyFrom(otherEmitterData)
        cachedTimesMap[otherEmitterData.id] = CachedTimes(
            activeTime?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 10,
            sleepTime?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0,
        )
    }

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
        val cachedTimes = cachedTimesMap[otherEmitterData.id]!!
        if (otherEmitterData.isActive) {
            if (otherEmitterData.age > cachedTimes.activeTime) {
                cachedTimesMap[otherEmitterData.id] = CachedTimes(
                    activeTime?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 10,
                    sleepTime?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0,
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

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<LoopingEmitterLifetimeComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return Result.success(LoopingEmitterLifetimeComponent(
                jsonObject.getExprOrNull("active_time")?.constructExpr<Number>(
                        engine, macros
                    )?.fold(
                        { it },
                        { return Result.failure(it) }
                    ),
                jsonObject.getExprOrNull("sleep_time")?.constructExpr<Number>(
                    engine, macros
                )?.fold(
                    { it },
                    { return Result.failure(it) }
                ),
                emitterData
            ))
        }
    }
}