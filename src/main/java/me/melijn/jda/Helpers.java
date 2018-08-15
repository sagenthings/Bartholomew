package me.melijn.jda;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.commands.management.SetLogChannelCommand;
import me.melijn.jda.commands.util.SetNotifications;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.discordbots.api.client.DiscordBotListAPI;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Helpers {


    private static ScheduledExecutorService executorPool = Executors.newScheduledThreadPool(10);
    public static long lastRunTimer1, lastRunTimer2, lastRunTimer3;

    public static long starttime;
    public static String guildOnly = "This command is to be used in guilds only";
    public static String nsfwOnly = "This command is to be used in (not safe for work) better known as [NSFW] channels only and can contain 18+ content";
    public static String noPerms = "You don't have the permission: ";
    public static final Logger LOG = LogManager.getLogger(Melijn.class.getName());
    public static Color EmbedColor = Color.decode("#00ffd8");
    public static boolean voteChecks = true;
    public static int guildCount = 0;
    public static List<String> perms = Arrays.asList(
            "pause",
            "splay.yt",
            "splay.sc",
            "splay.link",
            "splay.*",
            "play.yt",
            "play.sc",
            "play.link",
            "skip",
            "skipx",
            "stop",
            "volume",
            "loop",
            "loopqueue",
            "queue",
            "clear",
            "loop",
            "resume",
            "userinfo",
            "np",
            "remove",
            "cat",
            "t2e",
            "perm.add",
            "perm.remove",
            "perm.clear",
            "perm.view",
            "perm.copy",
            "perm.*",
            "*",
            "play.*",
            "guildinfo",
            "role",
            "roles",
            "dog",
            "about",
            "ping",
            "setprefix",
            "setlogchannel",
            "tempban",
            "unban",
            "setmusicchannel",
            "setstreamermode",
            "setstreamurl",
            "warn",
            "purge",
            "ban",
            "history",
            "mute",
            "setmuterole",
            "tempmute",
            "unmute",
            "avatar",
            "potato",
            "filter",
            "pat",
            "slap",
            "triggered",
            "setjoinleavechannel",
            "setjoinmessage",
            "setleavemessage",
            "setjoinrole",
            "shuffle",
            "setmusiclogchannel",
            "setnotifications",
            "pitch",
            "tremelo",
            "nightcore",
            "lewd",
            "punch",
            "wasted",
            "highfive",
            "dab",
            "shrug",
            "cry",
            "kick",
            "bypass.sameVoiceChannel",
            "summon",
            "nyancat",
            "clearchannel",
            "shards",
            "setVerificationCode",
            "setVerificationChannel",
            "setUnverifiedRole",
            "setVerificationThreshold",
            "verify",
            "invert",
            "blurple",
            "urban",
            "bird",
            "enable",
            "disabled"
    );


    public static void startTimer(JDA jda, DiscordBotListAPI dbl, int i) {
        if (i == 0 || i == 1) {
            lastRunTimer1 = System.currentTimeMillis();
            executorPool.scheduleAtFixedRate(() -> {
                lastRunTimer1 = System.currentTimeMillis();
                try {
                    ResultSet bans = Melijn.mySQL.query("SELECT * FROM active_bans WHERE endTime < " + System.currentTimeMillis());
                    ResultSet mutes = Melijn.mySQL.query("SELECT * FROM active_mutes WHERE endTime < " + System.currentTimeMillis());
                    while (bans.next()) {
                        User toUnban = jda.retrieveUserById(bans.getLong("victimId")).complete();
                        try {
                            Guild guild = jda.asBot().getShardManager().getGuildById(bans.getLong("guildId"));
                            if (guild != null)
                                Melijn.mySQL.unban(toUnban, guild, jda.getSelfUser());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    while (mutes.next()) {
                        User toUnmute = jda.retrieveUserById(mutes.getLong("victimId")).complete();
                        try {
                            Guild guild = jda.asBot().getShardManager().getGuildById(mutes.getLong("guildId"));
                            if (guild != null)
                                Melijn.mySQL.unmute(guild, toUnmute, jda.getSelfUser());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, 1, 2, TimeUnit.SECONDS);
        } if (i == 0 || i == 2) {
            lastRunTimer2 = System.currentTimeMillis();
            executorPool.scheduleAtFixedRate(() -> {
                lastRunTimer2 = System.currentTimeMillis();
                if (dbl != null) dbl.setStats(guildCount == 0 ? jda.asBot().getShardManager().getGuilds().size() : guildCount);
                ArrayList<Long> votesList = Melijn.mySQL.getVoteList();
                for (long userId : SetNotifications.nextVotes.keySet()) {
                    for (long targetId : SetNotifications.nextVotes.get(userId)) {
                        if (votesList.contains(targetId)) {
                            jda.retrieveUserById(userId).queue((u) ->
                                    jda.retrieveUserById(targetId).queue((t) ->
                                            u.openPrivateChannel().queue((c) -> c.sendMessage(String.format("It's time to vote for **%#s**", t)).queue())));
                        }
                    }
                }
            }, 1, 60, TimeUnit.SECONDS);
        } if (i == 0 || i == 3) {
            lastRunTimer3 = System.currentTimeMillis();
            executorPool.scheduleAtFixedRate(() -> {
                lastRunTimer3 = System.currentTimeMillis();
                if (System.currentTimeMillis() - starttime > 10_000)
                    WebUtils.getWebUtilsInstance().updateSpotifyCredentials();
                Melijn.mySQL.updateVoteStreak();
            }, 0, 30, TimeUnit.MINUTES);
        }
    }

    public static boolean hasPerm(Member member, String permission, int level) {
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (level == 0) {
            if (Melijn.mySQL.noOneHasPermission(member.getGuild().getIdLong(), permission)) return true;
        }
        return Melijn.mySQL.hasPermission(member.getGuild(), member.getUser().getIdLong(), permission) || Melijn.mySQL.hasPermission(member.getGuild(), member.getUser().getIdLong(), "*");
    }

    public static void waitForIt(User user) {
        executorPool.schedule(() -> {
            MusicManager.usersRequest.remove(user);
            MusicManager.usersFormToReply.remove(user);
        }, 30, TimeUnit.SECONDS);
    }

    public static String getDurationBreakdown(long millis) {
        if (millis < 0L) {
            return "error";
        }
        if (millis > 43200000000L) return "LIVE";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days != 0) {
            sb.append(days);
            sb.append("d ");
        }
        if (hours != 0) {
            if (hours < 10) sb.append(0);
            sb.append(hours);
            sb.append(":");
        }
        if (minutes != 0) {
            if (minutes < 10) sb.append(0);
            sb.append(minutes);
            sb.append(":");
        }
        if (seconds < 10) sb.append(0);
        sb.append(seconds);
        sb.append("s");

        return (sb.toString());
    }

    private static ForkJoinPool executor = ForkJoinPool.commonPool();

    public static void ScheduleClose(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect()) return;
        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

    public static String getOnlineTime() {
        return getDurationBreakdown(System.currentTimeMillis() - starttime);
    }

    public static String getFooterStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public static String getFooterIcon() {
        return null;
    }

    public static String numberToString(int i) {
        switch (i) {
            case 0:
                return "zero";
            case 1:
                return "one";
            case 2:
                return "two";
            case 3:
                return "three";
            case 4:
                return "four";
            case 5:
                return "five";
            case 6:
                return "six";
            case 7:
                return "seven";
            case 8:
                return "eight";
            case 9:
                return "nine";
            default:
                return "zero";
        }
    }

    public static User getUserByArgs(CommandEvent event, String arg) {//Without null
        User user = getUserByArgsN(event, arg);
        if (user == null) user = event.getAuthor();
        return user;
    }
    public static User getUserByArgsN(CommandEvent event, String arg) {//With null
        User user = null;
        if (!arg.matches("\\s+") && !arg.equalsIgnoreCase("")) {
            if (event.getMessage().getMentionedUsers().size() > event.getOffset())
                user = event.getMessage().getMentionedUsers().get(event.getOffset());
            else if (arg.matches("\\d+") && event.getJDA().getUserById(arg) != null)
                user = event.getJDA().getUserById(arg);
            else if (event.getGuild() != null && event.getGuild().getMembersByName(arg, true).size() > event.getOffset())
                user = event.getGuild().getMembersByName(arg, true).get(event.getOffset()).getUser();
            else if (event.getGuild() != null && event.getGuild().getMembersByNickname(arg, true).size() > event.getOffset())
                user = event.getGuild().getMembersByNickname(arg, true).get(event.getOffset()).getUser();
        }
        return user;
    }

    public static long getTextChannelByArgsN(CommandEvent event, String arg) {
        long id = -1L;
        if (event.getMessage().getMentionedChannels().size() == 1) {
            id = event.getMessage().getMentionedChannels().get(0).getIdLong();
        } else if (arg.matches("\\d+") && event.getGuild().getTextChannelById(arg) != null) {
            id = Long.valueOf(arg);
        } else if (arg.equalsIgnoreCase("null")) {
            id = 0L;
        } else if (event.getGuild().getTextChannelsByName(arg, true).size() > 0) {
            id = event.getGuild().getTextChannelsByName(arg, true).get(0).getIdLong();
        }
        return id;
    }

    public static long getVoiceChannelByArgsN(CommandEvent event, String arg) {
        long id = -1L;
        if (event.getMessage().getMentionedChannels().size() == 1) {
            id = event.getMessage().getMentionedChannels().get(0).getIdLong();
        } else if (arg.matches("\\d+") && event.getGuild().getVoiceChannelById(arg) != null) {
            id = Long.valueOf(arg);
        } else if (arg.equalsIgnoreCase("null")) {
            id = 0L;
        } else if (event.getGuild().getVoiceChannelsByName(arg, true).size() > 0) {
            id = event.getGuild().getVoiceChannelsByName(arg, true).get(0).getIdLong();
        }
        return id;
    }

    public static void postMusicLog(MusicPlayer player, AudioTrack track) {
        if (SetLogChannelCommand.musicLogChannelMap.containsKey(player.getGuild().getIdLong())) {
            TextChannel tc = player.getGuild().getTextChannelById(SetLogChannelCommand.musicLogChannelMap.get(player.getGuild().getIdLong()));
            if (tc == null) {
                SetLogChannelCommand.musicLogChannelMap.remove(player.getGuild().getIdLong());
                Melijn.mySQL.removeChannel(player.getGuild().getIdLong(), ChannelType.MUSIC_LOG);
                return;
            }
            if (tc.canTalk())
                tc.sendMessage(new EmbedBuilder()
                        .setTitle("Now playing")
                        .setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** `" + Helpers.getDurationBreakdown(track.getDuration()) + "`\n")
                        .setThumbnail(MessageHelper.getThumbnailURL(track.getInfo().uri))
                        .setColor(Helpers.EmbedColor)
                        .setFooter(Helpers.getFooterStamp(), null)
                        .build()).queue();
        }
    }

    public static Role getRoleByArgs(CommandEvent event, String arg) {
        if (!arg.matches("\\s+") && !arg.equalsIgnoreCase("")) {
            if (event.getMessage().getMentionedRoles().size() > 0) return event.getMessage().getMentionedRoles().get(0);
            else if (arg.matches("\\d+") && event.getGuild().getRoleById(arg) != null)
                return event.getGuild().getRoleById(arg);
            else if (event.getGuild() != null && event.getGuild().getRolesByName(arg, true).size() > 0)
                return event.getGuild().getRolesByName(arg, true).get(0);
        }
        return null;
    }

    public static void retrieveUserByArgs(CommandEvent event, String arg, Consumer<User> success) {
        executor.execute(() -> {
            User user = getUserByArgsN(event, arg);
            if (user == null && arg.matches("\\d+") && event.getJDA().getUserById(arg) == null) user = event.getJDA().retrieveUserById(arg).complete();
            if (user == null) user = event.getAuthor();
            success.accept(user);
        });
    }

    public static void retrieveUserByArgsN(CommandEvent event, String arg, Consumer<User> success) {
        executor.execute(() -> {
            User user = getUserByArgsN(event, arg);
            if (user == null && arg.matches("\\d+") && event.getJDA().getUserById(arg) == null) success.accept(event.getJDA().retrieveUserById(arg).complete());
            else success.accept(user);
        });
    }

    public static long parseTimeFromArgs(String[] args) {
        long millis = -1;
        switch (args.length) {
            case 0:
                break;
            case 1: {
                if (args[0].matches("(\\d)|(\\d\\d)")) {
                    millis = 1000 * Short.parseShort(args[0]);
                }
                break;
            }
            case 2: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)")) {
                    millis = 60000 * Short.parseShort(args[0]) + 1000 * Short.parseShort(args[1]);
                }
                break;
            }
            case 3: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                    millis = 3600000 * Short.parseShort(args[0]) + 60000 * Short.parseShort(args[1]) + 1000 * Short.parseShort(args[2]);
                }
                break;
            }
            default: {
                if (args[0].matches("(\\d)|(\\d\\d)") && args[1].matches("(\\d)|(\\d\\d)") && args[2].matches("(\\d)|(\\d\\d)")) {
                    millis = 3600000 * Short.parseShort(args[0]) + 60000 * Short.parseShort(args[1]) + 1000 * Short.parseShort(args[2]);
                }
                break;
            }
        }
        return millis;
    }

    public static boolean canNotInteract(CommandEvent event, User target) {
        if (event.getGuild().getMember(target).getRoles().size() > 0 && event.getGuild().getSelfMember().getRoles().size() > 0) {
            if (!event.getGuild().getSelfMember().getRoles().get(0).canInteract(event.getGuild().getMember(target).getRoles().get(0))) {
                event.reply("Can't modify a member with higher or equal highest role than myself");
                return true;
            }
        } else if (event.getGuild().getSelfMember().getRoles().size() == 0) {
            event.reply("Can't modify a member with higher or equal highest role than myself");
            return true;
        }
        return false;
    }

    public static boolean canNotInteract(CommandEvent event, Role target) {
        if (event.getGuild().getSelfMember().getRoles().size() > 0) {
            if (!event.getGuild().getSelfMember().getRoles().get(0).canInteract(target)) {
                event.reply("Can't modify a member with higher or equal highest role than myself");
                return true;
            }
        } else {
            event.reply("Can't modify a member with higher or equal highest role than myself");
            return true;
        }
        return false;
    }
}