package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendMsg


class PingCommand : AbstractCommand("command.ping") {

    init {
        id = 1
        name = "ping"
        aliases = arrayOf("pong", "latency")
        children = arrayOf(
            PongCommand(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()

        val part1 = context.getTranslation("$root.response1.part1")
            .replace("%gatewayPing%", context.jda.gatewayPing.toString())

        val part2 = context.getTranslation("$root.response1.part2")
        val part3 = context.getTranslation("$root.response1.part3")

        val message = sendMsg(context, part1)
        val timeStamp2 = System.currentTimeMillis()
        val msgPing = timeStamp2 - timeStamp1
        val restPing = context.jda.restPing.await()

        val editedMessage = message[0].editMessage("${message[0].contentRaw}${replacePart2(part2, restPing, msgPing)}").await()
        val timeStamp3 = System.currentTimeMillis()
        val eMsgPing = timeStamp3 - timeStamp2
        editedMessage.editMessage("${editedMessage.contentRaw}${replacePart3(part3, eMsgPing)}").queue()
    }


    private fun replacePart2(string: String, restPing: Long, sendMessagePing: Long): String = string
        .replace("%restPing%", "$restPing")
        .replace("%sendMessagePing%", "$sendMessagePing")


    private fun replacePart3(string: String, editMessagePing: Long): String = string
        .replace("%editMessagePing%", "$editMessagePing")


    private class PongCommand(parent: String) : AbstractCommand("$parent.pong") {

        init {
            name = "pong"
            children = arrayOf(
                DunsteCommand(root)
            )
            aliases = arrayOf("ping")
        }

        override suspend fun execute(context: CommandContext) {
            val msg = context.getTranslation("$root.response1")
            sendMsg(context, msg)
        }

        private class DunsteCommand(parent: String) : AbstractCommand("$parent.dunste") {

            init {
                name = "dunste"
                aliases = arrayOf("duncte")
            }

            override suspend fun execute(context: CommandContext) {
                val msg = context.getTranslation("$root.response1")
                sendMsg(context, msg)
            }
        }
    }
}