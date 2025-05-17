package gg.aquatic.comet.parsing

import com.google.gson.JsonParser
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import org.joml.Vector3d

object PassengerOffsets {
    lateinit var offsets: Map<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType, Vector3d>

    fun load() {
        val map =
            mutableMapOf<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType, Vector3d>()
        val bytes = AbstractParticleEmitter.INSTANCE.getResource("passenger_offsets.json")!!.readAllBytes()
        val json = JsonParser.parseString(bytes.decodeToString()).asJsonObject
        for ((name, elem) in json.entrySet()) {
            val vec = Vector3d(
                elem.asJsonObject["x"].asJsonPrimitive.asNumber.toDouble(),
                elem.asJsonObject["y"].asJsonPrimitive.asNumber.toDouble(),
                elem.asJsonObject["z"].asJsonPrimitive.asNumber.toDouble(),
            )

            val type = EntityTypes.getByName(name)

            map += (type as gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType) to vec
        }

        offsets = map
    }
}