package gg.aquatic.comet.command

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object FireParticleSpawnCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("fireparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false
        ParticleJsonParser.jsonUnrealizedEmitters["fire_bright"]!!.realize(sender.location)
        ParticleJsonParser.jsonUnrealizedEmitters["fire_spark"]!!.realize(sender.location)
        ParticleJsonParser.jsonUnrealizedEmitters["fire_smoke"]!!.realize(sender.location)
        return true
    }
}