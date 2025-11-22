package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class WebConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WebConfig instance;
    private transient Path configPath;

    private int port = 8081;
    private String language = "en"; // Options: "en", "cz"
    private String publicHostname = "localhost";

    public WebConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static WebConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("web.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, WebConfig.class);
                instance.configPath = configPath;
            } catch (IOException e) {
                e.printStackTrace();
                instance = new WebConfig(configPath);
            }
        } else {
            instance = new WebConfig(configPath);
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
    
    public int getPort() { return port; }
    public String getLanguage() { return language; }
    public String getPublicHostname() { return publicHostname; }
}
