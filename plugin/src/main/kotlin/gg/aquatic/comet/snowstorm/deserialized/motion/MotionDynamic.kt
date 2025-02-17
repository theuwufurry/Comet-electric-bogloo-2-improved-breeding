package gg.aquatic.comet.snowstorm.deserialized.motion

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.position.MotionPositionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class MotionDynamic(
    private var linearAcceleration: Triple<List<Expr>?, List<Expr>?, List<Expr>?> = Triple(null, null, null)
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()

        if (linearAcceleration != Triple(null, null, null)) {
            val accelerationObject = JsonObject()

            linearAcceleration.first?.let { accelerationObject.addProperty("x", deserializedEffect.toJS(it)) }
            linearAcceleration.second?.let { accelerationObject.addProperty("y", deserializedEffect.toJS(it)) }
            linearAcceleration.third?.let { accelerationObject.addProperty("z", deserializedEffect.toJS(it)) }

            jsonObject.add("acceleration", accelerationObject)
        }

        root["components"].asJsonObject.add(MotionPositionComponent.id, jsonObject)
    }
}