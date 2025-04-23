package gg.aquatic.comet.api.emitter.environment

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.api.emitter.VariableMutableMap
import gg.aquatic.comet.api.parsing.asNumberOrNull
import java.awt.Color

class EnvironmentData(
    val size: Double = 1.0,
    val data: MutableMap<String, Any> = VariableMutableMap(mutableMapOf())
) {
    fun clone(): EnvironmentData {
        return EnvironmentData(size, (data as VariableMutableMap).clone())
    }
}

interface Datum<V, T : Datum<V, T>> {
    val value: V
    fun clone(): T
}

class DatumColor(
    override val value: Color
) : Datum<Color, DatumColor> {
    override fun clone(): DatumColor {
        return DatumColor(Color(value.red, value.green, value.blue, value.alpha))
    }
}

class DatumStr(
    override val value: String
) : Datum<String, DatumStr> {
    override fun clone(): DatumStr {
        return DatumStr(value)
    }
}

class DatumNum(
    override val value: Number
) : Datum<Number, DatumNum> {
    override fun clone(): DatumNum {
        return DatumNum(value)
    }
}

class DatumBool(
    override val value: Boolean
) : Datum<Boolean, DatumBool> {
    override fun clone(): DatumBool {
        return DatumBool(value)
    }
}

fun tryParseAsColor(key: String, element: JsonElement, data: MutableMap<String, Datum<*, *>>): Boolean {
    if (!(element.isJsonPrimitive && element.asJsonPrimitive.isString)) return false
    val color = element.asString?.toRGBA() ?: return false

    data += key to DatumColor(color)
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

fun String.parseEnvironmentData(): EnvironmentData {
    val root = JsonParser.parseString(this).asJsonObject
    val size = root["size"]?.asNumberOrNull()?.toDouble() ?: 1.0
    val data: MutableMap<String, Datum<*, *>> = mutableMapOf()

    for ((key, element) in root.entrySet()) {
        if (!tryParseAsColor(key, element, data)) {
            if (!element.isJsonPrimitive) continue
            element as JsonPrimitive

            if (element.isString) {
                data += key to DatumStr(element.asString)
            }

            if (element.isNumber) {
                data += key to DatumNum(element.asNumber)
            }

            if (element.isBoolean) {
                data += key to DatumBool(element.asBoolean)
            }
        }
    }

    return EnvironmentData(size, VariableMutableMap(data))
}
