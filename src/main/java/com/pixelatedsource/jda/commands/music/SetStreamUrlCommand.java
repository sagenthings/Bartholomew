package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetStreamUrlCommand extends Command {


    public SetStreamUrlCommand() {
        this.commandName = "setstreamurl";
        this.description = "set the stream url of the bot";
        this.usage = PREFIX + commandName + " [stream url]";
        this.aliases = new String[]{"ssu"};
        this.category = Category.MUSIC;
    }

    HashMap<String, String> linkjes = new HashMap<String, String>() {{
        put("slam-nonstop", "http://stream.radiocorp.nl/web10_mp3");
        put("radio538", "http://18973.live.streamtheworld.com/RADIO538.mp3");
    }};

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                String url = PixelSniper.mySQL.getStreamUrl(guild);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply(url);
                } else if (args.length == 1) {
                    if (args[0].contains("http://") || args[0].contains("https://")) {
                        if (PixelSniper.mySQL.setStreamUrl(guild, args[0])) {
                            event.reply("Changed the url from " + url + " to " + PixelSniper.mySQL.getStreamUrl(guild));
                        }
                    } else {
                        if (args[0].equalsIgnoreCase("list")) {
                            event.reply(linkjes.toString());
                        } else {
                            if (linkjes.keySet().contains(args[0])) {
                                if (PixelSniper.mySQL.setStreamUrl(guild, linkjes.get(args[0]))) {
                                    event.reply("Changed the url from " + url + " to " + PixelSniper.mySQL.getStreamUrl(guild));
                                }
                            } else {
                                event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                            }
                        }
                    }
                } else {
                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
