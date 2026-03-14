package cz.voidium.entitycleaner;

import cz.voidium.config.EntityCleanerConfig;
import cz.voidium.config.VoidiumConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EntityCleaner {
    private final MinecraftServer server;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cleanupTask;
    private int secondsUntilCleanup;
    
    public EntityCleaner(MinecraftServer server) {
        this.server = server;
    }
    
    public void start() {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        if (config == null || !config.isEnabled()) {
            System.out.println("[EntityCleaner] Disabled or config not loaded.");
            return;
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleNextCleanup();
        System.out.println("[EntityCleaner] Started with interval of " + config.getCleanupIntervalSeconds() + " seconds.");
    }
    
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
            cleanupTask = null;
        }
        System.out.println("[EntityCleaner] Stopped.");
    }
    
    public void reload() {
        stop();
        start();
    }
    
    private void scheduleNextCleanup() {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        if (config == null || !config.isEnabled()) return;
        
        secondsUntilCleanup = config.getCleanupIntervalSeconds();
        
        // Schedule countdown task every second
        cleanupTask = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }
    
    private void tick() {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        if (config == null || !config.isEnabled()) return;
        
        secondsUntilCleanup--;
        
        // Check if we need to send warning
        if (config.getWarningTimes().contains(secondsUntilCleanup)) {
            broadcastWarning(secondsUntilCleanup);
        }
        
        // Time to clean
        if (secondsUntilCleanup <= 0) {
            CleanupResult result = cleanEntities(true, true, true, true);
            broadcastCleanupResult(result);
            
            // Schedule next cleanup
            secondsUntilCleanup = config.getCleanupIntervalSeconds();
        }
    }
    
    private void broadcastWarning(int seconds) {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        String message = config.getWarningMessage()
                .replace("%seconds%", String.valueOf(seconds));
        
        server.execute(() -> {
            server.getPlayerList().getPlayers().forEach(player ->
                player.sendSystemMessage(VoidiumConfig.formatMessage(message))
            );
        });
    }
    
    private void broadcastCleanupResult(CleanupResult result) {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        String message = config.getCleanupMessage()
                .replace("%items%", String.valueOf(result.items))
                .replace("%mobs%", String.valueOf(result.mobs))
                .replace("%xp%", String.valueOf(result.xpOrbs))
                .replace("%arrows%", String.valueOf(result.arrows))
                .replace("%total%", String.valueOf(result.total()));
        
        server.execute(() -> {
            server.getPlayerList().getPlayers().forEach(player ->
                player.sendSystemMessage(VoidiumConfig.formatMessage(message))
            );
        });
    }
    
    /**
     * Clean entities based on config settings.
     * Returns cleanup result with counts.
     */
    public CleanupResult cleanEntities(boolean items, boolean mobs, boolean xp, boolean arrows) {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        CleanupResult result = new CleanupResult();
        
        // We need to run this synchronously to get accurate counts
        // Use CountDownLatch to wait for completion if called from scheduler thread
        if (Thread.currentThread().getName().contains("Server") || server.isSameThread()) {
            // Already on server thread, execute directly
            performCleanup(config, result, items, mobs, xp, arrows);
        } else {
            // Called from scheduler, need to execute on server thread and wait
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            server.execute(() -> {
                performCleanup(config, result, items, mobs, xp, arrows);
                latch.countDown();
            });
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return result;
    }
    
    private void performCleanup(EntityCleanerConfig config, CleanupResult result, 
                                 boolean items, boolean mobs, boolean xp, boolean arrows) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> toRemove = new ArrayList<>();
            
            for (Entity entity : level.getAllEntities()) {
                if (shouldRemove(entity, config, items, mobs, xp, arrows)) {
                    toRemove.add(entity);
                    // Count by type
                    if (entity instanceof ItemEntity) result.items++;
                    else if (entity instanceof ExperienceOrb) result.xpOrbs++;
                    else if (entity instanceof AbstractArrow) result.arrows++;
                    else result.mobs++;
                }
            }
            
            // Remove entities
            for (Entity entity : toRemove) {
                entity.discard();
            }
        }
    }
    
    /**
     * Get preview of entities that would be removed.
     */
    public CleanupResult previewCleanup() {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        CleanupResult result = new CleanupResult();
        
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (shouldRemove(entity, config, true, true, true, true)) {
                    if (entity instanceof ItemEntity) result.items++;
                    else if (entity instanceof ExperienceOrb) result.xpOrbs++;
                    else if (entity instanceof AbstractArrow) result.arrows++;
                    else result.mobs++;
                }
            }
        }
        
        return result;
    }
    
    private boolean shouldRemove(Entity entity, EntityCleanerConfig config, 
                                  boolean items, boolean mobs, boolean xp, boolean arrows) {
        // Check entity whitelist first
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (config.getEntityWhitelist().contains(entityId.toString())) {
            return false;
        }

        if (config.isProtectBosses() && isProtectedBoss(entity, entityId)) {
            return false;
        }
        
        // Check named entity protection
        if (!config.isRemoveNamedEntities() && entity.hasCustomName()) {
            return false;
        }
        
        // Dropped items
        if (entity instanceof ItemEntity itemEntity) {
            if (!items || !config.isRemoveDroppedItems()) return false;
            
            // Check item whitelist
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
            if (config.getItemWhitelist().contains(itemId.toString())) {
                return false;
            }
            return true;
        }
        
        // XP orbs
        if (entity instanceof ExperienceOrb) {
            return xp && config.isRemoveXpOrbs();
        }
        
        // Arrows
        if (entity instanceof AbstractArrow arrow) {
            // Only remove arrows that are not moving (stuck in ground or walls)
            return arrows && config.isRemoveArrows() && !arrow.isNoGravity() && arrow.getDeltaMovement().lengthSqr() < 0.01;
        }
        
        // Tamed animals protection
        if (entity instanceof TamableAnimal tamable) {
            if (!config.isRemoveTamedAnimals() && tamable.isTame()) {
                return false;
            }
        }
        
        // Hostile mobs
        if (entity instanceof Monster) {
            return mobs && config.isRemoveHostileMobs();
        }
        
        // Passive mobs (animals)
        if (entity instanceof Animal) {
            return mobs && config.isRemovePassiveMobs();
        }
        
        return false;
    }

    private boolean isProtectedBoss(Entity entity, ResourceLocation entityId) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }
        String path = entityId != null ? entityId.getPath().toLowerCase(java.util.Locale.ROOT) : "";
        String className = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("boss") || className.contains("boss")) {
            return true;
        }
        return hasBossBarField(entity.getClass(), entity);
    }

    private boolean hasBossBarField(Class<?> type, Entity entity) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!BossEvent.class.isAssignableFrom(field.getType()) && !ServerBossEvent.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(entity) != null) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
    
    /**
     * Force immediate cleanup with specified types.
     */
    public CleanupResult forceCleanup(boolean items, boolean mobs, boolean xp, boolean arrows) {
        CleanupResult result = cleanEntities(items, mobs, xp, arrows);
        // Reset timer
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        if (config != null) {
            secondsUntilCleanup = config.getCleanupIntervalSeconds();
        }
        return result;
    }
    
    public int getSecondsUntilCleanup() {
        return secondsUntilCleanup;
    }

    /**
     * Per-dimension preview of entities that would be removed.
     * Returns map: dimension path (e.g. "overworld","the_nether","the_end") -> CleanupResult.
     */
    public java.util.Map<String, CleanupResult> previewCleanupByDimension() {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        java.util.Map<String, CleanupResult> map = new java.util.LinkedHashMap<>();

        for (ServerLevel level : server.getAllLevels()) {
            String dim = level.dimension().location().getPath(); // e.g. overworld, the_nether, the_end
            CleanupResult result = new CleanupResult();
            for (Entity entity : level.getAllEntities()) {
                if (shouldRemove(entity, config, true, true, true, true)) {
                    if (entity instanceof ItemEntity) result.items++;
                    else if (entity instanceof ExperienceOrb) result.xpOrbs++;
                    else if (entity instanceof AbstractArrow) result.arrows++;
                    else result.mobs++;
                }
            }
            map.put(dim, result);
        }
        return map;
    }

    /**
     * Result of entity cleanup operation.
     */
    public static class CleanupResult {
        public int items = 0;
        public int mobs = 0;
        public int xpOrbs = 0;
        public int arrows = 0;
        
        public int total() {
            return items + mobs + xpOrbs + arrows;
        }
        
        @Override
        public String toString() {
            return String.format("items=%d, mobs=%d, xp=%d, arrows=%d", items, mobs, xpOrbs, arrows);
        }
    }
}
