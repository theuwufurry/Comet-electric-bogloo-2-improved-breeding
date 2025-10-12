package gg.aquatic.comet.v2.parsing.api.default

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

data class ColorStop(val r: Int, val g: Int, val b: Int, val t: Double, val a: Int) : Comparable<ColorStop> {
    override fun compareTo(other: ColorStop): Int = t.compareTo(other.t)
}

class Gradient(stops: List<ColorStop>) : ProxyObject {
    private val sortedStops: List<ColorStop> = stops.sorted()

    init {
        require(sortedStops.isNotEmpty()) { "Gradient must have at least one color stop" }
    }

    fun color(t: Double): Int {
        if (sortedStops.size == 1) {
            return packColor(sortedStops[0])
        }

        val minT = sortedStops.first().t
        val maxT = sortedStops.last().t
        val clampedT = t.coerceIn(minT, maxT)

        for (i in 0 until sortedStops.size - 1) {
            val start = sortedStops[i]
            val end = sortedStops[i + 1]
            if (clampedT in start.t..end.t) {
                val fraction = (clampedT - start.t) / (end.t - start.t)
                val r = lerp(start.r, end.r, fraction)
                val g = lerp(start.g, end.g, fraction)
                val b = lerp(start.b, end.b, fraction)
                val a = lerp(start.a, end.a, fraction)
                return (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return packColor(sortedStops.last())
    }

    private fun lerp(start: Int, end: Int, fraction: Double): Int {
        return (start + fraction * (end - start)).toInt().coerceIn(0, 255)
    }

    private fun packColor(stop: ColorStop): Int {
        return (stop.a shl 24) or (stop.r shl 16) or (stop.g shl 8) or stop.b
    }

    private val color = ProxyExecutable { args ->
        color(args[0].asDouble())
    }

    private val members = arrayOf("color")

    override fun getMember(key: String): Any? {
        return when (key) {
            "color" -> color
            else -> null
        }
    }

    override fun getMemberKeys(): Any = members

    override fun hasMember(key: String): Boolean = key in members

    override fun putMember(key: String, value: Value?) {
        throw UnsupportedOperationException()
    }
}