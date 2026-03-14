package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class WebConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WebConfig instance;
    private transient Path configPath;

    private int port = 8081;
    private String language = "en"; // Options: "en", "cz"
    private String publicHostname = "localhost";
    private String bindAddress = "0.0.0.0";
    private String adminToken = generateToken();
    private int sessionTtlMinutes = 120;

    public WebConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static WebConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("web.json");
        if (Files.exists(configPath)) {
            try {
                instance = ConfigFileHelper.loadJson(configPath, GSON, WebConfig.class);
                if (instance == null) {
                    instance = new WebConfig(configPath);
                }
                instance.configPath = configPath;
                instance.normalize();
            } catch (IOException e) {
                e.printStackTrace();
                instance = new WebConfig(configPath);
            }
        } else {
            instance = new WebConfig(configPath);
        }
        instance.normalize();
        instance.save();
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void normalize() {
        if (language == null || language.isBlank() || (!"en".equalsIgnoreCase(language) && !"cz".equalsIgnoreCase(language))) {
            language = "en";
        }
        if (publicHostname == null || publicHostname.isBlank()) {
            publicHostname = "localhost";
        }
        if (bindAddress == null || bindAddress.isBlank()) {
            bindAddress = "0.0.0.0";
        }
        if (adminToken == null || adminToken.isBlank()) {
            adminToken = generateToken();
        }
        if (sessionTtlMinutes < 5) {
            sessionTtlMinutes = 120;
        }
        if (port < 1 || port > 65535) {
            port = 8081;
        }
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    public int getPort() { return port; }
    public String getLanguage() { return language; }
    public String getPublicHostname() { return publicHostname; }
    public String getBindAddress() { return bindAddress; }
    public String getAdminToken() { return adminToken; }
    public int getSessionTtlMinutes() { return sessionTtlMinutes; }

    public void setPort(int port) { this.port = port; }
    public void setLanguage(String language) { this.language = language; }
    public void setPublicHostname(String publicHostname) { this.publicHostname = publicHostname; }
    public void setBindAddress(String bindAddress) { this.bindAddress = bindAddress; }
    public void setAdminToken(String adminToken) { this.adminToken = adminToken; }
    public void setSessionTtlMinutes(int sessionTtlMinutes) { this.sessionTtlMinutes = sessionTtlMinutes; }
}
