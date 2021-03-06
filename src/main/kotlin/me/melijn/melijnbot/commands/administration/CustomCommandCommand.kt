package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*

const val CUSTOM_COMMAND_LIMIT = 10
const val PREMIUM_CUSTOM_COMMAND_LIMIT = 100
const val PREMIUM_CC_LIMIT_PATH = "premium.feature.cc.limit"

class CustomCommandCommand : AbstractCommand("command.customcommand") {

    init {
        id = 36
        name = "customCommand"
        aliases = arrayOf("cc")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            AliasesArg(root),
            RemoveArg(root),
            SelectArg(root),
            SetChanceArg(root),
            SetPrefixStateArg(root),
            SetDescriptionArg(root),
            ResponseArg(root),
            InfoArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, Long>()
        suspend fun getSelectedCCNMessage(context: CommandContext): CustomCommand? {
            val pair = Pair(context.guildId, context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val id = selectionMap[pair]
                val ccs = context.daoManager.customCommandWrapper.customCommandCache.get(context.guildId).await()
                    .filter { (ccId) -> ccId == id }
                if (ccs.isNotEmpty()) {
                    ccs[0]
                } else {
                    val msg = context.getTranslation("message.ccremoved")
                        .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                    sendMsg(context, msg)
                    null
                }
            } else {
                val msg = context.getTranslation("message.noccselected")
                    .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendMsg(context, msg)
                null
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
            aliases = arrayOf("information")
        }

        override suspend fun execute(context: CommandContext) {
            val id = getLongFromArgN(context, 0)
            var cc = context.daoManager.customCommandWrapper.getCCById(context.guildId, id)
            if (cc == null && context.args.isNotEmpty()) {

                val msg = context.getTranslation("message.unknown.ccid")
                    .replace(PLACEHOLDER_ARG, id.toString())
                sendMsg(context, msg)
                return
            } else if (cc == null) {
                cc = getSelectedCCNMessage(context) ?: return
            }

            val title = context.getTranslation("$root.title")
            val description = context.getTranslation("$root.description")
                .replace("%ccName%", cc.name)
            val eb = Embedder(context)
            eb.setTitle(title)
            eb.setDescription(description)
            sendEmbed(context, eb.build())
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val title = context.getTranslation("$root.title")

            val ccs = context.daoManager.customCommandWrapper.customCommandCache.get(context.guildId).await()
            var content = "```INI"

            content += "\n[id] - [name] - [chance]"
            for (cc in ccs) {
                content += "\n[${cc.id}] - ${cc.name} - ${cc.chance}"
            }
            content += "```"

            val msg = title + content
            sendMsgCodeBlock(context, msg, "INI")
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            val args = context.rawArg.split("\\s*>\\s*".toRegex())
            if (args.size < 2) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.customCommandWrapper
            val size = wrapper.customCommandCache[context.guildId].await().size
            if (size >= CUSTOM_COMMAND_LIMIT && !isPremiumGuild(context)) {
                sendFeatureRequiresGuildPremiumMessage(context, PREMIUM_CC_LIMIT_PATH)
                return
            }

            if (size >= PREMIUM_CUSTOM_COMMAND_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .replace("%limit%", PREMIUM_CUSTOM_COMMAND_LIMIT.toString())
                sendMsg(context, msg)
            }

            val name = args[0]
            val content = if (args[1].isBlank()) {
                "empty"
            } else {
                args[1]
            }

            val cc = CustomCommand(0, name, ModularMessage(content))

            val ccId = context.daoManager.customCommandWrapper.add(context.guildId, cc)
            cc.id = ccId

            val msg = context.getTranslation("$root.success")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)
                .replace("%content%", cc.content.messageContent ?: "error")
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("delete", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val guildId = context.guildId

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.customCommandCache.get(guildId).await()
                .firstOrNull { (ccId) -> ccId == id }

            if (cc == null) {
                val msg = context.getTranslation("$root.failed")
                    .replace("%id%", id.toString())
                    .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendMsg(context, msg)
                return
            }

            context.daoManager.customCommandWrapper.remove(guildId, id)

            val msg = context.getTranslation("$root.success")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)

            sendMsg(context, msg)
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val guildId = context.guildId

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.getCCById(guildId, id)
            if (cc == null) {
                val msg = context.getTranslation("message.unknown.ccid")
                    .replace(PLACEHOLDER_ARG, id.toString())
                sendMsg(context, msg)
                return
            }

            selectionMap[Pair(guildId, context.authorId)] = id


            val msg = context.getTranslation("$root.selected")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)
            sendMsg(context, msg)

        }
    }


    class AliasesArg(parent: String) : AbstractCommand("$parent.aliases") {

        init {
            name = "aliases"
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                ListArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class AddArg(root: String) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                s.add(context.rawArg)
                ccSelected.aliases = s.toList()

                context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

                val msg = context.getTranslation("$root.success")
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)
                    .replace(PLACEHOLDER_ARG, context.rawArg)

                sendMsg(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm", "rem", "delete")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                val possibleLong = getIntegerFromArgN(context, 0) ?: s.indexOf(context.rawArg)

                val alias: String

                if (possibleLong == -1) {
                    sendSyntax(context)
                    return
                } else {
                    alias = s[possibleLong]
                    s.removeAt(possibleLong)
                }
                ccSelected.aliases = s.toList()

                context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

                val msg = context.getTranslation("$root.success")
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)
                    .replace("%position%", possibleLong.toString())
                    .replace(PLACEHOLDER_ARG, alias)

                sendMsg(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val ccSelected = getSelectedCCNMessage(context) ?: return
                val aliases = ccSelected.aliases

                val path = if (aliases == null) "$root.empty" else "$root.title"
                val title = context.getTranslation(path)
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)

                val content = if (aliases == null) {
                    ""
                } else {
                    var build = "```INI"
                    for ((index, alias) in aliases.withIndex()) {
                        build += "\n[$index] - $alias"
                    }
                    "$build```"
                }

                val msg = title + content

                sendMsg(context, msg)
            }
        }
    }

    class SetDescriptionArg(parent: String) : AbstractCommand("$parent.setdescription") {

        init {
            name = "setDescription"
            aliases = arrayOf("setDesc", "sd")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val unset = context.rawArg == "null"
            val ccSelected = getSelectedCCNMessage(context) ?: return
            ccSelected.description = if (context.rawArg == "null") null else context.rawArg

            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val msg = context.getTranslation("$root.${if (unset) "unset" else "set"}")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)
                .replace(PLACEHOLDER_ARG, context.rawArg)

            sendMsg(context, msg)
        }
    }

    class SetChanceArg(parent: String) : AbstractCommand("$parent.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val chance = getIntegerFromArgNMessage(context, 0) ?: return
            ccSelected.chance = chance

            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val msg = context.getTranslation("$root.success")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)
                .replace(PLACEHOLDER_ARG, chance.toString())

            sendMsg(context, msg)
        }

    }

    class SetPrefixStateArg(parent: String) : AbstractCommand("$parent.setprefixstate") {

        init {
            name = "setPrefixState"
            aliases = arrayOf("sps")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val state = getBooleanFromArgNMessage(context, 0) ?: return
            ccSelected.prefix = state


            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val pathPart = if (state) "enabled" else "disabled"
            val msg = context.getTranslation("$root.$pathPart")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)

            sendMsg(context, msg)
        }

    }

    class ResponseArg(parent: String) : AbstractCommand("$parent.response") {

        init {
            name = "response"
            aliases = arrayOf("r")
            children = arrayOf(
                SetContentArg(root),
                EmbedArg(root),
                AttachmentsArg(root),
                ViewArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class ViewArg(parent: String) : AbstractCommand("$parent.view") {
            init {
                name = "view"
                aliases = arrayOf("preview", "show", "info")
            }

            override suspend fun execute(context: CommandContext) {
                val cc = getSelectedCCNMessage(context) ?: return
                MessageCommandUtil.showMessagePreviewCC(context, cc)
            }
        }

        class SetContentArg(parent: String) : AbstractCommand("$parent.setcontent") {

            init {
                name = "setContent"
                aliases = arrayOf("sc")
            }

            override suspend fun execute(context: CommandContext) {
                val cc = getSelectedCCNMessage(context) ?: return
                val property = ModularMessageProperty.CONTENT
                if (context.args.isEmpty()) {
                    MessageCommandUtil.showMessageCC(context, property, cc)
                } else {
                    MessageCommandUtil.setMessageCC(context, property, cc)
                }
            }
        }

        class EmbedArg(parent: String) : AbstractCommand("$parent.embed") {

            init {
                name = "embed"
                aliases = arrayOf("e")
                children = arrayOf(
                    ClearArg(root),
                    SetDescriptionArg(root),
                    SetColorArg(root),
                    SetTitleArg(root),
                    SetTitleUrlArg(root),
                    SetAuthorArg(root),
                    SetAuthorIconArg(root),
                    SetAuthorUrlArg(root),
                    SetThumbnailArg(root),
                    SetImageArg(root),
                    FieldArg(root),
                    SetFooterArg(root),
                    SetFooterIconArg(root),
                    SetTimeStampArg(root)
                    //What even is optimization
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

                init {
                    name = "setTitle"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_TITLE
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetTitleUrlArg(parent: String) : AbstractCommand("$parent.settitleurl") {

                init {
                    name = "setTitleUrl"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class SetAuthorArg(parent: String) : AbstractCommand("$parent.setauthor") {

                init {
                    name = "setAuthor"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetAuthorIconArg(parent: String) : AbstractCommand("$parent.setauthoricon") {

                init {
                    name = "setAuthorIcon"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_ICON_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetAuthorUrlArg(parent: String) : AbstractCommand("$parent.setauthorurl") {

                init {
                    name = "setAuthorUrl"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class SetThumbnailArg(parent: String) : AbstractCommand("$parent.setthumbnail") {

                init {
                    name = "setThumbnail"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_THUMBNAIL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetImageArg(parent: String) : AbstractCommand("$parent.setimage") {

                init {
                    name = "setImage"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_IMAGE
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class FieldArg(parent: String) : AbstractCommand("$parent.field") {

                init {
                    name = "field"
                    children = arrayOf(
                        AddArg(root),
                        RemoveArg(root),
                        ListArg(root),
                        SetTitleArg(root),
                        SetValueArg(root),
                        SetInlineArg(root)
                    )
                }

                override suspend fun execute(context: CommandContext) {
                    sendSyntax(context)
                }

                class AddArg(parent: String) : AbstractCommand("$parent.add") {

                    init {
                        name = "add"
                        aliases = arrayOf("addInline")
                    }

                    override suspend fun execute(context: CommandContext) {
                        val split = context.rawArg.split(">")
                        if (split.size < 2) {
                            sendSyntax(context)
                        }
                        val title = split[0]
                        val value = context.rawArg.removeFirst("$title>")

                        val inline = context.commandParts[1].equals("addInline", true)
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.addEmbedFieldCC(title, value, inline, context, cc)
                    }
                }

                class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

                    init {
                        name = "setTitle"
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val title = context.rawArg
                            .removeFirst("$index")
                            .trim()
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldTitleCC(index, title, context, cc)
                    }
                }

                class SetValueArg(parent: String) : AbstractCommand("$parent.setvalue") {

                    init {
                        name = "setValue"
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = context.rawArg
                            .removeFirst("$index")
                            .trim()
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldValueCC(index, value, context, cc)
                    }
                }

                class SetInlineArg(parent: String) : AbstractCommand("$parent.setinline") {

                    init {
                        name = "setInline"
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = getBooleanFromArgNMessage(context, 1) ?: return
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldInlineCC(index, value, context, cc)
                    }
                }

                class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                    init {
                        name = "remove"
                        aliases = arrayOf("rm", "rem", "delete")
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.isEmpty()) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.removeEmbedFieldCC(index, context, cc)
                    }
                }

                class ListArg(parent: String) : AbstractCommand("$parent.list") {

                    init {
                        name = "list"
                        aliases = arrayOf("ls")
                    }

                    override suspend fun execute(context: CommandContext) {
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.showEmbedFieldsCC(context, cc)
                    }
                }
            }


            class SetDescriptionArg(parent: String) : AbstractCommand("$parent.setdescription") {

                init {
                    name = "setDescription"
                    aliases = arrayOf("setDesc")
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_DESCRIPTION
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetColorArg(parent: String) : AbstractCommand("$parent.setcolor") {

                init {
                    name = "setColor"
                    aliases = arrayOf("setColour")
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_COLOR
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetFooterArg(parent: String) : AbstractCommand("$parent.setfooter") {

                init {
                    name = "setFooter"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetFooterIconArg(parent: String) : AbstractCommand("$parent.setfootericon") {

                init {
                    name = "setFooterIcon"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER_ICON_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetTimeStampArg(parent: String) : AbstractCommand("$parent.settimestamp") {

                init {
                    name = "setTimeStamp"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_TIME_STAMP
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.clearEmbedCC(context, cc)
                }
            }
        }

        class AttachmentsArg(parent: String) : AbstractCommand("$parent.attachments") {

            init {
                name = "attachments"
                aliases = arrayOf("a")
                children = arrayOf(
                    ListArg(root),
                    AddArg(root),
                    RemoveArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class ListArg(parent: String) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                    arrayOf("ls")
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.listAttachmentsCC(context, cc)
                }
            }

            class AddArg(parent: String) : AbstractCommand("$parent.add") {

                init {
                    name = "add"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }
                    MessageCommandUtil.addAttachmentCC(context, cc)
                }

            }

            class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm", "rem", "delete")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.removeAttachmentCC(context, cc)
                }
            }
        }
    }
}