package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.parsing.asNumberOrNull
import java.awt.Color

fun String.parseEnvironmentData(): EnvironmentData {
    val root = JsonParser.parseString(this).asJsonObject
    val size = root["size"]?.asNumberOrNull()?.toDouble() ?: 1.0
    val data: MutableMap<String, Any> = mutableMapOf()

    for ((key, element) in root.entrySet()) {
        if (!tryParseAsColor(key, element, data)) {
            if (!element.isJsonPrimitive) continue
            element as JsonPrimitive

            if (element.isString) {
                data += key to element.asString
            }

            if (element.isNumber) {
                data += key to element.asNumber
            }
        }
    }

    return EnvironmentData(size, data)
}

fun tryParseAsColor(key: String, element: JsonElement, data: MutableMap<String, Any>): Boolean {
    if (!(element.isJsonPrimitive && element.asJsonPrimitive.isString)) return false
    val color = element.asString?.toRGBA() ?: return false

    data += key to color
    return true
}

fun String.toRGBA(): Color? {
    val hex = substring(1)
    try {
        when (hex.length) {
            6 -> {
                val r = Integer.decode("#${hex.substring(0, 2)}")
                val g = Integer.decode("#${hex.substring(2, 4)}")
                val b = Integer.decode("#${hex.substring(4, 6)}")

                return Color(r, g, b)
            }

            8 -> {
                val a = Integer.decode("#${hex.substring(0, 2)}")
                val r = Integer.decode("#${hex.substring(2, 4)}")
                val g = Integer.decode("#${hex.substring(4, 6)}")
                val b = Integer.decode("#${hex.substring(6, 8)}")

                return Color(r, g, b, a)
            }

            else -> return null
        }
    } catch (ignored: NumberFormatException) {
        return null
    }
}
