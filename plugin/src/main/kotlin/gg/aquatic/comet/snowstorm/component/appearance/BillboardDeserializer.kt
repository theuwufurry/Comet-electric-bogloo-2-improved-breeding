package gg.aquatic.comet.snowstorm.component.appearance

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.deserialized.transformation.scale.Scale
import gg.aquatic.comet.snowstorm.transpilation.expression.BinaryExpr
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import gg.aquatic.comet.snowstorm.transpilation.token.Token
import gg.aquatic.comet.snowstorm.transpilation.token.TokenType
import java.io.File

object BillboardDeserializer : Deserializer {
    override val id = "minecraft:particle_appearance_billboard"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val sizeArray = jsonObject["size"] as? JsonArray
        if (sizeArray == null || sizeArray.size() != 2) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed size in $id component parsing ${file.path}")
            return
        }

        val parsedArray = sizeArray.map {
            val parsed = deserializedEffect.parseExpr(it.primitiveString())

            val last = BinaryExpr(
                parsed.last(),
                Token(TokenType.STAR, "*", null, 0),
                LiteralExpr(8.0)
            )

            parsed.dropLast(1).toMutableList().apply {
                add(last)
            }
        }
        deserializedEffect.components += Scale(parsedArray[0], parsedArray[1], listOf(LiteralExpr(1.0)))
    }
}