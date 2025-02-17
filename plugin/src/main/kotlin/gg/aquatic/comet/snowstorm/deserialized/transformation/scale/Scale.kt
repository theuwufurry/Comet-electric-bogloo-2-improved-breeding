package gg.aquatic.comet.snowstorm.deserialized.transformation.scale

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.transformation.scale.ExpressionScaleComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.Script

class Scale(
    private val x: Script,
    private val y: Script,
    private val z: Script,
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()

        jsonObject.addProperty("x", deserializedEffect.toJS(x))
        jsonObject.addProperty("y", deserializedEffect.toJS(y))
        jsonObject.addProperty("z", deserializedEffect.toJS(z))

        root["components"].asJsonObject.add(ExpressionScaleComponent.id, jsonObject)
    }
}