package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO


class SayCommand : AbstractCommand("command.say") {

    init {
        id = 115
        name = "say"
        aliases = arrayOf("zeg")
        discordChannelPermissions = arrayOf(
            Permission.MESSAGE_ATTACH_FILES
        )
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
        }

        val fontSize = getIntegerFromArgN(context, 0)
        val input = if (fontSize == null) {
            context.rawArg
        } else {
            context.rawArg.removeFirst("$fontSize").trim()
        }

        val resourceName = if (context.commandParts[1].equals("zeg", true)) {
            "melijn_zegt.jpg"
        } else {
            "melijn_says.jpg"
        }

        val fileBytes = SayCommand::class.java.getResource("/$resourceName").readBytes()
        val image = ImageIO.read(ByteArrayInputStream(fileBytes))
        val graphics = image.graphics
        val font = graphics.font.deriveFont(fontSize?.toFloat() ?: 60f)
        graphics.font = font
        graphics.color = Color.DARK_GRAY
        val startX = 1133
        val endX = 1800
        val startY = 82
        //val endY = 1000
        val endImg = ImageUtils.putText(image, input, startX, endX, startY, graphics)

        sendMsg(context, endImg, "png")
    }
}