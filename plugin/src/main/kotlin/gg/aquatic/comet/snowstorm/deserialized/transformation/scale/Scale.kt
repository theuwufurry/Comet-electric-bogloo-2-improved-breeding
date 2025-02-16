package gg.aquatic.comet.snowstorm.deserialized.transformation.scale

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.transformation.scale.ExpressionScaleComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class Scale(
    private val x: String,
    private val y: String,
    private val z: String,
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()

        jsonObject.addProperty("x", "($x) * 12")
        jsonObject.addProperty("y", "($y) * 12")
        jsonObject.addProperty("z", "($z) * 12")

        root["components"].asJsonObject.add(ExpressionScaleComponent.id, jsonObject)
    }
}