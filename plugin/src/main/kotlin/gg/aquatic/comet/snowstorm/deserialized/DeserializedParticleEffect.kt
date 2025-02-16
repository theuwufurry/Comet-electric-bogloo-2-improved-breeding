package gg.aquatic.comet.snowstorm.deserialized

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.component.variable.Randoms
import gg.aquatic.comet.snowstorm.transpilation.JavascriptPrinter
import gg.aquatic.comet.snowstorm.transpilation.Parser
import gg.aquatic.comet.snowstorm.transpilation.Scanner
import java.io.File
import kotlin.math.max

class DeserializedParticleEffect {
    val components: MutableList<DeserializedComponent> = mutableListOf()
    val curves: MutableList<DeserializedComponent> = mutableListOf()

    fun serializeInto(output: File) {
        val root = JsonObject()
        val componentsObj = JsonObject()
        val macrosObj = JsonObject()
        root.add("components", componentsObj)
        root.add("macros", macrosObj)
        for (component in components) {
            component.serialize(root)
        }

        for (curve in curves) {
            curve.serialize(root)
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val string = gson.toJson(root)
        output.writeText(string)
    }

    fun parseMolang(
        molang: String,
        toTicks: Boolean = false,
    ): String {
        val scanner = Scanner(molang)
        val (tokens, errors) = scanner.scanTokens()
        for (error in errors) {
            println("Error: $error")
        }

        val parser = Parser(tokens, this)
        val expr = parser.parse()
        var js = JavascriptPrinter.print(expr)
        if (toTicks) {
            js = "($js) * 20.0"
        }

        return js
    }

    fun specifyRandoms(
        emitter: Int = 0,
        particle: Int = 0,
    ) {
        if (emitter == 0 && particle == 0) return

        val deserializedRandoms = components.firstOrNull { it is Randoms } as? Randoms

        if (deserializedRandoms != null) {
            deserializedRandoms.emitter = max(emitter, deserializedRandoms.emitter)
            deserializedRandoms.particle = max(particle, deserializedRandoms.particle)
        } else {
            components += Randoms(emitter, particle)
        }
    }
}

fun JsonElement.primitiveString(): String {
    return if (!isJsonPrimitive)
        toString()
    else if (asJsonPrimitive.isString)
        asJsonPrimitive.asString
    else if (asJsonPrimitive.isNumber)
        asJsonPrimitive.asNumber.toString()
    else toString()
}