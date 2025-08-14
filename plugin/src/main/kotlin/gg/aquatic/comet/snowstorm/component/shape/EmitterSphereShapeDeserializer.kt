package gg.aquatic.comet.snowstorm.component.shape

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asBooleanOrNull
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.particle.position.initial.SpherePositionComponent
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.motion.MotionDynamic
import gg.aquatic.comet.snowstorm.deserialized.motion.SphereShapeDeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import java.io.File

object EmitterSphereShapeDeserializer : Deserializer {
    override val id = "minecraft:emitter_shape_sphere"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val offsetArray = jsonObject["offset"] as? JsonArray
        val parsedArray = offsetArray?.map { deserializedEffect.parseExpr(it.primitiveString()) }

        val radius =
            jsonObject.getExprOrNull("radius")?.let { deserializedEffect.parseExpr(it) } ?: listOf(LiteralExpr(1.0))
        val surfaceOnly = jsonObject["surface_only"]?.asBooleanOrNull() ?: false


        val motionDir = jsonObject["direction"]?.let top@{
            if (it is JsonArray) {
                if (it.size() < 3) return@top null
                MotionDynamic.Direction.VectorDirection(
                    deserializedEffect.parseExpr(it[0].asString),
                    deserializedEffect.parseExpr(it[1].asString),
                    deserializedEffect.parseExpr(it[2].asString),
                )
            } else if (it is JsonPrimitive && it.isString) {
                if (it.asString == "inwards") return@top MotionDynamic.Direction.SphereDirection(SpherePositionComponent.DirType.INWARDS)
                else return@top MotionDynamic.Direction.SphereDirection(SpherePositionComponent.DirType.OUTWARDS)
            } else null
        }

        val motionDynamic = deserializedEffect.components.firstOrNull { it is MotionDynamic }
        if (motionDynamic == null) {
            deserializedEffect.components += MotionDynamic(
                dir = motionDir
            )
        } else {
            motionDynamic as MotionDynamic
            motionDynamic.dir = motionDir
        }

        deserializedEffect.components += SphereShapeDeserializedComponent(radius)
    }
}