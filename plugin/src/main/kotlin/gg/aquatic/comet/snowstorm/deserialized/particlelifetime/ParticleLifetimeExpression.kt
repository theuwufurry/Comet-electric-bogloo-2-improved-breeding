package gg.aquatic.comet.snowstorm.deserialized.particlelifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeExpressionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class ParticleLifetimeExpression(
    private val maxLifetime: String
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("max_lifetime", maxLifetime)
        root["components"].asJsonObject.add(ParticleLifetimeExpressionComponent.id, jsonObject)
    }
}