package gg.aquatic.comet.snowstorm.deserialized.rate

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.rate.SteadyRateComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class RateSteady(
    private val spawnRate: String,
    private val maxParticles: String
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("spawn_rate", spawnRate)
        jsonObject.addProperty("max_particles", maxParticles)
        root["components"].asJsonObject.add(SteadyRateComponent.id, jsonObject)
    }
}