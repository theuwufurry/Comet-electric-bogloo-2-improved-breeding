package com.ixume.particleemitter.particle.data

import com.ixume.particleemitter.particle.display.DisplayData
import com.ixume.particleemitter.particle.display.model.ModelData
import com.ixume.particleemitter.particle.display.sprite.SpriteData
import com.mojang.math.Transformation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
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

object EntityDataBuilder {
    private val cache: MutableMap<EntityData, SynchedEntityData> = mutableMapOf()
    private val level: Level = (Bukkit.getWorld("world") as CraftWorld).handle
    private val dataComponentType =
        BuiltInRegistries.DATA_COMPONENT_TYPE.get(
            ResourceLocation.tryBuild(
                "minecraft",
                "custom_model_data"
            )
        ) as DataComponentType<CustomModelData>

    fun getDataFor(entityData: EntityData): SynchedEntityData {
        return genData(entityData)
//        performance of this is questionable
//        return cache[componentData]?.let { return it } ?: genData(componentData).also { cache += (componentData to it) }
    }

    private fun genData(component: EntityData): SynchedEntityData {
        val entity: Display
        if (component.displayData is SpriteData) {
            entity = TextDisplay(EntityType.TEXT_DISPLAY, level)
            entity.text = Component.Serializer.fromJson(
                JSONComponentSerializer.json().serialize(
                    net.kyori.adventure.text.Component.translatable(component.displayData.id).font(
                        Key.key("particlecreator.${component.color ushr 24}:default")
                    ).color(TextColor.color(component.color and 0xFFFFFF))
                ), RegistryAccess.ImmutableRegistryAccess.EMPTY
            )!!
            entity.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, 0)
        } else {
            entity = ItemDisplay(EntityType.ITEM_DISPLAY, level)
            val modelData = component.displayData as ModelData
            entity.itemStack = ItemStack(BuiltInRegistries.ITEM[ResourceLocation.tryBuild("minecraft", modelData.item)])
            entity.itemStack.applyComponents(
                DataComponentMap.builder().set(
                    dataComponentType, CustomModelData(modelData.id)
                ).build()
            )
        }

        entity.billboardConstraints = component.billboardConstraints
        entity.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)
        entity.setTransformation(Transformation(component.matrix))
        entity.transformationInterpolationDuration = 2
        entity.transformationInterpolationDelay = -1
        return entity.entityData
    }
}

data class EntityData(
    val displayData: DisplayData,
    val color: Int,
    val matrix: Matrix4f,
    val billboardConstraints: BillboardConstraints
)