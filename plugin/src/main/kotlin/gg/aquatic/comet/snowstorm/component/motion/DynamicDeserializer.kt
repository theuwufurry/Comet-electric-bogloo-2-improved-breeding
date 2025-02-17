package gg.aquatic.comet.snowstorm.component.motion

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.motion.MotionDynamic
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.transpilation.expression.BinaryExpr
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import java.io.File

object DynamicDeserializer : Deserializer {
    override val id = "minecraft:particle_motion_dynamic"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val offsetArray = jsonObject["linear_acceleration"] as? JsonArray
        val parsedArray = offsetArray?.map {
            if (it.primitiveString() == "0") return@map null
            val parsed = deserializedEffect.parseExpr(it.primitiveString())
            val last = BinaryExpr(
                parsed.last(),
                Token(TokenType.SLASH, "/", null, 0),
                LiteralExpr(256.0)
            )

            parsed.dropLast(1).toMutableList().apply {
                add(last)
            }
        }

        val dragScript = jsonObject["linear_drag_coefficient"]?.primitiveString()?.let {
            if (it == "0") return@let null
            val parsed = deserializedEffect.parseExpr(it)
            val last = BinaryExpr(
                parsed.last(),
                Token(TokenType.SLASH, "/", null, 0),
                LiteralExpr(20.0)
            )

            parsed.dropLast(1).toMutableList().apply {
                add(last)
            }
        }

        val motionDynamic = deserializedEffect.components.firstOrNull { it is MotionDynamic }
        if (motionDynamic == null) {
            deserializedEffect.components += MotionDynamic()
                .apply {
                    if (parsedArray != null) {
                        linearAcceleration = Triple(
                            parsedArray[0],
                            parsedArray[1],
                            parsedArray[2]
                        )
                        drag = dragScript
                    }
                }
        } else {
            motionDynamic as MotionDynamic
            if (parsedArray != null) {
                motionDynamic.linearAcceleration = Triple(
                    parsedArray[0],
                    parsedArray[1],
                    parsedArray[2]
                )
            }

            if (dragScript != null) {
                motionDynamic.drag = dragScript
            }
        }
    }
}