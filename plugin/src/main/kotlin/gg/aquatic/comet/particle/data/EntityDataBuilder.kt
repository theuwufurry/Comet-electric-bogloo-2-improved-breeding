package gg.aquatic.comet.particle.data


import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.api.particle.display.text.TextData
import gg.aquatic.waves.api.nms.entity.DataSerializerTypes
import gg.aquatic.waves.api.nms.entity.EntityDataValue
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

    private val key = Key.key(ResourcepackCreator.NAMESPACE, ResourcepackCreator.FONT_NAME)

    private val DEBUG = 0

    override fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
    ): List<EntityDataValue>? {
        return genData(entityData, flags, initial)
    }

    private fun genData(
        component: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
    ): List<EntityDataValue>? {
        val entityData: MutableList<EntityDataValue> =
            mutableListOf()

        when (val displayData = component.displayData) {
            is SpriteData -> {
                if (initial) {
                    entityData += EntityDataValue.create(23 + PACKET_OFFSET, DataSerializerTypes.INT, Int.MAX_VALUE)
                    entityData += EntityDataValue.create(24 + PACKET_OFFSET, DataSerializerTypes.INT, 0)

                    var b = 0
                    if (component.shadow) b = b or 1
                    if (component.seeThrough) b = b or 2

                    if (b != 0) {
                        entityData += EntityDataValue.create(26 + PACKET_OFFSET, DataSerializerTypes.BYTE, b.toByte())
                    }
                }

                if (flags.display) {
                    entityData += EntityDataValue.create(
                        22 + PACKET_OFFSET, DataSerializerTypes.COMPONENT, Component.translatable(displayData.id)
                            .color(TextColor.color(component.color and 0xFFFFFF)).font(key)
                    )
                }

                if (flags.transparency) {
                    val transformedTransparency = component.transparency.coerceAtLeast(25)
                    entityData += EntityDataValue.create(
                        25 + PACKET_OFFSET,
                        DataSerializerTypes.BYTE,
                        transformedTransparency.toByte()
                    )
                }
            }

            is ModelData -> {
                if (flags.display) {
                    entityData += EntityDataValue.create(
                        22 + PACKET_OFFSET,
                        DataSerializerTypes.ITEM_STACK,
                        ResourcepackCreator.stack(displayData.id, component.color) ?: ItemStack.empty()
                    )
                }
            }

            is TextData -> {
                val td = (component.displayData as TextData)
                if (initial) {
                    println("TEXT! shadow: ${component.shadow}, seethrough: ${component.seeThrough}")
                    var b = 0
                    if (component.shadow) b = b or 1
                    if (component.seeThrough) b = b or 2

                    if (b != 0) {
                        entityData += EntityDataValue.create(26 + PACKET_OFFSET, DataSerializerTypes.BYTE, b.toByte())
                    }
                }
                
                if (flags.display) {
                    entityData += EntityDataValue.create(
                        22 + PACKET_OFFSET,
                        DataSerializerTypes.COMPONENT,
                        td.with(component.color and 0xFFFFFF)
                    )
                    entityData += EntityDataValue.create(
                        23 + PACKET_OFFSET,
                        DataSerializerTypes.INT,
                        td.lineWidth,
                    )

                    entityData += EntityDataValue.create(
                        24 + PACKET_OFFSET,
                        DataSerializerTypes.INT,
                        td.backgroundColor,
                    )
                }

                if (flags.transparency) {
                    entityData += EntityDataValue.create(
                        25 + PACKET_OFFSET,
                        DataSerializerTypes.BYTE,
                        ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
                    )
                }
            }

            else -> {
                return null
            }
        }

        entityData += EntityDataValue.create(8, DataSerializerTypes.INT, component.interpolationDelay)

        if (initial || flags.transformationInterpolation) {
            entityData += EntityDataValue.create(
                9,
                DataSerializerTypes.INT,
                component.transformationInterpolationDuration
            )
        }

        if ((initial || flags.teleportationDuration) && PACKET_OFFSET > 0) {
            entityData += EntityDataValue.create(10, DataSerializerTypes.INT, component.teleportationDuration)
        }

        if (initial) {
            entityData += EntityDataValue.create(
                14 + PACKET_OFFSET, DataSerializerTypes.BYTE,
                component.billboardConstraints.byte
            )
        }

        if (flags.scale) {
            if (!(initial && component.scale == defaultScale)) {
                entityData += EntityDataValue.create(
                    11 + PACKET_OFFSET, DataSerializerTypes.VECTOR3,
                    Vector3f(component.scale.x, component.scale.y, component.scale.z)
                )
            }
        }

        if (flags.rotation) {
            if (!(initial && component.rotation == defaultRotation)) {
                entityData += EntityDataValue.create(
                    12 + PACKET_OFFSET, DataSerializerTypes.QUATERNION,
                    Quaternionf(component.rotation.x, component.rotation.y, component.rotation.z, component.rotation.w)
                )
            }
        }

        if (flags.rotation || flags.scale) {
            val offset = Vector3f(
                -0.0125f,
                0.025f,
                0f
            )

            offset
                .mul(component.scale)
                .rotate(component.rotation)
                .add(component.translation)
            entityData += EntityDataValue.create(
                10 + PACKET_OFFSET, DataSerializerTypes.VECTOR3,
                Vector3f(
                    offset.x,
                    offset.y,
                    offset.z
                )
            )
        }

        if (initial && component.lightData != null) {
            entityData += EntityDataValue.create(
                15 + PACKET_OFFSET, DataSerializerTypes.INT,
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