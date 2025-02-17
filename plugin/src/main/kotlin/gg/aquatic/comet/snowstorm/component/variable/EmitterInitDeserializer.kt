package gg.aquatic.comet.snowstorm.component.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.variable.EmitterInit
import java.io.File

object EmitterInitDeserializer : Deserializer {
    override val id = "minecraft:emitter_initialization"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val creationStr = jsonObject["creation_expression"]?.asStringOrNull()
        if (creationStr != null) {
            deserializedEffect.components += EmitterInit(deserializedEffect.parseExpr(creationStr))
        }
    }
}