package gg.aquatic.comet.snowstorm.deserialized.emitterlifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.lifetime.ExpressionEmitterLifetimeComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class EmitterLifetimeExpression(
    private val activation: List<Expr>?,
    private val expiration: List<Expr>?
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        activation?.let { jsonObject.addProperty("activation_expression", deserializedEffect.toJS(it)) }
        expiration?.let { jsonObject.addProperty("expiration_expression", deserializedEffect.toJS(it)) }

        root["components"].asJsonObject.add(ExpressionEmitterLifetimeComponent.id, jsonObject)
    }
}