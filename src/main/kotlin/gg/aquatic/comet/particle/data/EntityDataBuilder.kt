package gg.aquatic.comet.particle.data

//import com.mojang.math.Transformation
//import net.minecraft.core.component.DataComponentMap
//import net.minecraft.core.component.DataComponentType
//import net.minecraft.core.registries.BuiltInRegistries
//import net.minecraft.network.chat.Component
//import net.minecraft.network.chat.Style
//import net.minecraft.network.syncher.SynchedEntityData
//import net.minecraft.resources.ResourceLocation
//import net.minecraft.world.entity.Display
//import net.minecraft.world.entity.Display.*
//import net.minecraft.world.entity.EntityType
//import net.minecraft.world.item.ItemStack
//import net.minecraft.world.item.component.CustomModelData
//import net.minecraft.world.level.Level
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

class EntityDataBuilder {
    private val key = Key.key("particlecreator", "default")

    fun getDataFor(
        entityData: EntityData,
        initial: Boolean
    ): List<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData> {
        return genData(entityData, initial)
    }

    private fun genData(
        component: EntityData,
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

                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    22 + PACKET_OFFSET,
                    EntityDataTypes.ADV_COMPONENT,
                    Component.translatable(component.displayData.id)
                        .color(TextColor.color(component.color and 0xFFFFFF)).font(key)
                )

                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    25 + PACKET_OFFSET,
                    EntityDataTypes.BYTE,
                    ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
                )
            }

            is ModelData -> {
                val modelData = component.displayData

                val stack = ItemStack.builder().type(ItemTypes.getByName(modelData.item)).amount(1).build()
                stack.setComponent(ComponentTypes.CUSTOM_MODEL_DATA, modelData.id)

                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    22 + PACKET_OFFSET,
                    EntityDataTypes.ITEMSTACK,
                    stack
                )
            }

            else -> {
                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    22 + PACKET_OFFSET,
                    EntityDataTypes.ADV_COMPONENT,
                    Component.translatable((component.displayData as TextDisplayComponent).string)
                        .color(TextColor.color(component.color and 0xFFFFFF)).font(key)
                )
                entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
                    25 + PACKET_OFFSET,
                    EntityDataTypes.BYTE,
                    ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
                )
            }
        }

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            8,
            EntityDataTypes.INT,
            component.interpolationDelay
        )

        if (initial) {
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

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            14 + PACKET_OFFSET,
            EntityDataTypes.BYTE,
            component.billboardConstraints.byte
        )

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            10 + PACKET_OFFSET,
            EntityDataTypes.VECTOR3F,
            gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f(
                component.translation.x,
                component.translation.y,
                component.translation.z
            )
        )

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            11 + PACKET_OFFSET,
            EntityDataTypes.VECTOR3F,
            gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f(
                component.scale.x,
                component.scale.y,
                component.scale.z
            )
        )

        entityData += gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData(
            12 + PACKET_OFFSET,
            EntityDataTypes.QUATERNION,
            Quaternion4f(component.rotation.x, component.rotation.y, component.rotation.z, component.rotation.w)
        )

        return entityData
    }
}

data class EntityData(
    val displayData: DisplayData,
    val color: Int,
    val translation: Vector3f,
    val rotation: Quaternionf,
    val scale: Vector3f,
    val billboardConstraints: BillboardConstraints,
    val interpolationDelay: Int,
    val interpolationDuration: Int
)

enum class BillboardConstraints(val byte: Byte) {
    FIXED((0).toByte()),
    VERTICAL((1).toByte()),
    HORIZONTAL((2).toByte()),
    CENTER((3).toByte())
}