package gg.aquatic.comet.snowstorm.component.motion

import com.google.gson.JsonElement
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.motion.MotionDynamic
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.transpilation.expression.BinaryExpr
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import java.io.File

object InitialSpeedDeserializer : Deserializer {
    override val id = "minecraft:particle_initial_speed"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val str = jsonElement.primitiveString()

        var parsed = deserializedEffect.parseExpr(str)
        val last = BinaryExpr(
            parsed.last(),
            Token(TokenType.SLASH, "/", null, 0),
            LiteralExpr(16.0)
        )

        parsed = parsed.dropLast(1).toMutableList().apply {
            add(last)
        }

        val motionDynamic = deserializedEffect.components.firstOrNull { it is MotionDynamic }
        if (motionDynamic == null) {
            deserializedEffect.components += MotionDynamic(
                magnitude = parsed
            )
        } else {
            motionDynamic as MotionDynamic
            motionDynamic.magnitude = parsed
        }
    }
}