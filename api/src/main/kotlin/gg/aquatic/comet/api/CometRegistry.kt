package gg.aquatic.comet.api

import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.AbstractParticleJsonParser
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.macro.MacroParser

object CometRegistry {
    val macroParsers: MutableMap<String, MacroParser> = mutableMapOf()
    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()
    val componentParsers: MutableMap<String, BaseComponentParser> = mutableMapOf()

    val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()

    lateinit var jsonParser: AbstractParticleJsonParser

    fun unrealizedEmitterByID(id: String): AbstractUnrealizedEmitter? {
        return jsonParser.getUnrealizedEmitterByID(id)
    }

    fun BaseComponentParser.register() {
        componentParsers += this.id to this
    }

    fun ComponentParser<out RateComponent>.registerRate() {
        rateComponentParsers += this.id to this
    }

    fun ComponentParser<out UpdateFrequencyComponent>.registerUpdate() {
        updateFrequencyParsers += this.id to this
    }
}