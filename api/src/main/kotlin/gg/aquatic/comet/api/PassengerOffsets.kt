package gg.aquatic.comet.api

import com.google.gson.JsonParser
import org.bukkit.entity.EntityType
import org.joml.Vector3d

object PassengerOffsets {
    lateinit var offsets: Map<EntityType, Vector3d>

    fun load() {
        val map =
            mutableMapOf<EntityType, Vector3d>()
        val bytes = AbstractParticleEmitter.INSTANCE.getResource("passenger_offsets.json")!!.readAllBytes()
        val json = JsonParser.parseString(bytes.decodeToString()).asJsonObject
        for ((name, elem) in json.entrySet()) {
            val vec = Vector3d(
                elem.asJsonObject["x"].asJsonPrimitive.asNumber.toDouble(),
                elem.asJsonObject["y"].asJsonPrimitive.asNumber.toDouble(),
                elem.asJsonObject["z"].asJsonPrimitive.asNumber.toDouble(),
            )

            val type = EntityType.valueOf(name.uppercase())

            map += type to vec
        }

        offsets = map
    }
}