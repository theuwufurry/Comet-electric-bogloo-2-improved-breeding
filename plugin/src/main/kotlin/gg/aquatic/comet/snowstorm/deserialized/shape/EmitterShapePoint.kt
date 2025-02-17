package gg.aquatic.comet.snowstorm.deserialized.shape

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.position.initial.InitialExpressionPositionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class EmitterShapePoint(
    private val x: List<Expr>?,
    private val y: List<Expr>?,
    private val z: List<Expr>?,
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()

        x?.let { jsonObject.addProperty("x", deserializedEffect.toJS(it)) }
        y?.let { jsonObject.addProperty("y", deserializedEffect.toJS(it)) }
        z?.let { jsonObject.addProperty("z", deserializedEffect.toJS(it)) }

        root["components"].asJsonObject.add(InitialExpressionPositionComponent.id, jsonObject)
    }
}