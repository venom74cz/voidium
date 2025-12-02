package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class TicketConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TicketConfig instance;
    private transient Path configPath;

    private boolean enableTickets = true;
    private String ticketCategoryId = "";
    private String supportRoleId = "";
    
    // Available placeholders: %user% (Discord user tag), %reason% (ticket reason)
    private String ticketChannelTopic = "Ticket for %user% | Reason: %reason%";
    private int maxTicketsPerUser = 1;
    
    // Messages - Available placeholders:
    // %user% - Discord user mention
    // %channel% - Ticket channel mention
    // %reason% - Ticket reason
    // %max% - Maximum tickets per user
    // Use & for color codes (e.g., &c for red, &a for green)
    
    private String ticketCreatedMessage = "Ticket created in %channel%!";
    private String ticketWelcomeMessage = "Hello %user%,\nSupport will be with you shortly.\nReason: %reason%";
    private String ticketCloseMessage = "Ticket closed by %user%.";
    private String noPermissionMessage = "You do not have permission to do this.";
    
    // Message when user reaches ticket limit. Placeholders: %max% (maximum tickets per user)
    // Zpráva při dosažení limitu ticketů. Proměnné: %max% (maximální počet ticketů na uživatele)
    private String ticketLimitReachedMessage = "You have reached the maximum number of open tickets (%max%).";
    
    private String ticketAlreadyClosedMessage = "This ticket is already closed.";
    
    // Ticket transcript settings
    private boolean enableTranscript = true;
    private String transcriptFormat = "TXT"; // "TXT" or "JSON"
    private String transcriptFilename = "ticket-%user%-%date%.txt";
    
    // === IN-GAME MESSAGES (sent to Minecraft player) ===
    // Message when Discord bot is not connected
    private String mcBotNotConnectedMessage = "&cDiscord bot is not connected.";
    // Message when configured Discord server is not found
    private String mcGuildNotFoundMessage = "&cConfigured Discord server was not found.";
    // Message when ticket category is not configured
    private String mcCategoryNotFoundMessage = "&cTicket category is not configured!";
    // Message when ticket is successfully created
    private String mcTicketCreatedMessage = "&aTicket created on Discord!";
    // Message when player's Discord account is not found on server
    private String mcDiscordNotFoundMessage = "&cYour Discord account was not found on the server.";

    public TicketConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static TicketConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("tickets.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, TicketConfig.class);
                instance.configPath = configPath;
            } catch (IOException e) {
                e.printStackTrace();
                instance = new TicketConfig(configPath);
            }
        } else {
            instance = new TicketConfig(configPath);
            instance.save();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnableTickets() { return enableTickets; }
    public String getTicketCategoryId() { return ticketCategoryId; }
    public String getSupportRoleId() { return supportRoleId; }
    public String getTicketChannelTopic() { return ticketChannelTopic; }
    public int getMaxTicketsPerUser() { return maxTicketsPerUser; }
    
    public String getTicketCreatedMessage() { return ticketCreatedMessage; }
    public String getTicketWelcomeMessage() { return ticketWelcomeMessage; }
    public String getTicketCloseMessage() { return ticketCloseMessage; }
    public String getNoPermissionMessage() { return noPermissionMessage; }
    public String getTicketLimitReachedMessage() { return ticketLimitReachedMessage; }
    public String getTicketAlreadyClosedMessage() { return ticketAlreadyClosedMessage; }
    public boolean isEnableTranscript() { return enableTranscript; }
    public String getTranscriptFormat() { return transcriptFormat; }
    public String getTranscriptFilename() { return transcriptFilename; }
    
    // MC message getters
    public String getMcBotNotConnectedMessage() { return mcBotNotConnectedMessage; }
    public String getMcGuildNotFoundMessage() { return mcGuildNotFoundMessage; }
    public String getMcCategoryNotFoundMessage() { return mcCategoryNotFoundMessage; }
    public String getMcTicketCreatedMessage() { return mcTicketCreatedMessage; }
    public String getMcDiscordNotFoundMessage() { return mcDiscordNotFoundMessage; }

    public void applyLocale(String locale) {
        Map<String, String> messages = LocalePresets.getTicketMessages(locale);
        this.ticketCreatedMessage = messages.get("ticketCreatedMessage");
        this.ticketWelcomeMessage = messages.get("ticketWelcomeMessage");
        this.ticketCloseMessage = messages.get("ticketCloseMessage");
        this.noPermissionMessage = messages.get("noPermissionMessage");
        this.ticketLimitReachedMessage = messages.get("ticketLimitReachedMessage");
        this.ticketAlreadyClosedMessage = messages.get("ticketAlreadyClosedMessage");
        // MC messages
        this.mcBotNotConnectedMessage = messages.get("mcBotNotConnectedMessage");
        this.mcGuildNotFoundMessage = messages.get("mcGuildNotFoundMessage");
        this.mcCategoryNotFoundMessage = messages.get("mcCategoryNotFoundMessage");
        this.mcTicketCreatedMessage = messages.get("mcTicketCreatedMessage");
        this.mcDiscordNotFoundMessage = messages.get("mcDiscordNotFoundMessage");
        save();
    }
}
