package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.parsing.resourcepack.packages.PackageManager
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender
import java.io.File

object ReloadParticleScriptsCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        sender.sendMessage("Reloading Comet...")

        GlobalTicker.killInstances()

        PackageManager.compile()

        ParticleJsonParser.parseJsons()

        AbstractParticleEmitter.INSTANCE.saveResource("config.yml", false)
        AbstractParticleEmitter.INSTANCE.config.load(File(AbstractParticleEmitter.INSTANCE.dataFolder, "config.yml"))

        ResourcepackCreator.reload()

        sender.sendMessage("Comet has been reloaded!")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}