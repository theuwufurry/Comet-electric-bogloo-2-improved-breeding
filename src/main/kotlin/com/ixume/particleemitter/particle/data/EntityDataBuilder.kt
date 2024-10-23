package com.ixume.particleemitter.particle.data

import com.mojang.math.Transformation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import net.minecraft.core.RegistryAccess
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.joml.Matrix4f

object EntityDataBuilder {
    private val cache: MutableMap<ComponentData, SynchedEntityData> = mutableMapOf()
    private val level: Level = (Bukkit.getWorld("world") as CraftWorld).handle

    fun getDataFor(componentData: ComponentData): SynchedEntityData {
        return genData(componentData)
//        performance of this is questionable
//        return cache[componentData]?.let { return it } ?: genData(componentData).also { cache += (componentData to it) }
    }

    private fun genData(component: ComponentData): SynchedEntityData {
        val entity = TextDisplay(EntityType.TEXT_DISPLAY, level)
        entity.text = Component.Serializer.fromJson(JSONComponentSerializer.json().serialize(net.kyori.adventure.text.Component.translatable(component.translation).font(
            Key.key("particlecreator.${component.color ushr 24}:default")).color(TextColor.color(component.color and 0xFFFFFF))), RegistryAccess.ImmutableRegistryAccess.EMPTY)!!
        entity.billboardConstraints = Display.BillboardConstraints.CENTER
        entity.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, 0)
        entity.entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)
        entity.setTransformation(Transformation(component.matrix))
        entity.transformationInterpolationDuration = 1
        entity.transformationInterpolationDelay = -1
        return entity.entityData
    }
}

data class ComponentData(val translation: String, val color: Int, val matrix: Matrix4f)