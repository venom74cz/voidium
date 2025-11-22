package cz.voidium.config;

import java.util.HashMap;
import java.util.Map;

public class LocalePresets {
    
    public static Map<String, String> getDiscordMessages(String locale) {
        Map<String, String> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("kickMessage", "&cNejsi na whitelistu!\\n&7Pro pripojeni se musis overit na nasem Discordu.\\n&7Tvuj overovaci kod: &b%code%");
            messages.put("linkSuccessMessage", "Uspesne propojen ucet **%player%**!");
            messages.put("alreadyLinkedMessage", "Tento Discord ucet je jiz propojen s maximem uctu (%max%).");
            messages.put("minecraftToDiscordFormat", "**%player%** » %message%");
            messages.put("discordToMinecraftFormat", "&9[Discord] &f%user% &8» &7%message%");
            messages.put("statusMessageStarting", ":yellow_circle: **Server startuje...**");
            messages.put("statusMessageStarted", ":green_circle: **Server je online!**");
            messages.put("statusMessageStopping", ":orange_circle: **Server se vypíná...**");
            messages.put("statusMessageStopped", ":red_circle: **Server je offline.**");
            messages.put("channelTopicFormat", "Online: %online%/%max% | Uptime: %uptime% | Voidium Server");
            messages.put("uptimeFormat", "%days%d %hours%h %minutes%m");
        } else {
            // EN (default)
            messages.put("kickMessage", "&cYou are not whitelisted!\\n&7To join, you must verify on our Discord.\\n&7Your verification code: &b%code%");
            messages.put("linkSuccessMessage", "Successfully linked account **%player%**!");
            messages.put("alreadyLinkedMessage", "This Discord account is already linked to the maximum number of accounts (%max%).");
            messages.put("minecraftToDiscordFormat", "**%player%** » %message%");
            messages.put("discordToMinecraftFormat", "&9[Discord] &f%user% &8» &7%message%");
            messages.put("statusMessageStarting", ":yellow_circle: **Server is starting...**");
            messages.put("statusMessageStarted", ":green_circle: **Server is online!**");
            messages.put("statusMessageStopping", ":orange_circle: **Server is stopping...**");
            messages.put("statusMessageStopped", ":red_circle: **Server is offline.**");
            messages.put("channelTopicFormat", "Online: %online%/%max% | Uptime: %uptime% | Voidium Server");
            messages.put("uptimeFormat", "%days%d %hours%h %minutes%m");
        }
        
        return messages;
    }
    
    public static Map<String, Object> getAnnouncementMessages(String locale) {
        Map<String, Object> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("prefix", "&8[&bVoidium&8]&r ");
            messages.put("announcements", new String[]{
                "&bVitej na serveru!",
                "&eNezapomen hlasovat!",
                "&aPodivej se na nase pravidla!"
            });
        } else {
            // EN (default)
            messages.put("prefix", "&8[&bVoidium&8]&r ");
            messages.put("announcements", new String[]{
                "&bWelcome to the server!",
                "&eDon't forget to vote!",
                "&aCheck out our rules!"
            });
        }
        
        return messages;
    }
    
    public static Map<String, String> getRankMessages(String locale) {
        Map<String, String> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("promotionMessage", "&aGratulujeme {player}! Za nahranych {hours} hodin jsi ziskal rank &b{rank}&a!");
        } else {
            // EN (default)
            messages.put("promotionMessage", "&aCongratulations {player}! After playing {hours} hours, you received rank &b{rank}&a!");
        }
        
        return messages;
    }
    
    public static Map<String, String> getGeneralMessages(String locale) {
        Map<String, String> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("modPrefix", "&8[&bVoidium&8]&r ");
        } else {
            // EN (default)
            messages.put("modPrefix", "&8[&bVoidium&8]&r ");
        }
        
        return messages;
    }
    
    public static Map<String, String> getVoteMessages(String locale) {
        Map<String, String> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("announcementMessage", "&b%PLAYER% &7hlasoval pro server a získal odměnu!");
        } else {
            // EN (default)
            messages.put("announcementMessage", "&b%PLAYER% &7voted for the server and received a reward!");
        }
        
        return messages;
    }
}
