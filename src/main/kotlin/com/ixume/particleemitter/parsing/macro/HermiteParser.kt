package com.ixume.particleemitter.parsing.macro

import com.google.gson.JsonObject
import org.joml.Vector2d
import kotlin.math.pow

object HermiteParser : MacroParser {
    override fun parse(name: String, jsonObject: JsonObject): Macro? {
        //output should be smth like
        //from: "size"
        //to: "size.eval(input)"
        //binding: "size" to a HermiteEvaluator ( instance which has preset nodes )
        val x = jsonObject.getAsJsonArray("x").asList()
        val y = jsonObject.getAsJsonArray("y").asList()
        val points: MutableList<Vector2d> = mutableListOf()
        for (i in x.indices) {
            points += Vector2d(x[i].asNumber.toDouble(), y[i].asNumber.toDouble())
        }

        val input: String = jsonObject.getAsJsonPrimitive("input")?.asString ?: return null
        return Macro("$name.eval($input)", Pair(name, HermiteEvaluator.fromPoints(points) ?: return null))
    }
}

//needs to store n - 1 degree 3 polynomial coefficients for every n points
//needs to also store n x values (for bounds for polynomials)
//doesn't technically need a "last" point
class HermiteEvaluator private constructor(private val polynomials: List<List<Double>>, private val domains: List<Double>) {
    companion object {
        fun fromPoints(points: List<Vector2d>): HermiteEvaluator? {
            if (points.size < 2) return null
            val slopes: MutableList<Double> = mutableListOf()
            slopes += (points[1].y - points[0].y) / (points[1].x - points[0].x)

            for (i in 1 until points.size - 1) {
                slopes += ((points[i].y - points[i - 1].y) / (points[i].x - points[i - 1].x) + (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x)) / 2.0
            }

            slopes += (points[points.size - 1].y - points[points.size - 2].y) / (points[points.size - 1].x - points[points.size - 2].x)

            val polynomials: MutableList<MutableList<Double>> = mutableListOf()

            for (i in 0 until points.size - 1) {
                val coefficients: MutableList<Double> = mutableListOf()
                coefficients += points[i].y
                coefficients += (points[i + 1].x - points[i].x) * slopes[i]
                coefficients += -3.0 * points[i].y + 3.0 * points[i + 1].y + 2.0 * (points[i].x - points[i + 1].x ) * slopes[i] + (points[i].x - points[i + 1].x) * slopes[i + 1]
                coefficients += 2.0 * points[i].y - 2.0 * points[i + 1].y + (slopes[i] + slopes[i + 1]) * (points[i + 1].x - points[i].x)
                polynomials += coefficients
            }

            val domains: MutableList<Double> = points.map { it.x }.toMutableList()

            return HermiteEvaluator(polynomials, domains)
        }
    }

    fun eval(input: Double): Double {
        var i = 0
        if (input > domains[0]) {
            while (input > domains[i + 1] && i + 2 < domains.size) i++
        }

        val coefficients = polynomials[i]
        val transformedInput = (input - domains[i]) / (domains[i + 1] - domains[i])
        val result =
            coefficients[0] + coefficients[1] * transformedInput + coefficients[2] * transformedInput.pow(2.0) + coefficients[3] * transformedInput.pow(3.0)
        return result
    }
}