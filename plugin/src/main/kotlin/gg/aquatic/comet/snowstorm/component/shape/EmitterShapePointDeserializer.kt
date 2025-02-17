package gg.aquatic.comet.snowstorm.component.shape

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.deserialized.shape.EmitterShapePoint
import java.io.File

object EmitterShapePointDeserializer : Deserializer {
    override val id = "minecraft:emitter_shape_point"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val offsetArray = jsonObject["offset"] as? JsonArray
        if (offsetArray == null || offsetArray.size() != 3) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed offset in $id component parsing ${file.path}")
            return
        }

        val parsedArray = offsetArray.map { deserializedEffect.parseExpr(it.primitiveString()) }
        deserializedEffect.components += EmitterShapePoint(parsedArray[0], parsedArray[1], parsedArray[2])
    }
}