package gg.aquatic.comet.snowstorm.deserialized.motion

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.position.MotionPositionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class MotionDynamic(
    private var linearAcceleration: Triple<String?, String?, String?> = Triple(null, null, null)
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()

        if (linearAcceleration != Triple(null, null, null)) {
            val accelerationObject = JsonObject()

            linearAcceleration.first?.let { accelerationObject.addProperty("x", it) }
            linearAcceleration.second?.let { accelerationObject.addProperty("y", it) }
            linearAcceleration.third?.let { accelerationObject.addProperty("z", it) }

            jsonObject.add("acceleration", accelerationObject)
        }

        root["components"].asJsonObject.add(MotionPositionComponent.id, jsonObject)
    }
}