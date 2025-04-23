package gg.aquatic.comet.snowstorm

import com.google.gson.JsonElement
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import java.io.File

interface Deserializer {
    val id: String
    fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect)
}