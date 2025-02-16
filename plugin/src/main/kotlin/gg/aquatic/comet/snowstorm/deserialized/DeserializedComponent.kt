package gg.aquatic.comet.snowstorm.deserialized

import com.google.gson.JsonElement
import com.google.gson.JsonObject

interface DeserializedComponent {
    fun serialize(root: JsonObject)
}