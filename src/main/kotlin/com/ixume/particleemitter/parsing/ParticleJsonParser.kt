package com.ixume.particlesTesting.parsing

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ixume.particlesTesting.ParticleEmitter
import com.ixume.particlesTesting.emitter.UnrealizedEmitter
import com.ixume.particlesTesting.emitter.lifetime.LifetimeComponent
import com.ixume.particlesTesting.emitter.lifetime.LifetimeExpressionComponent
import com.ixume.particlesTesting.emitter.rate.RateComponent
import com.ixume.particlesTesting.emitter.rate.SteadyRateComponent
import com.ixume.particlesTesting.emitter.shape.PointShapeComponent
import com.ixume.particlesTesting.emitter.shape.ShapeComponent
import java.io.FileReader

val variableReassignment : Map<String, String> = mapOf(
    "variable.particle_age" to "particle_age",
    "variable.emitter_age" to "emitter_age"
)

fun String.replaceWithMap(map: Map<String, String>): String {
    var string = this
    for ((old, new) in map) {
        string = string.replace(old, new)
    }

    return string
}

internal fun String.renameVariables(): String {
    return this.replaceWithMap(variableReassignment)
}

fun JsonObject.expression(field: String): String? {
    if (field !in keySet()) return null
    val fieldElement = this.get(field)
    if (!fieldElement.isJsonPrimitive) return null
    val fieldPrimitive = fieldElement.asJsonPrimitive
    return (if (fieldPrimitive.isNumber) fieldPrimitive.asNumber.toString() else fieldPrimitive.asString).renameVariables()
}

object ParticleJsonParser {
    lateinit var jsonUnrealizdEmitters: Map<String, UnrealizedEmitter>
        private set

    fun parseJsons() {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val logger = ParticleEmitter.INSTANCE.logger

        val unrealizedEmitters: MutableMap<String, UnrealizedEmitter> = mutableMapOf()

        for (file in dataFolder.listFiles()!!) {
            if (file.extension != "json") continue

            val rootObject = JsonParser.parseReader(FileReader(file)).asJsonObject

            val particleEffectObject: JsonObject? = rootObject.getAsJsonObject("particle_effect")
            if (particleEffectObject == null) {
                logger.warning("""Field "particle_effect" is null in ${file.name}!""")
                continue
            }

            val componentsObject: JsonObject? = particleEffectObject.getAsJsonObject("components")
            if (componentsObject == null) {
                logger.warning("""Field "components" is null in ${file.name}!""")
                continue
            }

            val emitter = parseComponents(componentsObject)
            emitter?.run {
                unrealizedEmitters += file.nameWithoutExtension to emitter
            }
        }

        jsonUnrealizdEmitters = unrealizedEmitters
    }

    private fun parseComponents(componentsObject: JsonObject): UnrealizedEmitter? {
        var rateComponent: RateComponent? = null
        var lifetimeComponent: LifetimeComponent? = null
        var shapeComponent: ShapeComponent? = null
        for ((key, componentElement) in componentsObject.entrySet()) {
            when (key) {
                "minecraft:emitter_rate_steady" -> {
                    rateComponent = SteadyRateComponent.parse(componentElement)
                }
                "minecraft:particle_lifetime_expression" -> {
                    lifetimeComponent = LifetimeExpressionComponent.parse(componentElement)
                }
                "minecraft:emitter_shape_point" -> {
                    shapeComponent = PointShapeComponent.parse(componentElement)
                }
            }
        }

        return UnrealizedEmitter(
            rateComponent ?: return null,
            lifetimeComponent ?: return null,
            shapeComponent ?: return null)
    }
}