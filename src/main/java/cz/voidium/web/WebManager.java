package cz.voidium.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cz.voidium.config.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WebManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Web");
    private static WebManager instance;
    private HttpServer server;
    private MinecraftServer mcServer;
    private String authToken;
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, new VoidiumConfig.LocalTimeAdapter())
            .create();

    private static final Map<String, Map<String, String>> LANG = new HashMap<>();
    static {
        Map<String, String> en = new HashMap<>();
        en.put("title", "Voidium Control Panel");
        en.put("unauthorized", "Unauthorized. Please use the link from server console.");
        en.put("dashboard", "Dashboard");
        en.put("status", "Server Status");
        en.put("online", "Online");
        en.put("players", "Players");
        en.put("memory", "Memory");
        en.put("tps", "TPS");
        en.put("actions", "Quick Actions");
        en.put("restart", "Restart Server");
        en.put("announce", "Broadcast Announcement");
        en.put("send", "Send");
        en.put("kick", "Kick");
        en.put("ban", "Ban");
        en.put("kicked_by_admin", "Kicked by admin");
        en.put("confirm_action", "Are you sure?");
        en.put("message_placeholder", "Type your message here...");
        en.put("config", "Configuration");
        en.put("save", "Save Configuration");
        en.put("saved", "Saved!");
        en.put("saving", "Saving...");
        en.put("general", "General");
        en.put("restart", "Restart");
        en.put("announcements", "Announcements");
        en.put("discord", "Discord");
        en.put("stats", "Statistics");
        en.put("vote", "Voting");
        en.put("ranks", "Ranks");
        en.put("web", "Web");
        en.put("add", "Add");
        en.put("remove", "Remove");
        en.put("uptime", "Uptime");
        en.put("cpu", "CPU Usage");
        en.put("active_players", "Active Players");
        en.put("no_players", "No players online");
        en.put("refresh", "Refresh");
        en.put("player_list", "Online Players");
        en.put("server_info", "Server Information");
        en.put("max_memory", "Max Memory");
        en.put("used_memory", "Used Memory");
        en.put("free_memory", "Free Memory");
        
        // Descriptions and tooltips
        en.put("desc.server_info", "Real-time server statistics and resource usage");
        en.put("desc.actions", "Quick actions to manage your server");
        en.put("desc.player_list", "List of currently connected players with management options");
        en.put("desc.restart", "Restart the Minecraft server. All players will be disconnected.");
        en.put("desc.announce", "Send a broadcast message to all online players");
        en.put("desc.kick", "Disconnect this player from the server");
        en.put("desc.web.language", "Choose the interface language for the control panel");
        en.put("desc.restart.restartType", "FIXED_TIME: Restart at specific times | INTERVAL: Restart every X hours | DELAY: Restart after X minutes from start");
        en.put("desc.restart.fixedRestartTimes", "Set exact times when the server should restart (format: HH:MM)");
        en.put("desc.restart.intervalHours", "Number of hours between automatic restarts");
        en.put("desc.restart.delayMinutes", "Minutes to wait before the first restart after server start");
        en.put("desc.announcement.prefix", "Text prefix added before each announcement message");
        en.put("desc.announcement.announcementIntervalMinutes", "Time interval in minutes between automatic announcements");
        en.put("desc.announcement.announcements", "List of messages that will be broadcasted automatically in rotation");
        en.put("desc.ranks.enableAutoRanks", "Automatically promote players to higher ranks based on playtime");
        en.put("desc.ranks.checkIntervalMinutes", "How often to check if players should be promoted (in minutes)");
        en.put("desc.ranks.promotionMessage", "Message sent to player when they get promoted. Variables: {player} = player name, {rank} = rank value, {hours} = required hours");
        en.put("desc.ranks.ranks", "Define ranks with required playtime hours. PREFIX adds text before name, SUFFIX after name");
        en.put("desc.discord.kickMessage", "Message shown to non-whitelisted players. Use %code% variable for verification code");
        en.put("desc.discord.linkSuccessMessage", "Message sent in Discord when account is successfully linked. Use %player% for player name");
        en.put("desc.discord.alreadyLinkedMessage", "Message when Discord account has reached maximum linked accounts. Use %max% for limit");
        en.put("desc.discord.minecraftToDiscordFormat", "Format for messages from Minecraft to Discord. Variables: %player%, %message%");
        en.put("desc.discord.discordToMinecraftFormat", "Format for messages from Discord to Minecraft. Variables: %user%, %message%");
        en.put("desc.discord.chatWebhookUrl", "Webhook URL for sending Minecraft chat to Discord channel");
        en.put("desc.vote.announcementMessage", "Vote announcement message. Variables: %PLAYER%");
        en.put("desc.vote.commands", "List of commands executed when player votes. Use %PLAYER% for player name");
        
        // Config Fields
        en.put("locale_reset_title", "Message Language Reset");
        en.put("locale_reset_description", "Reset all player-facing messages (announcements, kick messages, promotions) to English or Czech. This does NOT change ports, IDs, or technical settings.");
        en.put("reset_to_english", "Reset Messages to English");
        en.put("reset_to_czech", "Reset Messages to Czech");
        en.put("locale_reset_success", "Messages successfully reset to {locale}!");
        en.put("locale_reset_confirm_en", "Are you sure you want to reset all messages to English? This will overwrite custom messages.");
        en.put("locale_reset_confirm_cz", "Are you sure you want to reset all messages to Czech? This will overwrite custom messages.");
        
        en.put("web.language", "Language");
        en.put("restart.restartType", "Restart Type");
        en.put("restart.fixedRestartTimes", "Fixed Restart Times");
        en.put("restart.intervalHours", "Interval (Hours)");
        en.put("restart.delayMinutes", "Delay (Minutes)");
        en.put("announcement.prefix", "Prefix");
        en.put("announcement.announcementIntervalMinutes", "Interval (Minutes)");
        en.put("announcement.announcements", "Messages");
        en.put("ranks.enableAutoRanks", "Enable Auto Ranks");
        en.put("ranks.checkIntervalMinutes", "Check Interval (Minutes)");
        en.put("ranks.promotionMessage", "Promotion Message");
        en.put("ranks.ranks", "Rank Definitions");
        en.put("ranks.type", "Type");
        en.put("ranks.value", "Value");
        en.put("ranks.hours", "Hours Needed");
        
        // General Config Fields
        en.put("general.enableMod", "Enable Mod");
        en.put("general.enableRestarts", "Enable Restarts");
        en.put("general.enableAnnouncements", "Enable Announcements");
        en.put("general.enableSkinRestorer", "Enable Skin Restorer");
        en.put("general.enableDiscord", "Enable Discord");
        en.put("general.enableWeb", "Enable Web Panel");
        en.put("general.enableStats", "Enable Statistics");
        en.put("general.enableRanks", "Enable Ranks");
        en.put("general.enableVote", "Enable Voting");
        en.put("general.skinCacheHours", "Skin Cache Duration (Hours)");
        en.put("general.modPrefix", "Mod Prefix");
        
        // Discord Config Fields
        en.put("discord.enableDiscord", "Enable Discord Integration");
        en.put("discord.botToken", "Bot Token");
        en.put("discord.guildId", "Guild ID");
        en.put("discord.enableWhitelist", "Enable Discord Whitelist");
        en.put("discord.kickMessage", "Kick Message (use %code% for verification code)");
        en.put("discord.linkSuccessMessage", "Link Success Message");
        en.put("discord.alreadyLinkedMessage", "Already Linked Message");
        en.put("discord.maxAccountsPerDiscord", "Max Accounts per Discord");
        en.put("discord.chatChannelId", "Chat Channel ID");
        en.put("discord.consoleChannelId", "Console Channel ID");
        en.put("discord.linkChannelId", "Link Channel ID");
        en.put("discord.linkedRoleId", "Linked Role ID");
        en.put("discord.syncBansDiscordToMc", "Sync Bans: Discord → Minecraft");
        en.put("discord.syncBansMcToDiscord", "Sync Bans: Minecraft → Discord");
        en.put("discord.enableChatBridge", "Enable Chat Bridge");
        en.put("discord.minecraftToDiscordFormat", "Minecraft → Discord Format");
        en.put("discord.discordToMinecraftFormat", "Discord → Minecraft Format");
        en.put("discord.translateEmojis", "Translate Emojis");
        en.put("discord.chatWebhookUrl", "Chat Webhook URL");
        
        // Stats Config Fields
        en.put("stats.enableStats", "Enable Statistics");
        en.put("stats.reportChannelId", "Report Channel ID");
        en.put("stats.reportTime", "Report Time (HH:MM)");
        
        // Vote Config Fields
        en.put("vote.enabled", "Enable Voting System");
        en.put("vote.host", "Host Address");
        en.put("vote.port", "Port");
        en.put("vote.rsaPrivateKeyPath", "RSA Private Key Path");
        en.put("vote.rsaPublicKeyPath", "RSA Public Key Path");
        en.put("vote.sharedSecret", "Shared Secret");
        en.put("vote.announceVotes", "Announce Votes");
        en.put("vote.announcementMessage", "Announcement Message");
        en.put("vote.announcementCooldown", "Announcement Cooldown (seconds)");
        en.put("vote.commands", "Commands to Execute on Vote");
        
        LANG.put("en", en);

        Map<String, String> cz = new HashMap<>();
        cz.put("title", "Ovládací panel Voidium");
        cz.put("unauthorized", "Neautorizováno. Použijte prosím odkaz z konzole serveru.");
        cz.put("dashboard", "Nástěnka");
        cz.put("status", "Stav serveru");
        cz.put("online", "Online");
        cz.put("players", "Hráči");
        cz.put("memory", "Paměť");
        cz.put("tps", "TPS");
        cz.put("actions", "Rychlé akce");
        cz.put("restart", "Restartovat server");
        cz.put("announce", "Odeslat oznámení");
        cz.put("send", "Odeslat");
        cz.put("kick", "Vyhodit");
        cz.put("ban", "Zabanovat");
        cz.put("kicked_by_admin", "Vyhozen administrátorem");
        cz.put("confirm_action", "Jste si jistí?");
        cz.put("message_placeholder", "Napište zprávu zde...");
        cz.put("config", "Konfigurace");
        cz.put("save", "Uložit konfiguraci");
        cz.put("saved", "Uloženo!");
        cz.put("saving", "Ukládám...");
        cz.put("general", "Obecné");
        cz.put("restart", "Restart");
        cz.put("announcements", "Oznámení");
        cz.put("discord", "Discord");
        cz.put("stats", "Statistiky");
        cz.put("vote", "Hlasování");
        cz.put("ranks", "Ranky");
        cz.put("web", "Web");
        cz.put("add", "Přidat");
        cz.put("remove", "Odebrat");
        cz.put("uptime", "Doba běhu");
        cz.put("cpu", "Vytížení CPU");
        cz.put("active_players", "Aktivní hráči");
        cz.put("no_players", "Žádní hráči online");
        cz.put("refresh", "Obnovit");
        cz.put("player_list", "Online hráči");
        cz.put("server_info", "Informace o serveru");
        cz.put("max_memory", "Max. paměť");
        cz.put("used_memory", "Použitá paměť");
        cz.put("free_memory", "Volná paměť");

        // Descriptions and tooltips
        cz.put("desc.server_info", "Statistiky serveru a využití zdrojů v reálném čase");
        cz.put("desc.actions", "Rychlé akce pro správu serveru");
        cz.put("desc.player_list", "Seznam připojených hráčů s možnostmi správy");
        cz.put("desc.restart", "Restartuje Minecraft server. Všichni hráči budou odpojeni.");
        cz.put("desc.announce", "Odešle zprávu všem online hráčům");
        cz.put("desc.kick", "Odpojí tohoto hráče ze serveru");
        cz.put("desc.web.language", "Vyberte jazyk rozhraní ovládacího panelu");
        cz.put("desc.restart.restartType", "FIXED_TIME: Restart v konkrétní časy | INTERVAL: Restart každých X hodin | DELAY: Restart po X minutách od startu");
        cz.put("desc.restart.fixedRestartTimes", "Nastavte přesné časy, kdy se má server restartovat (formát: HH:MM)");
        cz.put("desc.restart.intervalHours", "Počet hodin mezi automatickými restarty");
        cz.put("desc.restart.delayMinutes", "Minuty čekání před prvním restartem po spuštění serveru");
        cz.put("desc.announcement.prefix", "Textový prefix přidaný před každou zprávu oznámení");
        cz.put("desc.announcement.announcementIntervalMinutes", "Časový interval v minutách mezi automatickými oznámeními");
        cz.put("desc.announcement.announcements", "Seznam zpráv, které budou automaticky vysílány v rotaci");
        cz.put("desc.ranks.enableAutoRanks", "Automaticky povyšovat hráče na vyšší ranky podle odehraného času");
        cz.put("desc.ranks.checkIntervalMinutes", "Jak často kontrolovat, zda mají být hráči povýšeni (v minutách)");
        cz.put("desc.ranks.promotionMessage", "Zpráva odeslaná hráči při povýšení. Proměnné: {player} = jméno hráče, {rank} = hodnota ranku, {hours} = požadované hodiny");
        cz.put("desc.ranks.ranks", "Definujte ranky s požadovanými hodinami hraní. PREFIX přidá text před jméno, SUFFIX za jméno");
        cz.put("desc.discord.kickMessage", "Zpráva zobrazená hráčům mimo whitelist. Použijte %code% pro ověřovací kód");
        cz.put("desc.discord.linkSuccessMessage", "Zpráva odeslaná na Discord při úspěšném propojení účtu. Použijte %player% pro jméno hráče");
        cz.put("desc.discord.alreadyLinkedMessage", "Zpráva, když Discord účet dosáhl maxima propojených účtů. Použijte %max% pro limit");
        cz.put("desc.discord.minecraftToDiscordFormat", "Formát zpráv z Minecraftu na Discord. Proměnné: %player%, %message%");
        cz.put("desc.discord.discordToMinecraftFormat", "Formát zpráv z Discordu do Minecraftu. Proměnné: %user%, %message%");
        cz.put("desc.discord.uptimeFormat", "Formát zobrazení uptime. Proměnné: %days%, %hours%, %minutes%, %seconds%");
        cz.put("desc.discord.chatWebhookUrl", "Webhook URL pro odesílání Minecraft chatu na Discord kanál");
        cz.put("desc.vote.announcementMessage", "Zpráva oznámení o hlasování. Proměnné: %PLAYER%");
        cz.put("desc.vote.commands", "Seznam příkazů provedených při hlasování hráče. Použijte %PLAYER% pro jméno hráče");

        // Config Fields
        cz.put("locale_reset_title", "Reset jazyka zpráv");
        cz.put("locale_reset_description", "Resetuje všechny zprávy pro hráče (oznámení, kick zprávy, povýšení) do angličtiny nebo češtiny. Toto NEZMĚNÍ porty, ID nebo technická nastavení.");
        cz.put("reset_to_english", "Resetovat zprávy do angličtiny");
        cz.put("reset_to_czech", "Resetovat zprávy do češtiny");
        cz.put("locale_reset_success", "Zprávy úspěšně resetovány do {locale}!");
        cz.put("locale_reset_confirm_en", "Opravdu chcete resetovat všechny zprávy do angličtiny? Tím přepíšete vlastní zprávy.");
        cz.put("locale_reset_confirm_cz", "Opravdu chcete resetovat všechny zprávy do češtiny? Tím přepíšete vlastní zprávy.");
        
        cz.put("web.language", "Jazyk");
        cz.put("restart.restartType", "Typ restartu");
        cz.put("restart.fixedRestartTimes", "Pevné časy restartu");
        cz.put("restart.intervalHours", "Interval (hodiny)");
        cz.put("restart.delayMinutes", "Zpoždění (minuty)");
        cz.put("announcement.prefix", "Prefix");
        cz.put("announcement.announcementIntervalMinutes", "Interval (minuty)");
        cz.put("announcement.announcements", "Zprávy");
        cz.put("ranks.enableAutoRanks", "Zapnout auto ranky");
        cz.put("ranks.checkIntervalMinutes", "Interval kontroly (minuty)");
        cz.put("ranks.promotionMessage", "Zpráva o povýšení");
        cz.put("ranks.ranks", "Definice ranků");
        cz.put("ranks.type", "Typ");
        cz.put("ranks.value", "Hodnota");
        cz.put("ranks.hours", "Potřebné hodiny");
        
        // General Config Fields
        cz.put("general.enableMod", "Zapnout mod");
        cz.put("general.enableRestarts", "Zapnout restarty");
        cz.put("general.enableAnnouncements", "Zapnout oznámení");
        cz.put("general.enableSkinRestorer", "Zapnout Skin Restorer");
        cz.put("general.enableDiscord", "Zapnout Discord");
        cz.put("general.enableWeb", "Zapnout webový panel");
        cz.put("general.enableStats", "Zapnout statistiky");
        cz.put("general.enableRanks", "Zapnout ranky");
        cz.put("general.enableVote", "Zapnout hlasování");
        cz.put("general.skinCacheHours", "Doba cachování skinů (hodiny)");
        cz.put("general.modPrefix", "Prefix modu");
        
        // Discord Config Fields
        cz.put("discord.enableDiscord", "Zapnout Discord integraci");
        cz.put("discord.botToken", "Token bota");
        cz.put("discord.guildId", "ID serveru");
        cz.put("discord.enableWhitelist", "Zapnout Discord whitelist");
        cz.put("discord.kickMessage", "Zpráva při vyhození (použijte %code% pro ověřovací kód)");
        cz.put("discord.linkSuccessMessage", "Zpráva při úspěšném propojení");
        cz.put("discord.alreadyLinkedMessage", "Zpráva při již propojeném účtu");
        cz.put("discord.maxAccountsPerDiscord", "Max. účtů na Discord");
        cz.put("discord.chatChannelId", "ID chat kanálu");
        cz.put("discord.consoleChannelId", "ID konzole kanálu");
        cz.put("discord.linkChannelId", "ID kanálu pro propojení");
        cz.put("discord.linkedRoleId", "ID role pro propojené");
        cz.put("discord.syncBansDiscordToMc", "Synchronizovat bany: Discord → Minecraft");
        cz.put("discord.syncBansMcToDiscord", "Synchronizovat bany: Minecraft → Discord");
        cz.put("discord.enableChatBridge", "Zapnout chat most");
        cz.put("discord.minecraftToDiscordFormat", "Formát Minecraft → Discord");
        cz.put("discord.discordToMinecraftFormat", "Formát Discord → Minecraft");
        cz.put("discord.translateEmojis", "Překládat emoji");
        cz.put("discord.chatWebhookUrl", "URL chat webhooku");
        cz.put("discord.enableConsoleLog", "Zapnout logování konzole");
        cz.put("discord.enableStatusMessages", "Zapnout status zprávy");
        cz.put("discord.statusMessageStarting", "Zpráva: Server startuje");
        cz.put("discord.statusMessageStarted", "Zpráva: Server online");
        cz.put("discord.statusMessageStopping", "Zpráva: Server se vypíná");
        cz.put("discord.statusMessageStopped", "Zpráva: Server offline");
        cz.put("discord.enableTopicUpdate", "Aktualizovat popis kanálu (Topic)");
        cz.put("discord.channelTopicFormat", "Formát popisu kanálu");
        cz.put("discord.uptimeFormat", "Formát uptime");
        cz.put("discord.statusChannelId", "ID status kanálu (nepovinné)");
        
        // Stats Config Fields
        cz.put("stats.enableStats", "Zapnout statistiky");
        cz.put("stats.reportChannelId", "ID kanálu pro reporty");
        cz.put("stats.reportTime", "Čas reportu (HH:MM)");
        
        // Vote Config Fields
        cz.put("vote.enabled", "Zapnout hlasovací systém");
        cz.put("vote.host", "Adresa hostitele");
        cz.put("vote.port", "Port");
        cz.put("vote.rsaPrivateKeyPath", "Cesta k RSA privátnímu klíči");
        cz.put("vote.rsaPublicKeyPath", "Cesta k RSA veřejnému klíči");
        cz.put("vote.sharedSecret", "Sdílené tajemství");
        cz.put("vote.announceVotes", "Oznamovat hlasy");
        cz.put("vote.announcementMessage", "Zpráva oznámení");
        cz.put("vote.announcementCooldown", "Cooldown oznámení (sekundy)");
        cz.put("vote.commands", "Příkazy vykonané při hlasování");

        LANG.put("cz", cz);
    }

    public WebManager() {
        instance = this;
    }

    public static WebManager getInstance() {
        if (instance == null) {
            instance = new WebManager();
        }
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.mcServer = server;
    }

    public void start() {
        try {
            WebConfig config = WebConfig.getInstance();
            authToken = UUID.randomUUID().toString();
            
            server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
            
            server.createContext("/", new DashboardHandler());
            server.createContext("/api/action", new ActionHandler());
            server.createContext("/api/config", new ConfigApiHandler());
            server.createContext("/api/locale", new LocaleResetHandler());
            server.createContext("/css/style.css", new StyleHandler());
            
            server.setExecutor(null);
            server.start();
            LOGGER.info("Web Control Panel started on port {}", config.getPort());
            LOGGER.info("Access URL: {}", getWebUrl());
        } catch (IOException e) {
            LOGGER.error("Failed to start Web Control Panel", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("Web Control Panel stopped");
        }
    }

    public String getWebUrl() {
        WebConfig config = WebConfig.getInstance();
        String hostname = config.getPublicHostname();
        
        // Try to get IP from server.properties first
        if (("localhost".equals(hostname) || "127.0.0.1".equals(hostname)) && mcServer != null) {
            try {
                String serverIp = mcServer.getLocalIp();
                if (serverIp != null && !serverIp.isEmpty() && !"0.0.0.0".equals(serverIp)) {
                    hostname = serverIp;
                } else {
                    // Fallback to network interface detection
                    java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                    boolean found = false;
                    while (interfaces.hasMoreElements()) {
                        java.net.NetworkInterface iface = interfaces.nextElement();
                        if (iface.isLoopback() || !iface.isUp()) continue;

                        java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            java.net.InetAddress addr = addresses.nextElement();
                            if (addr instanceof java.net.Inet4Address) {
                                hostname = addr.getHostAddress();
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    
                    if (!found) {
                        hostname = java.net.InetAddress.getLocalHost().getHostAddress();
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to detect server IP, using configured hostname", e);
            }
        }
        
        // Handle IPv6 format in URL just in case
        if (hostname.contains(":") && !hostname.startsWith("[")) {
            hostname = "[" + hostname + "]";
        }
        
        return "http://" + hostname + ":" + config.getPort() + "/?token=" + authToken;
    }

    private String getTranslation(String key) {
        String langCode = WebConfig.getInstance().getLanguage();
        return LANG.getOrDefault(langCode, LANG.get("en")).getOrDefault(key, key);
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("token=" + authToken)) {
            exchange.getResponseHeaders().set("Set-Cookie", "session=" + authToken + "; Path=/; HttpOnly");
            return true;
        }
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        return cookie != null && cookie.contains("session=" + authToken) && authToken != null;
    }

    private void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                String html = "<html><body style='background:#1a1a1a;color:#fff;font-family:sans-serif;text-align:center;padding-top:50px;'>" +
                              "<h1>" + getTranslation("unauthorized") + "</h1></body></html>";
                sendResponse(exchange, html, 401);
                return;
            }
            
            String html = getFullHtml();
            sendResponse(exchange, html, 200);
        }
    }

    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseParams(body);
                String action = params.get("action");
                
                if ("restart".equals(action)) {
                    mcServer.execute(() -> mcServer.halt(false));
                } else if ("announce".equals(action)) {
                    String msg = params.get("message");
                    if (msg != null && !msg.isEmpty()) {
                        mcServer.execute(() -> mcServer.getPlayerList().broadcastSystemMessage(
                            Component.literal("§8[§bVoidium§8] §f" + msg.replace("&", "§")), false
                        ));
                    }
                } else if ("kick".equals(action)) {
                    String player = params.get("player");
                    ServerPlayer sp = mcServer.getPlayerList().getPlayerByName(player);
                    if (sp != null) {
                        mcServer.execute(() -> sp.connection.disconnect(Component.literal(getTranslation("kicked_by_admin"))));
                    }
                }
                
                redirect(exchange, "/");
            }
        }
    }
    
    private class StyleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String css = "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                         "body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); color: #e0e0e0; margin: 0; padding: 0; min-height: 100vh; overflow-x: hidden; }" +
                         ".container { max-width: 1400px; margin: 0 auto; padding: 20px; }" +
                         "header { background: rgba(0,0,0,0.5); backdrop-filter: blur(20px); padding: 25px 30px; border-bottom: 2px solid rgba(138, 43, 226, 0.3); display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; border-radius: 0 0 20px 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.4); }" +
                         "h1 { margin: 0; color: #bb86fc; font-weight: 600; font-size: 2em; text-shadow: 0 0 20px rgba(187, 134, 252, 0.5); letter-spacing: -0.5px; }" +
                         ".status-badge { background: linear-gradient(135deg, #00d4aa, #00e676); color: #000; padding: 8px 20px; border-radius: 30px; font-size: 0.9em; font-weight: 700; box-shadow: 0 4px 20px rgba(0, 212, 170, 0.4); animation: pulse-glow 2s ease-in-out infinite; }" +
                         "@keyframes pulse-glow { 0%, 100% { box-shadow: 0 4px 20px rgba(0, 212, 170, 0.4); } 50% { box-shadow: 0 4px 30px rgba(0, 212, 170, 0.7); } }" +
                         ".card { background: linear-gradient(145deg, rgba(30, 30, 50, 0.8), rgba(20, 20, 35, 0.9)); backdrop-filter: blur(15px); border: 1px solid rgba(187, 134, 252, 0.1); border-radius: 20px; padding: 30px; margin-bottom: 25px; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05); transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); position: relative; overflow: hidden; }" +
                         ".card::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(187, 134, 252, 0.1), transparent); transition: left 0.5s; }" +
                         ".card:hover::before { left: 100%; }" +
                         ".card:hover { transform: translateY(-5px); border-color: rgba(187, 134, 252, 0.3); box-shadow: 0 15px 50px rgba(187, 134, 252, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1); }" +
                         ".card h2 { margin: 0 0 20px 0; color: #bb86fc; border-bottom: 2px solid rgba(187, 134, 252, 0.2); padding-bottom: 15px; font-size: 1.5em; font-weight: 600; display: flex; align-items: center; gap: 10px; }" +
                         ".card h2::before { content: '●'; font-size: 0.6em; color: #00d4aa; animation: blink 2s ease-in-out infinite; }" +
                         "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }" +
                         ".stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px; }" +
                         ".stat-item { background: rgba(0, 0, 0, 0.3); padding: 20px; border-radius: 15px; border: 1px solid rgba(255, 255, 255, 0.05); transition: all 0.3s; }" +
                         ".stat-item:hover { background: rgba(187, 134, 252, 0.1); border-color: rgba(187, 134, 252, 0.3); transform: scale(1.05); }" +
                         ".stat-label { font-size: 0.85em; color: #aaa; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }" +
                         ".stat-value { font-size: 2em; font-weight: 700; color: #fff; text-shadow: 0 2px 10px rgba(187, 134, 252, 0.3); }" +
                         ".stat-value.highlight { color: #00d4aa; text-shadow: 0 2px 10px rgba(0, 212, 170, 0.5); }" +
                         "input[type='text'], input[type='number'], input[type='password'], select, input[type='time'], textarea { width: 100%; padding: 14px; margin: 8px 0 20px; background: rgba(0, 0, 0, 0.4); border: 2px solid rgba(187, 134, 252, 0.2); color: #fff; border-radius: 12px; box-sizing: border-box; transition: all 0.3s; font-family: inherit; font-size: 1em; }" +
                         "input:focus, select:focus, textarea:focus { outline: none; border-color: #bb86fc; background: rgba(187, 134, 252, 0.05); box-shadow: 0 0 20px rgba(187, 134, 252, 0.2); }" +
                         "textarea { min-height: 100px; resize: vertical; }" +
                         "button { background: linear-gradient(135deg, #6200ea, #b388ff); color: #fff; border: none; padding: 14px 30px; border-radius: 12px; cursor: pointer; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px; transition: all 0.3s; box-shadow: 0 5px 20px rgba(98, 0, 234, 0.4); font-size: 0.9em; font-family: inherit; }" +
                         "button:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 8px 30px rgba(98, 0, 234, 0.6); }" +
                         "button:active:not(:disabled) { transform: translateY(-1px); }" +
                         "button:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }" +
                         "button.danger { background: linear-gradient(135deg, #d32f2f, #f44336); box-shadow: 0 5px 20px rgba(211, 47, 47, 0.4); }" +
                         "button.danger:hover:not(:disabled) { box-shadow: 0 8px 30px rgba(211, 47, 47, 0.6); }" +
                         "button.success { background: linear-gradient(135deg, #00b09b, #96c93d); box-shadow: 0 5px 20px rgba(0, 176, 155, 0.4); }" +
                         "button.success:hover:not(:disabled) { box-shadow: 0 8px 30px rgba(0, 176, 155, 0.6); }" +
                         ".success-anim { animation: success-pulse 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275); }" +
                         "@keyframes success-pulse { 0% { transform: scale(1); } 50% { transform: scale(1.1); } 100% { transform: scale(1); } }" +
                         ".tabs { display: flex; gap: 10px; margin-bottom: 30px; background: rgba(0,0,0,0.3); padding: 8px; border-radius: 15px; border: 1px solid rgba(187, 134, 252, 0.1); }" +
                         ".tab { padding: 14px 30px; cursor: pointer; color: #aaa; transition: all 0.3s; border-radius: 10px; flex: 1; text-align: center; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9em; }" +
                         ".tab:hover:not(.active) { color: #fff; background: rgba(255,255,255,0.05); }" +
                         ".tab.active { color: #fff; background: linear-gradient(135deg, #6200ea, #b388ff); box-shadow: 0 5px 20px rgba(98, 0, 234, 0.4); }" +
                         ".tab-content { display: none; animation: fadeInUp 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275); }" +
                         ".tab-content.active { display: block; }" +
                         "@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }" +
                         "table { width: 100%; border-collapse: separate; border-spacing: 0 12px; }" +
                         "thead { position: sticky; top: 0; z-index: 10; }" +
                         "th, td { text-align: left; padding: 18px; }" +
                         "tbody tr { background: rgba(187, 134, 252, 0.05); transition: all 0.3s; border-radius: 12px; }" +
                         "tbody tr:hover { background: rgba(187, 134, 252, 0.15); transform: scale(1.02); box-shadow: 0 5px 20px rgba(187, 134, 252, 0.2); }" +
                         "td:first-child { border-radius: 12px 0 0 12px; }" +
                         "td:last-child { border-radius: 0 12px 12px 0; }" +
                         "th { color: #bb86fc; font-weight: 700; text-transform: uppercase; font-size: 0.85em; letter-spacing: 1.5px; background: rgba(0,0,0,0.3); padding: 15px 18px; }" +
                         "thead tr th:first-child { border-radius: 12px 0 0 12px; }" +
                         "thead tr th:last-child { border-radius: 0 12px 12px 0; }" +
                         ".form-group { margin-bottom: 25px; }" +
                         ".form-group label { display: block; margin-bottom: 10px; color: #e0e0e0; font-weight: 600; font-size: 0.95em; }" +
                         ".switch { position: relative; display: inline-block; width: 60px; height: 30px; float: right; }" +
                         ".switch input { opacity: 0; width: 0; height: 0; }" +
                         ".slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(255,255,255,0.1); transition: .4s; border-radius: 30px; border: 2px solid rgba(255,255,255,0.2); }" +
                         ".slider:before { position: absolute; content: ''; height: 22px; width: 22px; left: 3px; bottom: 2px; background: linear-gradient(135deg, #fff, #f0f0f0); transition: .4s; border-radius: 50%; box-shadow: 0 2px 5px rgba(0,0,0,0.3); }" +
                         "input:checked + .slider { background: linear-gradient(135deg, #bb86fc, #9c6fd9); border-color: #bb86fc; }" +
                         "input:checked + .slider:before { transform: translateX(28px); }" +
                         ".array-item { display: flex; gap: 10px; margin-bottom: 10px; align-items: center; }" +
                         ".array-item input { margin: 0 !important; flex: 1; }" +
                         ".array-item button { padding: 10px 15px; margin: 0; }" +
                         ".no-players { text-align: center; padding: 40px; color: #aaa; font-style: italic; }" +
                         "[title] { position: relative; cursor: help; }" +
                         "[title]:hover::after { content: attr(title); position: absolute; bottom: 100%; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,0.95); color: #fff; padding: 8px 12px; border-radius: 8px; white-space: nowrap; font-size: 0.85em; z-index: 1000; margin-bottom: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.5); border: 1px solid rgba(187,134,252,0.3); max-width: 300px; white-space: normal; }" +
                         "[title]:hover::before { content: ''; position: absolute; bottom: 100%; left: 50%; transform: translateX(-50%); border: 6px solid transparent; border-top-color: rgba(0,0,0,0.95); z-index: 1001; }" +
                         "@media (max-width: 768px) { .stat-grid { grid-template-columns: 1fr; } .tabs { flex-direction: column; } h1 { font-size: 1.5em; } [title]:hover::after { left: 10px; transform: none; max-width: calc(100vw - 40px); } }";
            exchange.getResponseHeaders().set("Content-Type", "text/css");
            sendResponse(exchange, css, 200);
        }
    }

    private class ConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();
                    java.util.List<String> changedFiles = new java.util.ArrayList<>();
                    
                    // Update Web Config
                    if (json.has("web")) {
                        JsonObject webJson = json.getAsJsonObject("web");
                        if (webJson.has("language")) {
                            Field f = WebConfig.class.getDeclaredField("language");
                            f.setAccessible(true);
                            f.set(WebConfig.getInstance(), webJson.get("language").getAsString());
                            WebConfig.getInstance().save();
                            // Force flush to ensure file is written before page reload
                            Thread.sleep(100);
                            changedFiles.add("WebConfig");
                        }
                    }

                    // Update Configs
                    if (updateConfig(GeneralConfig.class, GeneralConfig.getInstance(), json.get("general"))) changedFiles.add("GeneralConfig");
                    if (updateConfig(RestartConfig.class, RestartConfig.getInstance(), json.get("restart"))) changedFiles.add("RestartConfig");
                    if (updateConfig(AnnouncementConfig.class, AnnouncementConfig.getInstance(), json.get("announcement"))) changedFiles.add("AnnouncementConfig");
                    if (updateConfig(DiscordConfig.class, DiscordConfig.getInstance(), json.get("discord"))) changedFiles.add("DiscordConfig");
                    if (updateConfig(StatsConfig.class, StatsConfig.getInstance(), json.get("stats"))) changedFiles.add("StatsConfig");
                    if (updateConfig(VoteConfig.class, VoteConfig.getInstance(), json.get("vote"))) changedFiles.add("VoteConfig");
                    if (updateConfig(RanksConfig.class, RanksConfig.getInstance(), json.get("ranks"))) changedFiles.add("RanksConfig");

                    if (!changedFiles.isEmpty()) {
                        LOGGER.info("Web Control Panel: Configuration saved. Updated files: {}", String.join(", ", changedFiles));
                    }

                    sendResponse(exchange, "{\"status\":\"ok\"}", 200);
                } catch (Exception e) {
                    LOGGER.error("Failed to update config", e);
                    sendResponse(exchange, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}", 500);
                }
            }
        }
        
        private <T> boolean updateConfig(Class<T> clazz, T instance, JsonElement json) {
            if (json == null) return false;
            try {
                T temp = GSON.fromJson(json, clazz);
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
                    field.setAccessible(true);
                    field.set(instance, field.get(temp));
                }
                clazz.getMethod("save").invoke(instance);
                return true;
            } catch (Exception e) {
                LOGGER.error("Error updating config " + clazz.getSimpleName(), e);
                return false;
            }
        }
    }
    
    private class LocaleResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "{\"status\":\"error\",\"message\":\"Unauthorized\"}", 401);
                return;
            }
            
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseParams(body);
                    String locale = params.get("locale");
                    
                    if (locale == null || (!locale.equals("en") && !locale.equals("cz"))) {
                        sendResponse(exchange, "{\"status\":\"error\",\"message\":\"Invalid locale\"}", 400);
                        return;
                    }
                    
                    // Apply locale to all configs
                    GeneralConfig.getInstance().applyLocale(locale);
                    AnnouncementConfig.getInstance().applyLocale(locale);
                    DiscordConfig.getInstance().applyLocale(locale);
                    RanksConfig.getInstance().applyLocale(locale);
                    VoteConfig.getInstance().applyLocale(locale);
                    
                    LOGGER.info("Web Control Panel: Reset config messages to {} locale", locale);
                    sendResponse(exchange, "{\"status\":\"ok\",\"message\":\"Messages reset to " + locale.toUpperCase() + "\"}", 200);
                } catch (Exception e) {
                    LOGGER.error("Failed to reset locale", e);
                    sendResponse(exchange, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}", 500);
                }
            }
        }
    }

    private Map<String, String> parseParams(String body) {
        Map<String, String> params = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private String getFullHtml() {
        Map<String, Object> allConfigs = new HashMap<>();
        allConfigs.put("web", WebConfig.getInstance()); // Need to make sure WebConfig serializes nicely or create map
        Map<String, Object> webMap = new HashMap<>();
        webMap.put("language", WebConfig.getInstance().getLanguage());
        allConfigs.put("web", webMap);
        
        allConfigs.put("general", GeneralConfig.getInstance());
        allConfigs.put("restart", RestartConfig.getInstance());
        allConfigs.put("announcement", AnnouncementConfig.getInstance());
        allConfigs.put("discord", DiscordConfig.getInstance());
        allConfigs.put("stats", StatsConfig.getInstance());
        allConfigs.put("vote", VoteConfig.getInstance());
        allConfigs.put("ranks", RanksConfig.getInstance());
        
        String configJson = GSON.toJson(allConfigs);
        
        // Calculate memory statistics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>").append(getTranslation("title")).append("</title>");
        sb.append("<link rel='stylesheet' href='/css/style.css'>");
        sb.append("<link rel='preconnect' href='https://fonts.googleapis.com'>");
        sb.append("<link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap' rel='stylesheet'>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("</head><body>");
        
        sb.append("<header>");
        sb.append("<h1>⚡ Voidium Control Panel</h1>");
        sb.append("<span class='status-badge'>● ").append(getTranslation("online")).append("</span>");
        sb.append("</header>");
        
        sb.append("<div class='container'>");
        
        // Tabs
        sb.append("<div class='tabs'>");
        sb.append("<div class='tab active' onclick='switchTab(\"dashboard\")'>📊 ").append(getTranslation("dashboard")).append("</div>");
        sb.append("<div class='tab' onclick='switchTab(\"config\")'>⚙️ ").append(getTranslation("config")).append("</div>");
        sb.append("</div>");
        
        // Dashboard Content
        sb.append("<div id='dashboard' class='tab-content active'>");
        
        // Server Statistics Grid
        sb.append("<div class='card'>");
        sb.append("<h2>").append(getTranslation("server_info")).append("</h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(getTranslation("desc.server_info")).append("</p>");
        sb.append("<div class='stat-grid'>");
        
        // Players stat
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(getTranslation("active_players")).append("</div>");
        sb.append("<div class='stat-value highlight'>").append(mcServer.getPlayerCount()).append("<span style='font-size:0.5em;color:#aaa;'>/").append(mcServer.getMaxPlayers()).append("</span></div>");
        sb.append("</div>");
        
        // Memory stat
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(getTranslation("used_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(usedMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("<div style='background:rgba(0,0,0,0.3);height:8px;border-radius:10px;margin-top:10px;overflow:hidden;'>");
        sb.append("<div style='width:").append(memoryPercent).append("%;height:100%;background:linear-gradient(90deg,#00d4aa,#bb86fc);border-radius:10px;transition:width 0.5s;'></div>");
        sb.append("</div>");
        sb.append("</div>");
        
        // Max Memory stat
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(getTranslation("max_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(maxMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("</div>");
        
        // Free Memory stat
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(getTranslation("free_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(maxMemory - usedMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("</div>");
        
        sb.append("</div></div>");
        
        // Actions
        sb.append("<div class='card'>");
        sb.append("<h2>").append(getTranslation("actions")).append("</h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(getTranslation("desc.actions")).append("</p>");
        sb.append("<div style='display:flex;gap:15px;flex-wrap:wrap;margin-bottom:20px;'>");
        sb.append("<form method='POST' action='/api/action' style='flex:1;min-width:200px;'>");
        sb.append("<input type='hidden' name='action' value='restart'>");
        sb.append("<button type='submit' class='danger' style='width:100%;' title='").append(getTranslation("desc.restart")).append("' onclick='return confirm(\"").append(getTranslation("confirm_action")).append("\")'>🔄 ").append(getTranslation("restart")).append("</button>");
        sb.append("</form>");
        sb.append("<button onclick='location.reload()' style='flex:1;min-width:200px;' title='Reload page to update statistics'>🔃 ").append(getTranslation("refresh")).append("</button>");
        sb.append("</div>");
        
        sb.append("<form method='POST' action='/api/action' style='display:flex; gap:15px;'>");
        sb.append("<input type='hidden' name='action' value='announce'>");
        sb.append("<input type='text' name='message' placeholder='").append(getTranslation("message_placeholder")).append("' style='margin:0;flex:1;' title='").append(getTranslation("desc.announce")).append("'>");
        sb.append("<button type='submit' style='white-space:nowrap;' title='").append(getTranslation("desc.announce")).append("'>📢 ").append(getTranslation("send")).append("</button>");
        sb.append("</form>");
        sb.append("</div>");
        
        // Players
        sb.append("<div class='card'>");
        sb.append("<h2>").append(getTranslation("player_list")).append(" <span style='font-size:0.7em;color:#aaa;'>(").append(mcServer.getPlayerCount()).append(")</span></h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(getTranslation("desc.player_list")).append("</p>");
        
        if (mcServer.getPlayerCount() == 0) {
            sb.append("<div class='no-players'>").append(getTranslation("no_players")).append("</div>");
        } else {
            sb.append("<table><thead><tr><th>👤 Name</th><th>🆔 UUID</th><th>⚡ Action</th></tr></thead><tbody>");
            for (ServerPlayer player : mcServer.getPlayerList().getPlayers()) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(player.getGameProfile().getName()).append("</strong></td>");
                sb.append("<td style='font-family:monospace;font-size:0.85em;color:#aaa;'>").append(player.getUUID()).append("</td>");
                sb.append("<td>");
                sb.append("<form method='POST' action='/api/action' style='display:inline;'>");
                sb.append("<input type='hidden' name='action' value='kick'>");
                sb.append("<input type='hidden' name='player' value='").append(player.getGameProfile().getName()).append("'>");
                sb.append("<button type='submit' class='danger' style='padding: 8px 16px;font-size:0.85em;' title='").append(getTranslation("desc.kick")).append("' onclick='return confirm(\"").append(getTranslation("kick")).append(" ").append(player.getGameProfile().getName()).append("?\")'>❌ ").append(getTranslation("kick")).append("</button>");
                sb.append("</form>");
                sb.append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }
        sb.append("</div>");
        sb.append("</div>"); // End Dashboard
        
        // Config Content
        sb.append("<div id='config' class='tab-content'>");
        sb.append("<div style='background: rgba(255,255,255,0.05); padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px solid rgba(147, 51, 234, 0.3);'>");
        sb.append("<h3 style='margin: 0 0 10px 0; color: #c084fc;'>🌐 ").append(getTranslation("locale_reset_title")).append("</h3>");
        sb.append("<p style='margin: 0 0 15px 0; opacity: 0.8; font-size: 0.9em;'>").append(getTranslation("locale_reset_description")).append("</p>");
        sb.append("<div style='display: flex; gap: 10px;'>");
        sb.append("<button class='success' onclick='resetLocale(\"en\")' style='flex: 1; padding: 12px;'>🇬🇧 ").append(getTranslation("reset_to_english")).append("</button>");
        sb.append("<button class='success' onclick='resetLocale(\"cz\")' style='flex: 1; padding: 12px;'>🇨🇿 ").append(getTranslation("reset_to_czech")).append("</button>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("<div id='config-editor'></div>"); // JS will render forms here
        sb.append("<button id='save-btn' class='success' onclick='saveConfig()' style='width:100%; margin-top:20px; padding: 15px; font-size: 1.1em;'>").append(getTranslation("save")).append("</button>");
        sb.append("</div>"); // End Config
        
        sb.append("</div>"); // End Container
        
        // JavaScript
        sb.append("<script>");
        sb.append("const config = ").append(configJson).append(";");
        sb.append("const translations = { add: '➕ ").append(getTranslation("add")).append("', remove: '✕', saved: '✓ ").append(getTranslation("saved")).append("', saving: '⏳ ").append(getTranslation("saving")).append("...' };");
        sb.append("const fieldNames = {");
        // Pass field translations to JS
        String langCode = WebConfig.getInstance().getLanguage();
        Map<String, String> currentLang = LANG.getOrDefault(langCode, LANG.get("en"));
        for (Map.Entry<String, String> entry : currentLang.entrySet()) {
            if (entry.getKey().contains(".")) {
                sb.append("'").append(entry.getKey()).append("': '").append(entry.getValue().replace("'", "\\'")).append("',");
            }
        }
        sb.append("};");
        
        sb.append("function getFieldName(prefix, key) {");
        sb.append("  return fieldNames[prefix + '.' + key] || key;");
        sb.append("}");
        
        sb.append("function switchTab(tabId) {");
        sb.append("  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));");
        sb.append("  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));");
        sb.append("  document.querySelector(`.tab[onclick*='${tabId}']`).classList.add('active');");
        sb.append("  document.getElementById(tabId).classList.add('active');");
        sb.append("}");
        
        sb.append("function renderConfig() {");
        sb.append("  const container = document.getElementById('config-editor');");
        sb.append("  let html = '';");
        
        // Web Config
        sb.append("  html += `<div class='card'><h2>🌐 Web</h2>`;");
        sb.append("  html += `<div class='form-group'><label>${getFieldName('web', 'language')}</label>`;");
        sb.append("  const webLangDesc = fieldNames['desc.web.language'];");
        sb.append("  if(webLangDesc) html += `<p style='color:#888;font-size:0.85em;margin:5px 0 10px;font-style:italic;'>${webLangDesc}</p>`;");
        sb.append("  html += `<div style='display:flex;gap:10px;'>`;");
        sb.append("  html += `<button class='${config.web.language==\"en\"?\"success\":\"\"}' onclick='changeLanguage(\"en\")' style='flex:1;padding:12px;${config.web.language==\"en\"?\"background:linear-gradient(135deg,#00b09b,#96c93d);\":\"\"}'>🇬🇧 English</button>`;");
        sb.append("  html += `<button class='${config.web.language==\"cz\"?\"success\":\"\"}' onclick='changeLanguage(\"cz\")' style='flex:1;padding:12px;${config.web.language==\"cz\"?\"background:linear-gradient(135deg,#00b09b,#96c93d);\":\"\"}'>🇨🇿 Čeština</button>`;");
        sb.append("  html += `</div></div></div>`;");
        
        // General
        sb.append("  html += renderSection('⚙️ General', config.general, 'general');");
        
        // Restart
        sb.append("  html += `<div class='card'><h2>🔄 Restart</h2>`;");
        sb.append("  html += renderField('restartType', config.restart.restartType, 'restart', 'select', ['FIXED_TIME','INTERVAL','DELAY']);");
        sb.append("  html += `<div id='restart-fixed' style='display:${config.restart.restartType=='FIXED_TIME'?'block':'none'}'>` + renderList('fixedRestartTimes', config.restart.fixedRestartTimes, 'restart', 'time') + `</div>`;");
        sb.append("  html += `<div id='restart-interval' style='display:${config.restart.restartType=='INTERVAL'?'block':'none'}'>` + renderField('intervalHours', config.restart.intervalHours, 'restart', 'number') + `</div>`;");
        sb.append("  html += `<div id='restart-delay' style='display:${config.restart.restartType=='DELAY'?'block':'none'}'>` + renderField('delayMinutes', config.restart.delayMinutes, 'restart', 'number') + `</div>`;");
        sb.append("  html += `</div>`;");
        
        // Announcements
        sb.append("  html += `<div class='card'><h2>📢 Announcements</h2>`;");
        sb.append("  html += renderField('prefix', config.announcement.prefix, 'announcement', 'text');");
        sb.append("  html += renderField('announcementIntervalMinutes', config.announcement.announcementIntervalMinutes, 'announcement', 'number');");
        sb.append("  html += renderList('announcements', config.announcement.announcements, 'announcement', 'text');");
        sb.append("  html += `</div>`;");
        
        // Ranks
        sb.append("  html += `<div class='card'><h2>🏆 Ranks</h2>`;");
        sb.append("  html += renderField('enableAutoRanks', config.ranks.enableAutoRanks, 'ranks', 'checkbox');");
        sb.append("  html += renderField('checkIntervalMinutes', config.ranks.checkIntervalMinutes, 'ranks', 'number');");
        sb.append("  html += renderField('promotionMessage', config.ranks.promotionMessage, 'ranks', 'text');");
        sb.append("  html += renderObjectList('ranks', config.ranks.ranks, 'ranks', [{key:'type',type:'select',opts:['PREFIX','SUFFIX']},{key:'value',type:'text'},{key:'hours',type:'number'}]);");
        sb.append("  html += `</div>`;");
        
        // Discord (explicit rendering for all fields)
        sb.append("  html += `<div class='card'><h2>💬 Discord</h2>`;");
        sb.append("  html += renderField('enableDiscord', config.discord.enableDiscord, 'discord', 'checkbox');");
        sb.append("  html += renderField('botToken', config.discord.botToken, 'discord', 'text');");
        sb.append("  html += renderField('guildId', config.discord.guildId, 'discord', 'text');");
        sb.append("  html += renderField('enableWhitelist', config.discord.enableWhitelist, 'discord', 'checkbox');");
        sb.append("  html += renderField('kickMessage', config.discord.kickMessage, 'discord', 'text');");
        sb.append("  html += renderField('linkSuccessMessage', config.discord.linkSuccessMessage, 'discord', 'text');");
        sb.append("  html += renderField('alreadyLinkedMessage', config.discord.alreadyLinkedMessage, 'discord', 'text');");
        sb.append("  html += renderField('maxAccountsPerDiscord', config.discord.maxAccountsPerDiscord, 'discord', 'number');");
        sb.append("  html += renderField('chatChannelId', config.discord.chatChannelId, 'discord', 'text');");
        sb.append("  html += renderField('consoleChannelId', config.discord.consoleChannelId, 'discord', 'text');");
        sb.append("  html += renderField('statusChannelId', config.discord.statusChannelId, 'discord', 'text');");
        sb.append("  html += renderField('linkChannelId', config.discord.linkChannelId, 'discord', 'text');");
        sb.append("  html += renderField('linkedRoleId', config.discord.linkedRoleId, 'discord', 'text');");
        sb.append("  html += renderField('syncBansDiscordToMc', config.discord.syncBansDiscordToMc, 'discord', 'checkbox');");
        sb.append("  html += renderField('syncBansMcToDiscord', config.discord.syncBansMcToDiscord, 'discord', 'checkbox');");
        sb.append("  html += renderField('enableChatBridge', config.discord.enableChatBridge, 'discord', 'checkbox');");
        sb.append("  html += renderField('minecraftToDiscordFormat', config.discord.minecraftToDiscordFormat, 'discord', 'text');");
        sb.append("  html += renderField('discordToMinecraftFormat', config.discord.discordToMinecraftFormat, 'discord', 'text');");
        sb.append("  html += renderField('translateEmojis', config.discord.translateEmojis, 'discord', 'checkbox');");
        sb.append("  html += renderField('chatWebhookUrl', config.discord.chatWebhookUrl, 'discord', 'text');");
        sb.append("  html += renderField('enableConsoleLog', config.discord.enableConsoleLog, 'discord', 'checkbox');");
        sb.append("  html += renderField('enableStatusMessages', config.discord.enableStatusMessages, 'discord', 'checkbox');");
        sb.append("  html += renderField('statusMessageStarting', config.discord.statusMessageStarting, 'discord', 'text');");
        sb.append("  html += renderField('statusMessageStarted', config.discord.statusMessageStarted, 'discord', 'text');");
        sb.append("  html += renderField('statusMessageStopping', config.discord.statusMessageStopping, 'discord', 'text');");
        sb.append("  html += renderField('statusMessageStopped', config.discord.statusMessageStopped, 'discord', 'text');");
        sb.append("  html += renderField('enableTopicUpdate', config.discord.enableTopicUpdate, 'discord', 'checkbox');");
        sb.append("  html += renderField('channelTopicFormat', config.discord.channelTopicFormat, 'discord', 'text');");
        sb.append("  html += renderField('uptimeFormat', config.discord.uptimeFormat, 'discord', 'text');");
        sb.append("  html += `</div>`;");
        
        // Stats (simple fields only)
        sb.append("  html += renderSection('📊 Stats', config.stats, 'stats');");
        
        // Vote (explicit rendering for commands list)
        sb.append("  html += `<div class='card'><h2>🗳️ Vote</h2>`;");
        sb.append("  html += renderField('enabled', config.vote.enabled, 'vote', 'checkbox');");
        sb.append("  html += renderField('host', config.vote.host, 'vote', 'text');");
        sb.append("  html += renderField('port', config.vote.port, 'vote', 'number');");
        sb.append("  html += renderField('rsaPrivateKeyPath', config.vote.rsaPrivateKeyPath, 'vote', 'text');");
        sb.append("  html += renderField('rsaPublicKeyPath', config.vote.rsaPublicKeyPath, 'vote', 'text');");
        sb.append("  html += renderField('sharedSecret', config.vote.sharedSecret, 'vote', 'text');");
        sb.append("  html += renderField('announceVotes', config.vote.announceVotes, 'vote', 'checkbox');");
        sb.append("  html += renderField('announcementMessage', config.vote.announcementMessage, 'vote', 'text');");
        sb.append("  html += renderField('announcementCooldown', config.vote.announcementCooldown, 'vote', 'number');");
        sb.append("  html += renderList('commands', config.vote.commands, 'vote', 'text');");
        sb.append("  html += `</div>`;");
        
        sb.append("  container.innerHTML = html;");
        sb.append("}");
        
        // Helper functions for rendering
        sb.append("function renderSection(title, obj, prefix) {");
        sb.append("  let h = `<div class='card'><h2>${title}</h2>`;");
        sb.append("  for(let key in obj) {");
        sb.append("    if(typeof obj[key] === 'object') continue;"); // Skip complex objects in auto-render
        sb.append("    h += renderField(key, obj[key], prefix, typeof obj[key] === 'boolean' ? 'checkbox' : (typeof obj[key] === 'number' ? 'number' : 'text'));");
        sb.append("  }");
        sb.append("  h += `</div>`;");
        sb.append("  return h;");
        sb.append("}");
        
        sb.append("function renderField(key, value, prefix, type, opts) {");
        sb.append("  const desc = fieldNames['desc.' + prefix + '.' + key];");
        sb.append("  let h = `<div class='form-group'><label>${getFieldName(prefix, key)}</label>`;");
        sb.append("  if(desc) h += `<p style='color:#888;font-size:0.85em;margin:5px 0 10px;font-style:italic;'>${desc}</p>`;");
        sb.append("  if(type === 'checkbox') {");
        sb.append("    h += `<label class='switch'><input type='checkbox' onchange='updateValue(\"${prefix}\", \"${key}\", this.checked)' ${value?'checked':''}><span class='slider'></span></label>`;");
        sb.append("  } else if(type === 'select') {");
        sb.append("    h += `<select onchange='updateValue(\"${prefix}\", \"${key}\", this.value); if(\"${key}\"===\"restartType\") updateRestartUi(this.value);'>`;");
        sb.append("    opts.forEach(o => h += `<option value='${o}' ${value===o?'selected':''}>${o}</option>`);");
        sb.append("    h += `</select>`;");
        sb.append("  } else {");
        sb.append("    h += `<input type='${type}' value='${value}' onchange='updateValue(\"${prefix}\", \"${key}\", this.value)'>`;");
        sb.append("  }");
        sb.append("  h += `</div>`;");
        sb.append("  return h;");
        sb.append("}");
        
        sb.append("function renderList(key, list, prefix, type) {");
        sb.append("  let h = `<h3 style='color:#bb86fc;font-size:1.1em;margin:20px 0 15px;'>${getFieldName(prefix, key)}</h3><div id='list-${prefix}-${key}'>`;");
        sb.append("  list.forEach((item, i) => {");
        sb.append("    h += `<div class='array-item'><input type='${type}' value='${item}' onchange='config.${prefix}.${key}[${i}]=this.value' style='flex:1;'><button class='danger' onclick='removeItem(\"${prefix}\", \"${key}\", ${i})' style='padding:10px 15px;min-width:auto;'>${translations.remove}</button></div>`;");
        sb.append("  });");
        sb.append("  h += `</div><button class='success' onclick='addItem(\"${prefix}\", \"${key}\", \"${type}\")' style='margin-top:10px;'>${translations.add}</button>`;");
        sb.append("  return h;");
        sb.append("}");
        
        sb.append("function renderObjectList(key, list, prefix, fields) {");
        sb.append("  let h = `<h3 style='color:#bb86fc;font-size:1.1em;margin:20px 0 15px;'>${getFieldName(prefix, key)}</h3><div id='list-${prefix}-${key}'>`;");
        sb.append("  list.forEach((item, i) => {");
        sb.append("    h += `<div class='card' style='background:rgba(187,134,252,0.05);border:1px solid rgba(187,134,252,0.15);padding:20px;margin-bottom:15px;'><div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:15px;align-items:end;'>`;");
        sb.append("    fields.forEach(f => {");
        sb.append("      h += `<div>`;");
        sb.append("      if(f.type==='select') {");
        sb.append("        h += `<label style='display:block;margin-bottom:8px;color:#aaa;font-size:0.85em;text-transform:uppercase;'>${getFieldName(prefix, f.key)}</label>`;");
        sb.append("        h += `<select onchange='config.${prefix}.${key}[${i}].${f.key}=this.value' style='margin:0;'>`;");
        sb.append("        f.opts.forEach(o => h += `<option value='${o}' ${item[f.key]===o?'selected':''}>${o}</option>`);");
        sb.append("        h += `</select>`;");
        sb.append("      } else {");
        sb.append("        h += `<label style='display:block;margin-bottom:8px;color:#aaa;font-size:0.85em;text-transform:uppercase;'>${getFieldName(prefix, f.key)}</label>`;");
        sb.append("        h += `<input type='${f.type}' value='${item[f.key]}' placeholder='${getFieldName(prefix, f.key)}' onchange='config.${prefix}.${key}[${i}].${f.key}=this.value' style='margin:0;'>`;");
        sb.append("      }");
        sb.append("      h += `</div>`;");
        sb.append("    });");
        sb.append("    h += `<button class='danger' onclick='removeItem(\"${prefix}\", \"${key}\", ${i})' style='padding:10px 15px;'>${translations.remove}</button></div></div>`;");
        sb.append("  });");
        sb.append("  h += `</div><button class='success' onclick='addRankItem()' style='margin-top:10px;'>${translations.add}</button>`;");
        sb.append("  return h;");
        sb.append("}");
        
        sb.append("function updateValue(prefix, key, value) {");
        sb.append("  if(prefix) config[prefix][key] = value;");
        sb.append("}");
        
        sb.append("function updateRestartUi(type) {");
        sb.append("  document.getElementById('restart-fixed').style.display = type==='FIXED_TIME'?'block':'none';");
        sb.append("  document.getElementById('restart-interval').style.display = type==='INTERVAL'?'block':'none';");
        sb.append("  document.getElementById('restart-delay').style.display = type==='DELAY'?'block':'none';");
        sb.append("}");
        
        sb.append("function removeItem(prefix, key, index) {");
        sb.append("  config[prefix][key].splice(index, 1);");
        sb.append("  renderConfig();");
        sb.append("}");
        
        sb.append("function addItem(prefix, key, type) {");
        sb.append("  config[prefix][key].push(type==='time'?'12:00':'New Item');");
        sb.append("  renderConfig();");
        sb.append("}");
        
        sb.append("function addRankItem() {");
        sb.append("  config.ranks.ranks.push({type:'PREFIX', value:'[New]', hours:10});");
        sb.append("  renderConfig();");
        sb.append("}");
        
        sb.append("function changeLanguage(lang) {");
        sb.append("  let currentTab = 'config';");
        sb.append("  const activeTabEl = document.querySelector('.tab.active');");
        sb.append("  if(activeTabEl) {");
        sb.append("    const match = activeTabEl.getAttribute('onclick').match(/switchTab\\('(.+?)'\\)/);");
        sb.append("    if(match) currentTab = match[1];");
        sb.append("  }");
        sb.append("  config.web.language = lang;");
        sb.append("  showToast('⏳ Changing language...', 'info');");
        sb.append("  fetch('/api/config', {");
        sb.append("    method: 'POST',");
        sb.append("    headers: {'Content-Type': 'application/json'},");
        sb.append("    body: JSON.stringify({web: {language: lang}})");
        sb.append("  }).then(r => r.json()).then(d => {");
        sb.append("    if(d.status === 'ok') {");
        sb.append("      showToast('✓ Language changed', 'success');");
        sb.append("      setTimeout(() => {");
        sb.append("        const url = new URL(window.location);");
        sb.append("        url.searchParams.set('tab', currentTab);");
        sb.append("        window.location.href = url.toString();");
        sb.append("      }, 800);");
        sb.append("    } else {");
        sb.append("      showToast('❌ Error changing language', 'error');");
        sb.append("    }");
        sb.append("  }).catch(err => {");
        sb.append("    showToast('❌ Network error', 'error');");
        sb.append("  });");
        sb.append("}");
        
        sb.append("function saveConfig() {");
        sb.append("  const btn = document.getElementById('save-btn');");
        sb.append("  const originalText = btn.innerText;");
        sb.append("  btn.innerText = translations.saving;");
        sb.append("  btn.disabled = true;");
        sb.append("  fetch('/api/config', {");
        sb.append("    method: 'POST',");
        sb.append("    headers: {'Content-Type': 'application/json'},");
        sb.append("    body: JSON.stringify(config)");
        sb.append("  }).then(r => r.json()).then(d => {");
        sb.append("    if(d.status === 'ok') {");
        sb.append("      btn.innerText = translations.saved;");
        sb.append("      btn.classList.add('success-anim');");
        sb.append("      btn.style.background = 'linear-gradient(135deg, #00b09b, #96c93d)';");
        sb.append("      showToast(translations.saved, 'success');");
        sb.append("      setTimeout(() => {");
        sb.append("        btn.innerText = originalText;");
        sb.append("        btn.disabled = false;");
        sb.append("        btn.classList.remove('success-anim');");
        sb.append("        btn.style.background = '';");
        sb.append("      }, 2000);");
        sb.append("    } else {");
        sb.append("      btn.innerText = '❌ Error';");
        sb.append("      btn.style.background = 'linear-gradient(135deg, #d32f2f, #f44336)';");
        sb.append("      showToast(d.message || 'Error saving configuration', 'error');");
        sb.append("      setTimeout(() => {");
        sb.append("        btn.innerText = originalText;");
        sb.append("        btn.disabled = false;");
        sb.append("        btn.style.background = '';");
        sb.append("      }, 2000);");
        sb.append("    }");
        sb.append("  }).catch(err => {");
        sb.append("    btn.innerText = '❌ Error';");
        sb.append("    btn.style.background = 'linear-gradient(135deg, #d32f2f, #f44336)';");
        sb.append("    showToast('Network error', 'error');");
        sb.append("    setTimeout(() => {");
        sb.append("      btn.innerText = originalText;");
        sb.append("      btn.disabled = false;");
        sb.append("      btn.style.background = '';");
        sb.append("    }, 2000);");
        sb.append("  });");
        sb.append("}");
        
        // Add toast notification function
        sb.append("function showToast(message, type) {");
        sb.append("  const toast = document.createElement('div');");
        sb.append("  toast.textContent = message;");
        sb.append("  toast.style.cssText = `position:fixed;top:20px;right:20px;padding:15px 25px;border-radius:12px;color:#fff;font-weight:600;z-index:10000;animation:slideIn 0.3s ease;box-shadow:0 10px 40px rgba(0,0,0,0.5);`;");
        sb.append("  if(type === 'success') toast.style.background = 'linear-gradient(135deg, #00b09b, #96c93d)';");
        sb.append("  else if(type === 'error') toast.style.background = 'linear-gradient(135deg, #d32f2f, #f44336)';");
        sb.append("  else if(type === 'info') toast.style.background = 'linear-gradient(135deg, #2196F3, #21CBF3)';");
        sb.append("  document.body.appendChild(toast);");
        sb.append("  setTimeout(() => {");
        sb.append("    toast.style.animation = 'slideOut 0.3s ease';");
        sb.append("    setTimeout(() => toast.remove(), 300);");
        sb.append("  }, 3000);");
        sb.append("}");
        
        // Add locale reset function
        sb.append("function resetLocale(locale) {");
        sb.append("  const confirmMsg = locale === 'en' ? '").append(getTranslation("locale_reset_confirm_en")).append("' : '").append(getTranslation("locale_reset_confirm_cz")).append("';");
        sb.append("  if(!confirm(confirmMsg)) return;");
        sb.append("  showToast('⏳ Resetting messages...', 'info');");
        sb.append("  fetch('/api/locale', {");
        sb.append("    method: 'POST',");
        sb.append("    headers: {'Content-Type': 'application/x-www-form-urlencoded'},");
        sb.append("    body: 'locale=' + locale");
        sb.append("  }).then(r => r.json()).then(d => {");
        sb.append("    if(d.status === 'ok') {");
        sb.append("      const successMsg = '").append(getTranslation("locale_reset_success")).append("'.replace('{locale}', locale.toUpperCase());");
        sb.append("      showToast(successMsg, 'success');");
        sb.append("      setTimeout(() => location.reload(), 1500);");
        sb.append("    } else {");
        sb.append("      showToast('❌ Error: ' + (d.message || 'Failed to reset'), 'error');");
        sb.append("    }");
        sb.append("  }).catch(err => {");
        sb.append("    showToast('❌ Network error', 'error');");
        sb.append("  });");
        sb.append("}");
        
        // Add CSS animations for toast
        sb.append("const style = document.createElement('style');");
        sb.append("style.textContent = '@keyframes slideIn { from { transform: translateX(400px); opacity: 0; } to { transform: translateX(0); opacity: 1; } } @keyframes slideOut { from { transform: translateX(0); opacity: 1; } to { transform: translateX(400px); opacity: 0; } }';");
        sb.append("document.head.appendChild(style);");
        
        // Initialize correct tab on load
        sb.append("window.addEventListener('DOMContentLoaded', function() {");
        sb.append("  const urlParams = new URLSearchParams(window.location.search);");
        sb.append("  const tab = urlParams.get('tab');");
        sb.append("  if(tab && document.getElementById(tab)) {");
        sb.append("    switchTab(tab);");
        sb.append("  }");
        sb.append("});");
        
        sb.append("renderConfig();");
        sb.append("</script>");
        
        sb.append("</body></html>");
        return sb.toString();
    }
}
