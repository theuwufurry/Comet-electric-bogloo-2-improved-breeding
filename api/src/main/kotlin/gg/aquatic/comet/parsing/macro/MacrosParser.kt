package gg.aquatic.comet.parsing.macro

import com.google.gson.JsonObject
import gg.aquatic.comet.api.parsing.macro.Macro

object MacrosParser {
    val macroParsers: MutableMap<String, MacroParser> = mutableMapOf()
}

interface MacroParser {
    fun parse(name: String, jsonObject: JsonObject): Macro?
}