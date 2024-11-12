package gg.aquatic.particleemitter.particle.data

import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.joml.Matrix3f
import org.joml.Matrix4f

object EntityDataBuilder {
    private val cache: MutableMap<ComponentData, MutableList<EntityData>> = mutableMapOf()
    //private val level: Level = (Bukkit.getWorld("world") as CraftWorld).handle

    fun getDataFor(componentData: ComponentData): MutableList<EntityData> {
        return genData(componentData)
//        performance of this is questionable
//        return cache[componentData]?.let { return it } ?: genData(componentData).also { cache += (componentData to it) }
    }

    private fun genData(component: ComponentData): MutableList<EntityData> {
        val matrix3f = Matrix3f(component.matrix)
        return mutableListOf(
            EntityData(
                23, EntityDataTypes.ADV_COMPONENT,
                Component.translatable(component.translation).font(
                    Key.key("customparticle:default")).color(TextColor.color(component.color)
            )),
            EntityData(
                15, EntityDataTypes.BYTE, 3.toByte()
            ),
            EntityData(
                25, EntityDataTypes.INT, 0
            ),
            EntityData(
                10, EntityDataTypes.INT, 1
            ),
            EntityData(
                9, EntityDataTypes.INT, 1
            ),
            EntityData(
                8, EntityDataTypes.INT, -1
            ),
        )
        /*
        entity.setTransformation(Transformation(component.matrix))
        return entity.entityData
         */
    }
}

data class ComponentData(val translation: String, val color: Int, val matrix: Matrix4f)