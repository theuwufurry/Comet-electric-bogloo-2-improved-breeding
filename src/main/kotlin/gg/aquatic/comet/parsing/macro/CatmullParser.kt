package gg.aquatic.comet.parsing.macro

import com.google.gson.JsonObject
import gg.aquatic.comet.parsing.macro.Catmull.Companion.B_SPLINE_DEGREE
import gg.aquatic.comet.parsing.macro.Catmull.Companion.LAGRANGE_DEGREE
import org.joml.Vector3d
import kotlin.math.pow

object CatmullParser : MacroParser {
    override fun parse(name: String, jsonObject: JsonObject): Macro? {
        val times = jsonObject.getAsJsonArray("x").map { it.asDouble }
        val values = jsonObject.getAsJsonArray("y").map { it.asDouble }

        val input: String = jsonObject.getAsJsonPrimitive("input")?.asString ?: return null
        return Macro("$name.eval($input)", Pair(name, CatmullEvaluator.fromPoints(values, times) ?: return null))
    }
}

class CatmullEvaluator private constructor(
    private val points: List<Double>,
    private val times: List<Double>
) {
    fun eval(inputTime: Double): Double {
        val usableInputTime = inputTime.coerceIn(times[1], times[times.size - 2])

        for (timeIndex in times.indices) {
            val time = getTime(timeIndex + 1) ?: continue
            if (time > usableInputTime) {
                return spline(LAGRANGE_DEGREE + B_SPLINE_DEGREE, timeIndex, usableInputTime)
            }
        }

        return 0.0
    }

    private fun getTime(index: Int): Double? {
        if (index - 2 < 0) return null
        return times[index - 2]
    }

    private fun lagrangeTime(index: Int): Double {
        return getTime(index + 2)!!
    }

    private fun bsplineTime(index: Int): Double {
        return getTime(index)!!
    }

    private fun spline(height: Int, index: Int, time: Double): Double {
        return when {
            height == 0 -> points[index]
            height <= LAGRANGE_DEGREE -> {
                (
                        spline(
                            height - 1,
                            index - 1,
                            time
                        )
                        ) * (
                        ((lagrangeTime(index) - time) / (lagrangeTime(index) - lagrangeTime(index - height)))
                        ) + (
                        (
                                spline(
                                    height - 1,
                                    index,
                                    time
                                )
                                ) * ((time - lagrangeTime(index - height)) / (lagrangeTime(index) - lagrangeTime(index - height)))
                        )
            }

            else -> {
                (
                        spline(
                            height - 1,
                            index - 1,
                            time
                        )
                        ) * (
                        (bsplineTime(B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height) - time) / (bsplineTime(
                            B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height
                        ) - bsplineTime(index))
                        ) + (
                        (spline(height - 1, index, time)) * (
                                (time - bsplineTime(index)) / (bsplineTime(B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height) - bsplineTime(
                                    index
                                ))
                                )
                        )
            }
        }
    }

    companion object {
        private const val EPSILON = 0.0001
        fun fromPoints(points: List<Double>, inputTimes: List<Double>): CatmullEvaluator? {
            require(points.size == inputTimes.size)
            val alteredPoints: MutableList<Double> = mutableListOf<Double>().also {
                it += points.first()
                it += points
                it += points.last()
            }

//            val times = (0 until alteredPoints.size).map { it.toDouble() }.toList()
            val alteredTimes: MutableList<Double> = mutableListOf<Double>().also {
                it += inputTimes[0] - (inputTimes[1] - inputTimes[0])
                it += inputTimes
                it += inputTimes.last() + (inputTimes.last() - inputTimes[inputTimes.size - 2])
            }

            return CatmullEvaluator(alteredPoints, alteredTimes)
        }
    }
}

class Catmull(
    private val points: List<Vector3d>
) {
    init {
        require(points.size >= 4)
    }

    private val times: List<Double> = generateTimes()

    private fun generateTimes(): List<Double> {
        val times: MutableList<Double> = mutableListOf()
        times += 0.0
        for (i in 1 until points.size) {
            times.add(i, points[i].distance(points[i - 1]).pow(ALPHA) + times[i - 1])
        }

        return times
    }

    fun evaluate(inputTime: Double): Vector3d? {
        if (inputTime !in times[1]..times[times.size - 2]) return null

        for (timeIndex in times.indices) {
            val time = getTime(timeIndex) ?: continue
            if (time > inputTime) {
                return spline(LAGRANGE_DEGREE + B_SPLINE_DEGREE, timeIndex - 1, inputTime)
            }
        }

        return null
    }

    private fun getTime(index: Int): Double? {
        if (index - 2 < 0) return null
        return times[index - 2]
    }

    private fun lagrangeTime(index: Int): Double {
        return getTime(index + 2)!!
    }

    private fun bsplineTime(index: Int): Double {
        return getTime(index)!!
    }

    private fun spline(height: Int, index: Int, time: Double): Vector3d {
        return when {
            height == 0 -> points[index]
            height <= LAGRANGE_DEGREE -> {
                Vector3d(
                    spline(
                        height - 1,
                        index - 1,
                        time
                    )
                ).mul(
                    ((lagrangeTime(index) - time) / (lagrangeTime(index) - lagrangeTime(index - height)))
                ).add(
                    Vector3d(
                        spline(
                            height - 1,
                            index,
                            time
                        )
                    ).mul((time - lagrangeTime(index - height)) / (lagrangeTime(index) - lagrangeTime(index - height)))
                )
            }

            else -> {
                Vector3d(
                    spline(
                        height - 1,
                        index - 1,
                        time
                    )
                ).mul(
                    (bsplineTime(B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height) - time) / (bsplineTime(
                        B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height
                    ) - bsplineTime(index))
                ).add(
                    Vector3d(spline(height - 1, index, time)).mul(
                        (time - bsplineTime(index)) / (bsplineTime(B_SPLINE_DEGREE + LAGRANGE_DEGREE + 1 + index - height) - bsplineTime(
                            index
                        ))
                    )
                )
            }
        }
    }

    companion object {
        const val ALPHA = 0.5
        const val LAGRANGE_DEGREE = 1
        const val B_SPLINE_DEGREE = 2
    }
}
