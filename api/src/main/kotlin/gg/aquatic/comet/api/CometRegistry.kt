package gg.aquatic.comet.api

import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.macro.MacroParser

val macroParsers: MutableMap<String, MacroParser> = mutableMapOf()
val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()
val componentParsers: MutableMap<String, BaseComponentParser> = mutableMapOf()

val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()