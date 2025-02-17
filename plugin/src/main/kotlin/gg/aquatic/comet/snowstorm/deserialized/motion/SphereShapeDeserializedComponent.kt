package gg.aquatic.comet.snowstorm.deserialized.motion

import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.Program

class SphereShapeDeserializedComponent(
    val radius: Program? = null,
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {

    }
}