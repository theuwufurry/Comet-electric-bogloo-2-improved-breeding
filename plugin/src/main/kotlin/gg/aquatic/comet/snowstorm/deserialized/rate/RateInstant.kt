package gg.aquatic.comet.snowstorm.deserialized.rate

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.rate.InstantRateComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class RateInstant(
    private val numParticles: String
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("amount", numParticles)
        root["components"].asJsonObject.add(InstantRateComponent.id, jsonObject)
    }
}