package gg.aquatic.comet.snowstorm.deserialized

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.component.variable.Randoms
import gg.aquatic.comet.snowstorm.deserialized.curve.CatmullCurve
import gg.aquatic.comet.snowstorm.transpilation.*
import gg.aquatic.comet.snowstorm.transpilation.expression.BinaryExpr
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import java.io.File
import kotlin.math.max

class DeserializedParticleEffect {
    val components: MutableList<DeserializedComponent> = mutableListOf()
    val curves: MutableList<CatmullCurve> = mutableListOf()

    fun serializeInto(output: File) {
        val root = JsonObject()
        val componentsObj = JsonObject()
        val macrosObj = JsonObject()
        root.add("components", componentsObj)
        root.add("macros", macrosObj)
        for (component in components) {
            component.serialize(root, this)
        }

        for (curve in curves) {
            curve.serialize(root, this)
        }

        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
        val string = gson.toJson(root)
        output.writeText(string)
    }

    fun parseExpr(
        molang: String,
        toTicks: Boolean = false
    ): Script {
        val scanner = Scanner(molang)
        val (tokens, errors) = scanner.scanTokens()
        for (error in errors) {
            println("Error: $error")
        }

        val parser = Parser(tokens, this)
        var expr = parser.parse()
        if (toTicks) {
            val lastExpr = expr.last()
            val transformedExpr = BinaryExpr(lastExpr, Token(TokenType.STAR, "*", null, 0), LiteralExpr(20.0))
            expr = expr.dropLast(1).toMutableList()
                .apply { add(transformedExpr) }
        }

        return expr
    }

    fun toJS(
        exprs: Script
    ): String {
        val resolver = ConstantResolver()
        val resolved = resolver.resolve(exprs)
        return JavascriptPrinter(this).print(resolved)
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