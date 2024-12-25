package gg.aquatic.comet.command

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CustomParticleCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("customparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false

        if (args != null && args.isNotEmpty()) {
            val spawnLoc = sender.eyeLocation
            ParticleJsonParser.jsonUnrealizedEmitters[args[0]]!!.realize(spawnLoc)
        } else {
            sender.sendMessage("You must specify an emitter to spawn!")
        }

        return true
    }
}