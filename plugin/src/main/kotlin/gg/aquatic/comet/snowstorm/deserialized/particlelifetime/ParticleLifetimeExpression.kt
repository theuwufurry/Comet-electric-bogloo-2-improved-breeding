package gg.aquatic.comet.snowstorm.deserialized.particlelifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeExpressionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class ParticleLifetimeExpression(
    private val maxLifetime: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("max_lifetime", deserializedEffect.toJS(maxLifetime))
        root["components"].asJsonObject.add(ParticleLifetimeExpressionComponent.id, jsonObject)
    }
}