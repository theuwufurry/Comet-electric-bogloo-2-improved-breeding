package com.ixume.particleemitter.particle.data

import com.ixume.particleemitter.particle.display.DisplayData
import com.ixume.particleemitter.particle.display.TextDisplayComponent
import com.ixume.particleemitter.particle.display.model.ModelData
import com.ixume.particleemitter.particle.display.sprite.SpriteData
import com.mojang.math.Transformation
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.level.Level
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.joml.Matrix4f

class EntityDataBuilder {
    private val level: Level = (Bukkit.getWorld("world") as CraftWorld).handle
    private val dataComponentType =
        BuiltInRegistries.DATA_COMPONENT_TYPE.get(
            ResourceLocation.tryBuild(
                "minecraft",
                "custom_model_data"
            )
        ) as DataComponentType<CustomModelData>

    private val placeholderTextDisplay: TextDisplay = TextDisplay(EntityType.TEXT_DISPLAY, level)
    private val placeholderItemDisplay: ItemDisplay = ItemDisplay(EntityType.ITEM_DISPLAY, level)

    init {
        placeholderTextDisplay.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, 0)

        placeholderTextDisplay.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 2)
        placeholderItemDisplay.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 2)


        placeholderTextDisplay.transformationInterpolationDuration = 2
        placeholderTextDisplay.transformationInterpolationDelay = -1

        placeholderItemDisplay.transformationInterpolationDuration = 2
        placeholderItemDisplay.transformationInterpolationDelay = -1
    }

    fun getDataFor(entityData: EntityData): SynchedEntityData? {
        return genData(entityData)
    }

    private fun genData(component: EntityData): SynchedEntityData? {
        val entity: Display
        when (component.displayData) {
            is SpriteData -> {
                entity = placeholderTextDisplay
                entity.text = Component.translatable(component.displayData.id).withColor(component.color and 0xFFFFFF)
                    .withStyle(
                        Style.EMPTY.withFont(
                            ResourceLocation(
                                "particlecreator",
                                "default"
                            )
                        )
                    )

                entity.textOpacity = ((component.color ushr 24) + 26).coerceAtMost(255).toByte()
    //            entity.entityData.set(TextDisplay, component.color ushr 24)
            }

            is ModelData -> {
                entity = placeholderItemDisplay
                val modelData = component.displayData
                entity.itemStack = ItemStack(BuiltInRegistries.ITEM[ResourceLocation.tryBuild("minecraft", modelData.item)])
                entity.itemStack.applyComponents(
                    DataComponentMap.builder().set(
                        dataComponentType, CustomModelData(modelData.id)
                    ).build()
                )
            }

            else -> {
                entity = placeholderTextDisplay
                entity.text = Component.literal((component.displayData as TextDisplayComponent).string).withColor(component.color and 0xFFFFFF)
                entity.textOpacity = ((component.color ushr 24) + 14).coerceAtMost(255).toByte()
            }
        }

        entity.billboardConstraints = component.billboardConstraints
        entity.setTransformation(Transformation(component.matrix))
        return entity.entityData
    }
}

data class EntityData(
    val displayData: DisplayData,
    val color: Int,
    val matrix: Matrix4f,
    val billboardConstraints: BillboardConstraints
)