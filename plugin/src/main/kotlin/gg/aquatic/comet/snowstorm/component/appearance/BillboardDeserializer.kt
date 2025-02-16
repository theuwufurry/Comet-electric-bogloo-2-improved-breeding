package gg.aquatic.comet.snowstorm.component.appearance

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import gg.aquatic.comet.snowstorm.deserialized.transformation.scale.Scale
import java.io.File

object BillboardDeserializer : Deserializer{
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

        val parsedArray = sizeArray.map { deserializedEffect.parseMolang(it.primitiveString()) }
        deserializedEffect.components += Scale(parsedArray[0], parsedArray[1], "1")
    }
}