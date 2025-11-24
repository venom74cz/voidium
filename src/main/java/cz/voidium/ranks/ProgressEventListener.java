package cz.voidium.ranks;

import cz.voidium.config.RanksConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for tracking custom rank conditions
 */
public class ProgressEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Progress");
    
    // Track visited biomes per player (session-based, UUID -> set of biome names)
    private static final ConcurrentHashMap<UUID, Set<String>> visitedBiomes = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!RanksConfig.getInstance().isEnableAutoRanks()) return;
        if (event.getEntity().level().isClientSide()) return;
        
        // Check biome every 20 ticks (1 second) to avoid spam
        if (event.getEntity().tickCount % 20 != 0) return;
        
        var player = event.getEntity();
        var biome = player.level().getBiome(player.blockPosition());
        
        // Get biome resource location
        var biomeKey = player.level().registryAccess()
            .registryOrThrow(Registries.BIOME)
            .getKey(biome.value());
        
        if (biomeKey != null) {
            String biomeName = biomeKey.toString();
            UUID uuid = player.getUUID();
            
            // Check if this is a new biome for this session
            Set<String> visited = visitedBiomes.computeIfAbsent(uuid, k -> new HashSet<>());
            if (!visited.contains(biomeName)) {
                visited.add(biomeName);
                
                // Increment progress
                ProgressTracker.getInstance().incrementProgress(
                    uuid.toString(), 
                    "VISIT_BIOMES", 
                    biomeName, 
                    1
                );
                
                // Auto-save periodically
                if (visited.size() % 5 == 0) {
                    ProgressTracker.getInstance().save();
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        if (!RanksConfig.getInstance().isEnableAutoRanks()) return;
        if (event.getEntity().level().isClientSide()) return;
        
        // Check if killed by a player
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            LivingEntity killed = event.getEntity();
            
            // Get entity type
            String entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(killed.getType())
                .toString();
            
            // Increment progress
            ProgressTracker.getInstance().incrementProgress(
                player.getUUID().toString(),
                "KILL_MOBS",
                entityType,
                1
            );
            
            // Auto-save every 10 kills
            if (player.tickCount % 200 == 0) {
                ProgressTracker.getInstance().save();
            }
        }
    }
    
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!RanksConfig.getInstance().isEnableAutoRanks()) return;
        if (event.getPlayer().level().isClientSide()) return;
        
        var player = event.getPlayer();
        var block = event.getState().getBlock();
        
        // Get block resource location
        String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(block)
            .toString();
        
        // Increment progress
        ProgressTracker.getInstance().incrementProgress(
            player.getUUID().toString(),
            "BREAK_BLOCKS",
            blockName,
            1
        );
    }
    
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!RanksConfig.getInstance().isEnableAutoRanks()) return;
        if (event.getEntity().level().isClientSide()) return;
        
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            var block = event.getPlacedBlock().getBlock();
            
            // Get block resource location
            String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(block)
                .toString();
            
            // Increment progress
            ProgressTracker.getInstance().incrementProgress(
                player.getUUID().toString(),
                "PLACE_BLOCKS",
                blockName,
                1
            );
        }
    }
    
    /**
     * Clear session-based tracking when player leaves
     */
    public static void clearSession(UUID uuid) {
        visitedBiomes.remove(uuid);
    }
}
