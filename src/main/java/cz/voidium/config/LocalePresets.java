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
            // Bot messages
            messages.put("invalidCodeMessage", "Neplatný nebo expirovaný kód.");
            messages.put("notLinkedMessage", "Nejsi propojen! Zadej platný kód ze hry.");
            messages.put("alreadyLinkedSingleMessage", ", jsi již propojen! UUID: `%uuid%`");
            messages.put("alreadyLinkedMultipleMessage", ", jsi již propojen k %count% účtům!");
            messages.put("unlinkSuccessMessage", "Všechny propojené účty byly úspěšně odpojeny.");
            messages.put("wrongGuildMessage", "Tento příkaz lze použít pouze na oficiálním Discord serveru.");
            messages.put("ticketCreatedMessage", "Ticket vytvořen!");
            messages.put("ticketClosingMessage", "Zavírám ticket...");
            messages.put("textChannelOnlyMessage", "Tento příkaz lze použít pouze v textovém kanálu.");
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
            // Bot messages
            messages.put("invalidCodeMessage", "Invalid or expired code.");
            messages.put("notLinkedMessage", "You are not linked! Enter a valid code from the game.");
            messages.put("alreadyLinkedSingleMessage", ", you are already linked! UUID: `%uuid%`");
            messages.put("alreadyLinkedMultipleMessage", ", you are already linked to %count% accounts!");
            messages.put("unlinkSuccessMessage", "All linked accounts have been successfully unlinked.");
            messages.put("wrongGuildMessage", "This command can only be used on the official Discord server.");
            messages.put("ticketCreatedMessage", "Ticket created!");
            messages.put("ticketClosingMessage", "Closing ticket...");
            messages.put("textChannelOnlyMessage", "This command can only be used in a text channel.");
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

    public static Map<String, String> getTicketMessages(String locale) {
        Map<String, String> messages = new HashMap<>();
        
        if ("cz".equalsIgnoreCase(locale)) {
            messages.put("ticketCreatedMessage", "Ticket vytvořen v %channel%!");
            messages.put("ticketWelcomeMessage", "Ahoj %user%,\nPodpora se ti bude brzy věnovat.\nDůvod: %reason%");
            messages.put("ticketCloseMessage", "Ticket uzavřen uživatelem %user%.");
            messages.put("noPermissionMessage", "Nemáš oprávnění k této akci.");
            messages.put("ticketLimitReachedMessage", "Dosáhl jsi maximálního počtu otevřených ticketů.");
            messages.put("ticketAlreadyClosedMessage", "Tento ticket je již uzavřen.");
        } else {
            // EN (default)
            messages.put("ticketCreatedMessage", "Ticket created in %channel%!");
            messages.put("ticketWelcomeMessage", "Hello %user%,\nSupport will be with you shortly.\nReason: %reason%");
            messages.put("ticketCloseMessage", "Ticket closed by %user%.");
            messages.put("noPermissionMessage", "You do not have permission to do this.");
            messages.put("ticketLimitReachedMessage", "You have reached the maximum number of open tickets.");
            messages.put("ticketAlreadyClosedMessage", "This ticket is already closed.");
        }
        
        return messages;
    }
}
