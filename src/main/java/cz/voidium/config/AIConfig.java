package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AIConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AIConfig instance;
    private transient Path configPath;

    private boolean enablePlayerChat = false;
    private boolean enableAdminAssistant = false;
    private boolean redactSensitiveValues = true;
    private int playerPromptMaxLength = 280;
    private int adminPromptMaxLength = 5000;
    private int adminContextMaxChars = 18000;
    private int playerCooldownSeconds = 20;
    private String playerAccessMode = "ALL";
    private int playerAccessMinHours = 0;
    private List<String> playerAccessDiscordRoleIds = new ArrayList<>();
    private List<String> disabledWorlds = new ArrayList<>();
    private List<String> disabledGameModes = new ArrayList<>();
    private EndpointProfile playerApi = EndpointProfile.playerDefaults();
    private EndpointProfile adminApi = EndpointProfile.adminDefaults();

    public static class EndpointProfile {
        private String endpointUrl = "https://api.openai.com/v1/chat/completions";
        private String apiKey = "PUT_API_KEY_HERE";
        private String authHeaderName = "Authorization";
        private String authHeaderPrefix = "Bearer ";
        private String model = "gpt-4.1-mini";
        private String systemPrompt = "";
        private double temperature = 0.5;
        private int maxTokens = 500;
        private int timeoutSeconds = 45;

        public static EndpointProfile playerDefaults() {
            EndpointProfile profile = new EndpointProfile();
            profile.systemPrompt = "You are Voidium AI inside a Minecraft server. Keep answers compact, helpful, and safe for in-game chat. Prefer practical answers. Never claim to perform actions you cannot perform.";
            profile.maxTokens = 280;
            profile.temperature = 0.7;
            return profile;
        }

        public static EndpointProfile adminDefaults() {
            EndpointProfile profile = new EndpointProfile();
            profile.systemPrompt = "You are the Voidium admin copilot inside a Minecraft control panel. Use the provided server and config context to give concrete operational help, identify risks, suggest better config values, and explain tradeoffs. Never expose masked secrets back to the user. Be concise and action-oriented.";
            profile.maxTokens = 900;
            profile.temperature = 0.35;
            return profile;
        }

        public String getEndpointUrl() { return endpointUrl; }
        public String getApiKey() { return apiKey; }
        public String getAuthHeaderName() { return authHeaderName; }
        public String getAuthHeaderPrefix() { return authHeaderPrefix; }
        public String getModel() { return model; }
        public String getSystemPrompt() { return systemPrompt; }
        public double getTemperature() { return temperature; }
        public int getMaxTokens() { return maxTokens; }
        public int getTimeoutSeconds() { return timeoutSeconds; }

        public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public void setAuthHeaderName(String authHeaderName) { this.authHeaderName = authHeaderName; }
        public void setAuthHeaderPrefix(String authHeaderPrefix) { this.authHeaderPrefix = authHeaderPrefix; }
        public void setModel(String model) { this.model = model; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public AIConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static AIConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("ai.json");
        instance = load(configPath);
    }

    private static AIConfig load(Path configPath) {
        AIConfig config = new AIConfig(configPath);
        if (Files.exists(configPath)) {
            try {
                AIConfig loaded = ConfigFileHelper.loadJson(configPath, GSON, AIConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    loaded.normalize();
                    loaded.save();
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load AI configuration: " + e.getMessage());
            }
        }
        config.normalize();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === AI CONFIGURATION ===\n");
                writer.write("// enablePlayerChat: enables /ai for players\n");
                writer.write("// enableAdminAssistant: enables the admin AI panel inside Voidium Web\n");
                writer.write("// Use separate API profiles for player chat and admin assistant\n");
                writer.write("// endpointUrl expects an OpenAI-compatible chat completions endpoint\n");
                writer.write("// redactSensitiveValues masks tokens and secrets before config context is sent to admin AI\n\n");
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save AI configuration: " + e.getMessage());
        }
    }

    private void normalize() {
        if (playerApi == null) {
            playerApi = EndpointProfile.playerDefaults();
        }
        if (adminApi == null) {
            adminApi = EndpointProfile.adminDefaults();
        }
        if (playerApi.endpointUrl == null || playerApi.endpointUrl.isBlank()) {
            playerApi.endpointUrl = EndpointProfile.playerDefaults().endpointUrl;
        }
        if (adminApi.endpointUrl == null || adminApi.endpointUrl.isBlank()) {
            adminApi.endpointUrl = EndpointProfile.adminDefaults().endpointUrl;
        }
        if (playerApi.model == null || playerApi.model.isBlank()) {
            playerApi.model = EndpointProfile.playerDefaults().model;
        }
        if (adminApi.model == null || adminApi.model.isBlank()) {
            adminApi.model = EndpointProfile.adminDefaults().model;
        }
        if (playerApi.systemPrompt == null) {
            playerApi.systemPrompt = EndpointProfile.playerDefaults().systemPrompt;
        }
        if (adminApi.systemPrompt == null) {
            adminApi.systemPrompt = EndpointProfile.adminDefaults().systemPrompt;
        }
        if (playerApi.authHeaderName == null || playerApi.authHeaderName.isBlank()) {
            playerApi.authHeaderName = "Authorization";
        }
        if (adminApi.authHeaderName == null || adminApi.authHeaderName.isBlank()) {
            adminApi.authHeaderName = "Authorization";
        }
        if (playerApi.authHeaderPrefix == null) {
            playerApi.authHeaderPrefix = "Bearer ";
        }
        if (adminApi.authHeaderPrefix == null) {
            adminApi.authHeaderPrefix = "Bearer ";
        }
        if (playerPromptMaxLength < 32) {
            playerPromptMaxLength = 280;
        }
        if (adminPromptMaxLength < 200) {
            adminPromptMaxLength = 5000;
        }
        if (adminContextMaxChars < 1000) {
            adminContextMaxChars = 18000;
        }
        if (playerCooldownSeconds < 0) {
            playerCooldownSeconds = 20;
        }
        if (playerAccessMode == null || playerAccessMode.isBlank()) {
            playerAccessMode = "ALL";
        }
        if (!List.of("ALL", "PLAYTIME", "DISCORD_ROLE", "PLAYTIME_OR_DISCORD_ROLE", "PLAYTIME_AND_DISCORD_ROLE").contains(playerAccessMode)) {
            playerAccessMode = "ALL";
        }
        if (playerAccessMinHours < 0) {
            playerAccessMinHours = 0;
        }
        if (playerAccessDiscordRoleIds == null) {
            playerAccessDiscordRoleIds = new ArrayList<>();
        }
        playerAccessDiscordRoleIds.removeIf(roleId -> roleId == null || roleId.isBlank());
        if (disabledWorlds == null) {
            disabledWorlds = new ArrayList<>();
        }
        disabledWorlds.removeIf(w -> w == null || w.isBlank());
        if (disabledGameModes == null) {
            disabledGameModes = new ArrayList<>();
        }
        disabledGameModes.removeIf(m -> m == null || m.isBlank());
        if (playerApi.maxTokens < 1) {
            playerApi.maxTokens = EndpointProfile.playerDefaults().maxTokens;
        }
        if (adminApi.maxTokens < 1) {
            adminApi.maxTokens = EndpointProfile.adminDefaults().maxTokens;
        }
        if (playerApi.timeoutSeconds < 1) {
            playerApi.timeoutSeconds = 45;
        }
        if (adminApi.timeoutSeconds < 1) {
            adminApi.timeoutSeconds = 45;
        }
    }

    public boolean isEnablePlayerChat() { return enablePlayerChat; }
    public boolean isEnableAdminAssistant() { return enableAdminAssistant; }
    public boolean isRedactSensitiveValues() { return redactSensitiveValues; }
    public int getPlayerPromptMaxLength() { return playerPromptMaxLength; }
    public int getAdminPromptMaxLength() { return adminPromptMaxLength; }
    public int getAdminContextMaxChars() { return adminContextMaxChars; }
    public int getPlayerCooldownSeconds() { return playerCooldownSeconds; }
    public String getPlayerAccessMode() { return playerAccessMode; }
    public int getPlayerAccessMinHours() { return playerAccessMinHours; }
    public List<String> getPlayerAccessDiscordRoleIds() { return playerAccessDiscordRoleIds; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
    public List<String> getDisabledGameModes() { return disabledGameModes; }
    public EndpointProfile getPlayerApi() { return playerApi; }
    public EndpointProfile getAdminApi() { return adminApi; }

    public void setEnablePlayerChat(boolean enablePlayerChat) { this.enablePlayerChat = enablePlayerChat; }
    public void setEnableAdminAssistant(boolean enableAdminAssistant) { this.enableAdminAssistant = enableAdminAssistant; }
    public void setRedactSensitiveValues(boolean redactSensitiveValues) { this.redactSensitiveValues = redactSensitiveValues; }
    public void setPlayerPromptMaxLength(int playerPromptMaxLength) { this.playerPromptMaxLength = playerPromptMaxLength; }
    public void setAdminPromptMaxLength(int adminPromptMaxLength) { this.adminPromptMaxLength = adminPromptMaxLength; }
    public void setAdminContextMaxChars(int adminContextMaxChars) { this.adminContextMaxChars = adminContextMaxChars; }
    public void setPlayerCooldownSeconds(int playerCooldownSeconds) { this.playerCooldownSeconds = playerCooldownSeconds; }
    public void setPlayerAccessMode(String playerAccessMode) { this.playerAccessMode = playerAccessMode; }
    public void setPlayerAccessMinHours(int playerAccessMinHours) { this.playerAccessMinHours = playerAccessMinHours; }
    public void setPlayerAccessDiscordRoleIds(List<String> playerAccessDiscordRoleIds) { this.playerAccessDiscordRoleIds = playerAccessDiscordRoleIds == null ? new ArrayList<>() : new ArrayList<>(playerAccessDiscordRoleIds); }
    public void setDisabledWorlds(List<String> disabledWorlds) { this.disabledWorlds = disabledWorlds == null ? new ArrayList<>() : new ArrayList<>(disabledWorlds); }
    public void setDisabledGameModes(List<String> disabledGameModes) { this.disabledGameModes = disabledGameModes == null ? new ArrayList<>() : new ArrayList<>(disabledGameModes); }
}