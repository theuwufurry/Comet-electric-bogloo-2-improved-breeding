package com.ixume.particleemitter.command

import com.ixume.particleemitter.ParticleEmitter
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.joml.Matrix4f
import org.joml.Vector4f

object OffsetTest : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("offsettest")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false

        val world = sender.world
        val entity = world.spawnEntity(Location(world, -174.0, -57.0, 84.0), EntityType.TEXT_DISPLAY) as TextDisplay
        entity.text(
            net.kyori.adventure.text.Component.translatable("test").font(
                Key.key("particlecreator.254:default")
            )
        )

//        entity.billboard = Display.Billboard.CENTER

        val matrix = Matrix4f()

        //-0.0125 for fixed
        val offset = Vector4f(-0.0125f, 0f, 0f, 1.0f)
        matrix.scale(17.8325f)
        matrix.rotateXYZ(
            (Math.random() * Math.PI * 2f).toFloat(), (Math.random() * Math.PI * 2f).toFloat(),
            (Math.random() * Math.PI * 2f).toFloat()
        )

//        matrix.transform(offset)
        matrix.translate(offset.x, offset.y, offset.z)
        entity.setTransformationMatrix(matrix)

        return true
    }
}