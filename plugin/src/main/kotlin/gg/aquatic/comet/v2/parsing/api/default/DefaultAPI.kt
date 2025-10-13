package gg.aquatic.comet.v2.parsing.api.default

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.particle.macro.CatmullEvaluator
import gg.aquatic.comet.udar.UdarBodyParent
import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper
import gg.aquatic.comet.v2.parsing.getMemberOrNull
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.util.concurrent.ConcurrentHashMap

object DefaultAPI {
    fun createSpriteData(id: String): SpriteData {
        return SpriteData(id)
    }

    fun sprite(id: String): SpriteData {
        return SpriteData(id)
    }

    fun light(sky: Int, block: Int): LightData {
        return LightData(sky, block)
    }

    fun parentOf(body: ActiveBody): UdarBodyParent {
        return UdarBodyParent(body)
    }

    fun parentOf(body: PhysicsBodyWrapper): UdarBodyParent {
        return UdarBodyParent(body.actual)
    }

    fun colorOf(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun colorOf(r: Int, g: Int, b: Int, a: Int): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun gradient(vararg stops: Value): Gradient {
        return Gradient(stops.map {
            ColorStop(
                r = it.getMember("r").asInt(),
                g = it.getMember("g").asInt(),
                b = it.getMember("b").asInt(),
                t = it.getMember("t").asDouble(),
                a = it.getMemberOrNull("a")?.asInt() ?: 255,
            )
        })
    }

    fun rate(rate: Double): RateProvider {
        return RateProvider(rate)
    }

    fun curve(options: Value): Curve {
        val data = options.getMember("data")
        val xs = mutableListOf<Double>()
        val ys = mutableListOf<Double>()
        var i = 0L
        val s = data.arraySize
        while (i < s) {
            val elem = data.getArrayElement(i)
            xs += elem.getArrayElement(0).asDouble()
            ys += elem.getArrayElement(1).asDouble()
            i++
        }

        return when (options.getMember("type").asString()) {
            "catmull" -> {
                CatmullCurve(CatmullEvaluator.Companion.fromPoints(ys, xs))
            }
           
            "linear" -> LinearCurve(xs.toDoubleArray(), ys.toDoubleArray())

            else -> throw IllegalArgumentException()
        }
    }

    val defaultBindings = ConcurrentHashMap<String, Any>().apply {
        this["comet"] = DefaultAPI
    }

    fun Context.loadAPI() {
        val bindings = getBindings("js")
        for ((key, obj) in defaultBindings) {
            bindings.putMember(key, obj)
        }
    }
}