package gg.aquatic.comet.particle.data


import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.util.Quaternion4f
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.api.particle.display.text.TextData
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
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

    @Volatile
    private var key = Key.key(ResourcepackCreator.NAMESPACE, ResourcepackCreator.FONT_NAME)

    private val DEBUG = 0

    override fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
        useUAP: Boolean,
    ): List<PEntityData<*>>? {
        return genData(entityData, flags, initial, useUAP)
    }

    private fun genData(
        component: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
        useUAP: Boolean,
    ): List<PEntityData<*>>? {
        if (key.namespace() != ResourcepackCreator.NAMESPACE) {
            key = Key.key(ResourcepackCreator.NAMESPACE, ResourcepackCreator.FONT_NAME)
        }

        val entityData: MutableList<PEntityData<*>> =
            mutableListOf()

        when (val displayData = component.displayData) {
            is SpriteData -> {
                if (initial) {
                    entityData += PEntityData(
                        23 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        Int.MAX_VALUE,
                    )
                    entityData += PEntityData(
                        24 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        0,
                    )

                    var b = 0
                    if (component.shadow) b = b or 1
                    if (component.seeThrough) b = b or 2

                    if (b != 0) {
                        entityData += PEntityData(
                            26 + PACKET_OFFSET,
                            EntityDataTypes.BYTE,
                            b.toByte()
                        )
                    }
                }

                if (flags.display) {
                    var component = Component.text(ResourcepackCreator.charMap[displayData.content]!!)
                        .color(TextColor.color(component.color and 0xFFFFFF))
                    if (!useUAP) {
                        component = component.font(key)
                    }
                    entityData += PEntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ADV_COMPONENT,
                        component
                    )
                }

                if (flags.transparency) {
                    val transformedTransparency = component.transparency.coerceAtLeast(25)
                    entityData += PEntityData(
                        25 + PACKET_OFFSET,
                        EntityDataTypes.BYTE,
                        transformedTransparency.toByte()
                    )
                }
            }

            is ModelData -> {
                if (flags.display) {
                    entityData += PEntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ITEMSTACK,
                        SpigotConversionUtil.fromBukkitItemStack(
                            ResourcepackCreator.stack(
                                displayData.content,
                                component.color,
                            ) ?: ItemStack.empty()
                        )
                    )
                }
            }

            is TextData -> {
                val td = (component.displayData as TextData)
                if (initial) {
                    var b = 0
                    if (component.shadow) b = b or 1
                    if (component.seeThrough) b = b or 2

                    if (b != 0) {
                        entityData += PEntityData(
                            26 + PACKET_OFFSET,
                            EntityDataTypes.BYTE,
                            b.toByte()
                        )
                    }
                }

                if (flags.display) {
                    entityData += PEntityData(
                        22 + PACKET_OFFSET,
                        EntityDataTypes.ADV_COMPONENT,
                        td.with(component.color and 0xFFFFFF)
                    )
                    entityData += PEntityData(
                        23 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        td.lineWidth,
                    )

                    entityData += PEntityData(
                        24 + PACKET_OFFSET,
                        EntityDataTypes.INT,
                        td.backgroundColor,
                    )
                }

                if (flags.transparency) {
                    entityData += PEntityData(
                        25 + PACKET_OFFSET,
                        EntityDataTypes.BYTE,
                        ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
                    )
                }
            }

            else -> {
                return null
            }
        }

        entityData += PEntityData(8, EntityDataTypes.INT, component.interpolationDelay)

        if (initial || flags.transformationInterpolation) {
            entityData += PEntityData(
                9,
                EntityDataTypes.INT,
                component.transformationInterpolationDuration
            )
        }

        if ((initial || flags.teleportationDuration) && PACKET_OFFSET > 0) {
            entityData += PEntityData(
                10,
                EntityDataTypes.INT,
                component.teleportationDuration
            )
        }

        if (initial) {
            entityData += PEntityData(
                14 + PACKET_OFFSET,
                EntityDataTypes.BYTE,
                component.billboardConstraints.byte
            )
        }

        if (flags.scale) {
            if (!(initial && component.scale == defaultScale)) {
                entityData += PEntityData(
                    11 + PACKET_OFFSET,
                    EntityDataTypes.VECTOR3F,
                    com.github.retrooper.packetevents.util.Vector3f(
                        component.scale.x,
                        component.scale.y,
                        component.scale.z
                    )
                )
            }
        }

        if (flags.rotation) {
            if (!(initial && component.rotation == defaultRotation)) {
                entityData += PEntityData(
                    12 + PACKET_OFFSET,
                    EntityDataTypes.QUATERNION,
                    Quaternion4f(component.rotation.x, component.rotation.y, component.rotation.z, component.rotation.w)
                )
            }
        }

        if (component.sensitiveCentering && (flags.rotation || flags.scale)) {
            val offset = Vector3f(
                -0.0125f,
                0.025f,
                0f
            )

            offset
                .mul(component.scale)
                .rotate(component.rotation)
                .add(component.translation)
            entityData += PEntityData(
                10 + PACKET_OFFSET,
                EntityDataTypes.VECTOR3F,
                com.github.retrooper.packetevents.util.Vector3f(
                    offset.x,
                    offset.y,
                    offset.z
                )
            )
        }

        if (initial && component.lightData != null) {
            entityData += PEntityData(
                15 + PACKET_OFFSET,
                EntityDataTypes.INT,
                (component.lightData!!.blocklight shl 4) or (component.lightData!!.skylight shl 20)
            )
            component.lightData
        }

        if (DEBUG >= 1) print(flags, component, initial)

        return entityData
    }

    private fun print(flags: UpdateFlags, entityData: EntityData, initial: Boolean) {
        if (!flags.anyTrue()) return
        val strB = StringBuilder()
        strB.append("/-- ${if (initial) "I" else ""}- EDB -----\n")
        if (flags.display) {
            strB.append("| DISPLAY: ${entityData.displayData}\n")
            strB.append("|_ COLOR: ${entityData.color}\n")
        }
        if (flags.translation)
            strB.append("| TRANSLATION: ${entityData.translation}\n")
        if (initial || flags.scale)
            strB.append("| SCALE: ${entityData.scale}\n")
        if (flags.teleportationDuration)
            strB.append("| TELEPORTATION DURATION: ${entityData.teleportationDuration}\n")
        if (initial || flags.transformationInterpolation)
            strB.append("| TRANSFORMATION INTERPOLATION DURATION: ${entityData.transformationInterpolationDuration}\n")
        if (flags.transparency)
            strB.append("| TRANSPARENCY: ${entityData.transparency}\n")
        if (flags.rotation)
            strB.append("| ROTATION: ${entityData.rotation}\n")

        if (flags.teleportationDuration &&
            !flags.scale && !flags.translation && !flags.display && !flags.transformationInterpolation && !flags.transparency && !flags.rotation
        ) {
            strB.append("| LONELY!")
        }

        println(strB)
    }
}

private typealias PEntityData<T> = com.github.retrooper.packetevents.protocol.entity.data.EntityData<T>