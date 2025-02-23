package gg.aquatic.comet.snowstorm.deserialized.texture

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.display.sprite.ConstantSpriteComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect

class FullUV : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()

        jsonObject.addProperty("sprite", deserializedEffect.texture)

        root["components"].asJsonObject.add(ConstantSpriteComponent.id, jsonObject)
    }
}