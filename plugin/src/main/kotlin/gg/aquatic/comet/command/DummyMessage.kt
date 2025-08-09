package gg.aquatic.comet.command

import gg.aquatic.waves.util.message.Message
import org.bukkit.command.CommandSender

object DummyMessage : Message {
    override val messages: Collection<String> = listOf()

    override fun replace(updater: (String) -> String): Message {
        return this
    }

    override fun replace(from: String, to: String): Message {
        return this
    }

    override fun send(sender: CommandSender) { }

    override fun broadcast() {
    }
}