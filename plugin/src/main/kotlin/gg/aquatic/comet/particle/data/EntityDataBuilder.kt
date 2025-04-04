package gg.aquatic.comet.particle.data

import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.api.particle.display.TextDisplayComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityDataTypes
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

object EntityDataBuilder : AbstractEntityDataBuilder() {
    private val defaultRotation = Quaternionf(0f, 0f, 0f, 1f)
    private val defaultScale = Vector3f(1f)

    private val key = Key.key("particlecreator", "default")

    private val DEBUG = 2

    override fun getDataFor(
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

        when (val displayData = component.displayData) {
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
                        Component.translatable(displayData.id)
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
                    entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ITEMSTACK,
                        ResourcepackCreator.modelMap[displayData.id]!!
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

        if (initial || flags.transformationInterpolation) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                9,
                EntityDataTypes.INT,
                component.transformationInterpolationDuration
            )
        }

        if ((initial || flags.teleportationDuration) && PACKET_OFFSET > 0) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                10,
                EntityDataTypes.INT,
                component.teleportationDuration
            )
        }

        if (initial) {
            entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                14 + PACKET_OFFSET,
                EntityDataTypes.BYTE,
                component.billboardConstraints.byte
            )
        }

        if (flags.scale) {
            if (!(initial && component.scale == defaultScale)) {
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
        }

        if (flags.rotation) {
            if (!(initial && component.rotation == defaultRotation)) {
                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    12 + PACKET_OFFSET,
                    EntityDataTypes.QUATERNION,
                    Quaternion4f(component.rotation.x, component.rotation.y, component.rotation.z, component.rotation.w)
                )
            }
        }

        if (flags.rotation || flags.scale) {
            val offset = if (component.billboardConstraints == BillboardConstraints.CENTER) Vector3f(
                -0.0125f,
                0f,
                0f
            ) else Vector3f()
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

        if (DEBUG >= 1) print(flags, component, initial)

        return entityData
    }

    private fun print(flags: UpdateFlags, entityData: EntityData, initial: Boolean) {
        if (!flags.anyRelevantTrue()) return
        val strB = StringBuilder()
        strB.append("/-- ${if (initial) "I" else ""}- EDB -----\n")
        if (flags.display) {
            strB.append("| DISPLAY: ${entityData.displayData}\n")
            strB.append("|_ COLOR: ${entityData.color}\n")
        }
        if (flags.translation)
            strB.append("| TRANSLATION: ${entityData.translation}\n")
        if (flags.scale)
            strB.append("| SCALE: ${entityData.scale}\n")
        if (flags.teleportationDuration)
            strB.append("| TELEPORTATION DURATION: ${entityData.teleportationDuration}\n")
        if (flags.transformationInterpolation)
            strB.append("| TRANSFORMATION INTERPOLATION DURATION: ${entityData.transformationInterpolationDuration}\n")
        if (flags.transparency)
            strB.append("| TRANSPARENCY: ${entityData.transparency}\n")
        if (flags.rotation)
            strB.append("| ROTATION: ${entityData.rotation}\n")

        println(strB)
    }
}