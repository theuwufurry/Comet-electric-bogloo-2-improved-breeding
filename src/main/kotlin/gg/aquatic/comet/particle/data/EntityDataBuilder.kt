package gg.aquatic.comet.particle.data

import gg.aquatic.comet.particle.UpdateFlags
import gg.aquatic.comet.particle.display.DisplayData
import gg.aquatic.comet.particle.display.TextDisplayComponent
import gg.aquatic.comet.particle.display.model.ModelData
import gg.aquatic.comet.particle.display.sprite.SpriteData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.component.ComponentTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.item.ItemStack
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.item.type.ItemTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Quaternion4f
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.joml.Quaternionf
import org.joml.Vector3f

val PACKET_OFFSET = if (checkVersion()) 1 else 0

private fun checkVersion(): Boolean {
    val bukkitVersion = Bukkit.getBukkitVersion()
    val versionPart = bukkitVersion.split("-")[0]
    val versionNumbers = versionPart.split(".").map { it.toIntOrNull() ?: 0 }

    val major = versionNumbers.getOrElse(0) { 0 }
    val minor = versionNumbers.getOrElse(1) { 0 }
    val patch = versionNumbers.getOrElse(2) { 0 }

    return (major > 1) || (major == 1 && minor > 20) || (major == 1 && minor == 20 && patch >= 2)
}

object EntityDataBuilder {
    private val key = Key.key("particlecreator", "default")

    fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean
    ): List<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData> {
        return genData(entityData, flags, initial)
    }

    private fun genData(
        component: EntityData,
        flags: UpdateFlags,
        initial: Boolean
    ): List<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData> {
        val entityData: MutableList<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData> =
            mutableListOf()

        when (component.displayData) {
            is SpriteData -> {
                if (initial) {
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        23 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        Int.MAX_VALUE
                    )

                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        24 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        0
                    )
                }

                if (flags.display) {
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ADV_COMPONENT,
                        Component.translatable(component.displayData.id)
                            .color(TextColor.color(component.color and 0xFFFFFF)).font(key)
                    )
                }

                if (flags.transparency) {
                    val transformedTransparency = component.transparency.coerceAtLeast(25)
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        25 + PACKET_OFFSET,
                        EntityDataTypes.BYTE,
                        (transformedTransparency).toByte()
                    )
                }
            }

            is ModelData -> {
                if (flags.display) {
                    val modelData = component.displayData

                    val stack = ItemStack.builder().type(ItemTypes.getByName(modelData.item)).amount(1).build()
                    stack.setComponent(ComponentTypes.CUSTOM_MODEL_DATA, modelData.id)

                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ITEMSTACK,
                        stack
                    )
                }
            }

            else -> {
                if (flags.display) {
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ADV_COMPONENT,
                        Component.text((component.displayData as TextDisplayComponent).string)
                            .color(TextColor.color(component.color and 0xFFFFFF))
                    )
                }

                if (flags.transparency) {
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        25 + PACKET_OFFSET,
                        EntityDataTypes.BYTE,
                        ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
                    )
                }
            }
        }

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            8,
            EntityDataTypes.INT,
            component.interpolationDelay
        )

        if (initial || flags.interpolation) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                9,
                EntityDataTypes.INT,
                component.interpolationDuration
            )

            if (PACKET_OFFSET > 0) {
                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    10,
                    EntityDataTypes.INT,
                    component.interpolationDuration
                )
            }
        }

        if (initial) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                14 + PACKET_OFFSET,
                EntityDataTypes.BYTE,
                component.billboardConstraints.byte
            )
        }

//        if (flags.translation) {
//            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
//                10 + PACKET_OFFSET,
//                EntityDataTypes.VECTOR3F,
//                gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f(
//                    component.translation.x,
//                    component.translation.y,
//                    component.translation.z
//                )
//            )
//        }

        if (flags.scale) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                11 + PACKET_OFFSET,
                EntityDataTypes.VECTOR3F,
                gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f(
                    component.scale.x,
                    component.scale.y,
                    component.scale.z
                )
            )
        }

        if (flags.rotation) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                12 + PACKET_OFFSET,
                EntityDataTypes.QUATERNION,
                Quaternion4f(component.rotation.x, component.rotation.y, component.rotation.z, component.rotation.w)
            )
        }

        if (flags.rotation || flags.scale) {
            val offset = Vector3f(-0.0125f, 0f, 0f)
            offset.mul(component.scale).rotate(component.rotation)
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                10 + PACKET_OFFSET,
                EntityDataTypes.VECTOR3F,
                gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f(
                    component.translation.x + offset.x,
                    component.translation.y + offset.y,
                    component.translation.z + offset.y
                )
            )
        }

        return entityData
    }
}

data class EntityData(
    val displayData: DisplayData,
    val color: Int,
    val transparency: Int,
    val reserveTransparency: Int?,
    val translation: Vector3f,
    val rotation: Quaternionf,
    val scale: Vector3f,
    val billboardConstraints: BillboardConstraints,
    val interpolationDelay: Int,
    val interpolationDuration: Int
) {
    fun copy(): EntityData {
        return EntityData(
            displayData.copy(),
            color,
            transparency,
            reserveTransparency,
            Vector3f(translation),
            Quaternionf(rotation),
            Vector3f(scale),
            billboardConstraints,
            interpolationDelay,
            interpolationDuration
        )
    }
}

enum class BillboardConstraints(val byte: Byte) {
    FIXED((0).toByte()),
    VERTICAL((1).toByte()),
    HORIZONTAL((2).toByte()),
    CENTER((3).toByte())
}