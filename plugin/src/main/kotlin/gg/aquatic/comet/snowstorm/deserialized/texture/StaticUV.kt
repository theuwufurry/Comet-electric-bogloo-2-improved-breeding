package gg.aquatic.comet.snowstorm.deserialized.texture

import com.google.gson.JsonObject
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.parsing.resourcepack.UVData
import gg.aquatic.comet.particle.display.sprite.ConstantSpriteComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import org.joml.Vector2i

class StaticUV(
    private val coords: Vector2i,
    private val size: Vector2i
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        ResourcepackCreator.uvs += UVData(
            coords,
            size,
            deserializedEffect.texture
        )

        val jsonObject = JsonObject()

        jsonObject.addProperty("sprite", "${deserializedEffect.texture}_${coords.x}_${coords.y}_${size.x}_${size.y}")

        root["components"].asJsonObject.add(ConstantSpriteComponent.id, jsonObject)
    }
}