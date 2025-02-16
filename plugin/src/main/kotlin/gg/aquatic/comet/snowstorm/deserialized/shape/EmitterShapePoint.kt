package gg.aquatic.comet.snowstorm.deserialized.shape

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.position.initial.InitialExpressionPositionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class EmitterShapePoint(
    private val x: String?,
    private val y: String?,
    private val z: String?,
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()

        x?.let { jsonObject.addProperty("x", it) }
        y?.let { jsonObject.addProperty("y", it) }
        z?.let { jsonObject.addProperty("z", it) }

        root["components"].asJsonObject.add(InitialExpressionPositionComponent.id, jsonObject)
    }
}