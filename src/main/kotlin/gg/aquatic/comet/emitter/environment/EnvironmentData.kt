package gg.aquatic.comet.emitter.environment

import com.google.gson.JsonParser
import gg.aquatic.comet.parsing.asNumberOrNull
import java.awt.Color

class EnvironmentData(
    val size: Double = 1.0,
    val data: MutableMap<String, Any> = mutableMapOf()
)

fun String.parseEnvironmentData(): EnvironmentData {
    val root = JsonParser.parseString(this).asJsonObject
    val size = root["size"]?.asNumberOrNull()?.toDouble() ?: 1.0
    val data: MutableMap<String, Any> = mutableMapOf()

    for ((key, element) in root.entrySet()) {
        if (!(element.isJsonPrimitive && element.asJsonPrimitive.isString)) continue
        val color = element.asString?.toRGBA() ?: continue

        data += key to color
    }

    return EnvironmentData(size, data)
}

fun String.toRGBA(): Color? {
    val i = try { Integer.decode(this) } catch (ignored: NumberFormatException) { return null }
    return when (length) {
        7 -> { Color((i ushr 16) and 0xFF, (i ushr 8) and 0xFF, i and 0xFF) }
        9 -> { Color((i ushr 16) and 0xFF, (i ushr 8) and 0xFF, i and 0xFF, (i ushr 24) and 0xFF) }
        else -> { null }
    }
}
