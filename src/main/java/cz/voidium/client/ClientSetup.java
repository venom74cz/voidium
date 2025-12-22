package cz.voidium.client;

import cz.voidium.Voidium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side setup for Voidium mod.
 * Vanilla chat is used with injected RGB/Emoji support via Mixins.
 */
@EventBusSubscriber(modid = Voidium.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Client");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Vanilla chat is now used directly
        // RGB colors and emoji support are handled via Mixins
        LOGGER.info("Voidium client initialized - using vanilla chat with enhanced features");
    }
}
