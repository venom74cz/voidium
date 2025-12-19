package cz.voidium.client;

import cz.voidium.Voidium;
import cz.voidium.client.chat.ChatChannelManager;
import cz.voidium.client.ui.ModernChatOverlay;
import cz.voidium.client.ui.ModernChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Voidium.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    // Track last chat time for fade
    public static long lastChatTime = 0;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register GAME event handlers (not mod bus)
        NeoForge.EVENT_BUS.addListener(ClientSetup::onChatReceived);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onRenderGuiLayerPre);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Register our overlay INSTEAD of vanilla chat (not above)
        event.registerAbove(VanillaGuiLayers.CHAT, 
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "modern_chat"), 
            new ModernChatOverlay());
    }
    
    @SubscribeEvent
    public static void onRenderGuiLayerPre(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.CHAT.equals(event.getName())) {
            event.setCanceled(true); // Stop vanilla chat from rendering
        }
    }

    // This handler intercepts incoming chat messages
    public static void onChatReceived(ClientChatReceivedEvent event) {
        Component message = event.getMessage();
        
        // Update fade timer
        lastChatTime = System.currentTimeMillis();
        
        // Route to our ChatChannelManager
        ChatChannelManager.getInstance().addMessage("main", message);
        
        // Ticket Auto-Detection
        // Example: "Ticket #123 created" or "Ticket #123: Hello"
        String plain = message.getString();
        // Regex for "Ticket #123"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Ticket #(\\d+)").matcher(plain);
        if (m.find()) {
            String ticketId = "ticket-" + m.group(1);
            ChatChannelManager.getInstance().getOrCreateChannel(ticketId, "Ticket #" + m.group(1));
            // Optional: Auto-switch or notification sound
            // ChatChannelManager.getInstance().setActiveChannel(ticketId);
        }
        
        // Cancel the vanilla chat display - our overlay will show it
        event.setCanceled(true); 
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.ChatScreen 
            && !(event.getScreen() instanceof ModernChatScreen)) {
            event.setNewScreen(new ModernChatScreen());
        }
    }
}
