package com.ixume.particleemitter.parsing.macro

import com.google.gson.JsonObject

object MacrosParser {
    val macroParsers: MutableMap<String, MacroParser> = mutableMapOf()

    init {
        macroParsers += "hermite" to HermiteParser
    }

    fun parseMacros(jsonObject: JsonObject): Map<String, Macro> {
        return jsonObject.asMap().mapNotNull { (name, obj) ->
            (if (obj.isJsonPrimitive) {
                Pair(name, Macro(obj.asString, null))
            } else {
                Pair(name, parseMacro(name, obj.asJsonObject))
            })
        }.associateBy({ it.first }, { it.second!! })
    }

    private fun parseMacro(name: String, jsonObject: JsonObject): Macro? {
        if ("type" !in jsonObject.keySet()) return null
        val parser: MacroParser = macroParsers[jsonObject.getAsJsonPrimitive("type").asString]
            ?: return null
        return parser.parse(name, jsonObject)
    }
}

interface MacroParser {
    fun parse(name: String, jsonObject: JsonObject): Macro?
}

data class Macro(val to: String, val binding: Pair<String, Any>?)