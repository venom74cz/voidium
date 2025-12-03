package cz.voidium.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Helper class to manage storage files in config/voidium/storage/ directory.
 * Provides automatic migration from old locations.
 */
public class StorageHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Storage");
    private static Path storageDir;
    
    private StorageHelper() {}
    
    /**
     * Initialize storage directory and create it if needed.
     * @param configDir The voidium config directory (config/voidium/)
     */
    public static void init(Path configDir) {
        storageDir = configDir.resolve("storage");
        try {
            Files.createDirectories(storageDir);
            LOGGER.info("Storage directory initialized: {}", storageDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create storage directory: {}", e.getMessage());
        }
    }
    
    /**
     * Get the storage directory path.
     */
    public static Path getStorageDir() {
        return storageDir;
    }
    
    /**
     * Resolve a storage file path.
     * @param filename The filename (e.g., "links.json")
     * @return Full path in storage directory
     */
    public static Path resolve(String filename) {
        return storageDir.resolve(filename);
    }
    
    /**
     * Migrate a file from old location to storage directory.
     * If old file exists and new doesn't, moves the file.
     * If both exist, keeps the new one and logs a warning.
     * 
     * @param oldPath The old file path
     * @param filename The filename in storage directory
     * @return The new path in storage directory
     */
    public static Path migrateIfNeeded(Path oldPath, String filename) {
        Path newPath = resolve(filename);
        
        if (Files.exists(oldPath)) {
            if (!Files.exists(newPath)) {
                // Migrate: move old file to new location
                try {
                    Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
                    LOGGER.info("Migrated {} to storage/", filename);
                } catch (IOException e) {
                    // Atomic move failed, try regular move
                    try {
                        Files.move(oldPath, newPath);
                        LOGGER.info("Migrated {} to storage/", filename);
                    } catch (IOException e2) {
                        LOGGER.error("Failed to migrate {}: {}", filename, e2.getMessage());
                    }
                }
            } else {
                // Both exist - this shouldn't happen normally, log warning
                LOGGER.warn("Both old and new storage file exist for {}. Using new location, old file ignored.", filename);
            }
        }
        
        return newPath;
    }
    
    /**
     * Migrate multiple files at once.
     * @param configDir The voidium config directory
     * @param filenames Array of filenames to migrate
     */
    public static void migrateAll(Path configDir, String... filenames) {
        for (String filename : filenames) {
            Path oldPath = configDir.resolve(filename);
            migrateIfNeeded(oldPath, filename);
        }
    }
}
