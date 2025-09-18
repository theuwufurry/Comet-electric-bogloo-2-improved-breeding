package gg.aquatic.comet.emitter.optimization

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asNumberOrNull
import gg.aquatic.comet.api.parsing.macro.Macro

class OptimizationIntervalComponent(
    val interval: Int,
) : EmitterComponent {
    override val priority: Int = 0
    override fun init(otherEmitterData: EmitterData) { 
        otherEmitterData.optimizationInterval = interval
    }
    
    override fun execute(otherEmitterData: EmitterData) { }
    override fun die(otherEmitterData: EmitterData) { }
    
    companion object : BaseComponentParser {
        override val id: String = "optimization_interval"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            val jsonObject = jsonElement.asJsonObject
            val i = jsonObject["interval"]?.asNumberOrNull()?.toInt() ?: 100
            
            return Result.success(OptimizationIntervalComponent(i))
        }
    }
}