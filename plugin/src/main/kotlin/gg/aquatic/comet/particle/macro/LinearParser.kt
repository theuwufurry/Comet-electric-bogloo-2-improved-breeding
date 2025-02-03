package gg.aquatic.comet.particle.macro

import com.google.gson.JsonObject
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.macro.MacroParser
import org.joml.Vector2d

object LinearParser : MacroParser {
    override fun parse(name: String, jsonObject: JsonObject): Macro? {
        val x = jsonObject.getAsJsonArray("x").asList()
        val y = jsonObject.getAsJsonArray("y").asList()
        val points: MutableList<Vector2d> = mutableListOf()
        for (i in x.indices) {
            points += Vector2d(x[i].asNumber.toDouble(), y[i].asNumber.toDouble())
        }

        val input: String = jsonObject.getAsJsonPrimitive("input")?.asString ?: return null
        return Macro("$name.eval($input)", Pair(name, LinearEvaluator(points)))
    }
}

class LinearEvaluator(private val points: List<Vector2d>) {
    fun eval(input: Double): Double {
        if (input <= points.first().x) return points.first().y
        if (input >= points.last().x) return points.last().y

        var prev = points.first()
        for (point in points) {
            if (point.x > input) {
                val factor = (input - prev.x) / (point.x - prev.x)
                return point.y * factor + (1.0 - factor) * prev.y
            }

            prev = point
        }

        return points.last().y
    }
}