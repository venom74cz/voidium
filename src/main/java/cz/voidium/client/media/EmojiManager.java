package cz.voidium.client.media;

import cz.voidium.Voidium;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Discord emoji textures.
 * Emojis are synced from server and rendered as inline textures.
 * Also includes built-in fallback emoji for when server doesn't send them.
 */
public class EmojiManager {
    private static EmojiManager instance;

    // Map of emoji name -> texture ResourceLocation
    private final Map<String, ResourceLocation> emojiTextures = new ConcurrentHashMap<>();
    // Map of emoji name -> URL (synced from server or built-in)
    private final Map<String, String> emojiUrls = new ConcurrentHashMap<>();
    // Loading state
    private final Map<String, Boolean> loading = new ConcurrentHashMap<>();
    // Whether we received server sync
    private boolean receivedServerSync = false;

    private EmojiManager() {
        // Load built-in emoji URLs
        loadBuiltInEmojis();
        // Pre-load most popular emoji textures immediately
        preloadPopularEmojis();
    }

    public static synchronized EmojiManager getInstance() {
        if (instance == null) {
            instance = new EmojiManager();
        }
        return instance;
    }

    /**
     * Called when receiving emoji sync packet from server.
     */
    public void syncEmojis(Map<String, String> emojis) {
        System.out.println("[Voidium-Client] Received emoji sync: " + emojis.size() + " emojis");
        receivedServerSync = true;
        // Merge with existing (server emojis override built-in)
        this.emojiUrls.putAll(emojis);

        // Pre-load all emojis (both custom and standard provided by server)
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            loadEmoji(entry.getKey(), entry.getValue());
        }
        System.out.println("[Voidium-Client] Started loading emojis, first 5: " +
                emojis.keySet().stream().limit(5).toList());
    }

    /**
     * Get the texture for an emoji by name.
     * Returns null if not loaded yet.
     */
    public ResourceLocation getEmojiTexture(String name) {
        // If not loaded but we have URL, try load (lazy load)
        if (!emojiTextures.containsKey(name) && emojiUrls.containsKey(name)) {
            loadEmoji(name, emojiUrls.get(name));
        }
        return emojiTextures.get(name);
    }

    /**
     * Check if emoji exists (was synced from server or is built-in).
     */
    public boolean hasEmoji(String name) {
        return emojiUrls.containsKey(name);
    }

    /**
     * Get list of all available emoji names for autocomplete.
     */
    public java.util.Set<String> getEmojiNames() {
        return emojiUrls.keySet();
    }

    private void loadEmoji(String name, String urlString) {
        if (loading.containsKey(name) || emojiTextures.containsKey(name))
            return;

        loading.put(name, true);

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Voidium-Mod");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (InputStream stream = conn.getInputStream()) {
                    NativeImage image = NativeImage.read(stream);

                    Minecraft.getInstance().execute(() -> {
                        DynamicTexture texture = new DynamicTexture(image);
                        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "emoji/" + name);
                        Minecraft.getInstance().getTextureManager().register(rl, texture);
                        emojiTextures.put(name, rl);
                        loading.remove(name);
                    });
                }
            } catch (Exception e) {
                loading.remove(name);
            }
        });
    }

    /**
     * Pre-load the most commonly used emoji textures so they're ready instantly.
     */
    private void preloadPopularEmojis() {
        String[] popular = { "eyes", "heart", "fire", "100", "skull", "joy", "sob", "thinking",
                "thumbsup", "+1", "clap", "wave", "smile", "grin", "sunglasses", "rocket",
                "star", "sparkles", "check", "x", "warning", "tada", "gift", "crown", "trophy" };

        for (String name : popular) {
            if (emojiUrls.containsKey(name)) {
                loadEmoji(name, emojiUrls.get(name));
            }
        }
    }

    /**
     * Get emoji size for rendering (Discord emojis are typically 22x22 or 32x32).
     */
    public int getEmojiSize() {
        return 9; // Reduce to match standard font height (was 12)
    }

    /**
     * Load built-in emoji so they work without server sync.
     */
    private void loadBuiltInEmojis() {
        String cdn = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/";

        // Smileys & Emotion - most popular
        emojiUrls.put("grinning", cdn + "1f600.png");
        emojiUrls.put("smile", cdn + "1f604.png");
        emojiUrls.put("grin", cdn + "1f601.png");
        emojiUrls.put("laughing", cdn + "1f606.png");
        emojiUrls.put("sweat_smile", cdn + "1f605.png");
        emojiUrls.put("rofl", cdn + "1f923.png");
        emojiUrls.put("joy", cdn + "1f602.png");
        emojiUrls.put("slightly_smiling_face", cdn + "1f642.png");
        emojiUrls.put("upside_down_face", cdn + "1f643.png");
        emojiUrls.put("wink", cdn + "1f609.png");
        emojiUrls.put("blush", cdn + "1f60a.png");
        emojiUrls.put("innocent", cdn + "1f607.png");
        emojiUrls.put("heart_eyes", cdn + "1f60d.png");
        emojiUrls.put("star_struck", cdn + "1f929.png");
        emojiUrls.put("kissing_heart", cdn + "1f618.png");
        emojiUrls.put("kissing", cdn + "1f617.png");
        emojiUrls.put("yum", cdn + "1f60b.png");
        emojiUrls.put("stuck_out_tongue", cdn + "1f61b.png");
        emojiUrls.put("stuck_out_tongue_winking_eye", cdn + "1f61c.png");
        emojiUrls.put("zany_face", cdn + "1f92a.png");
        emojiUrls.put("thinking", cdn + "1f914.png");
        emojiUrls.put("thinking_face", cdn + "1f914.png");
        emojiUrls.put("neutral_face", cdn + "1f610.png");
        emojiUrls.put("expressionless", cdn + "1f611.png");
        emojiUrls.put("no_mouth", cdn + "1f636.png");
        emojiUrls.put("smirk", cdn + "1f60f.png");
        emojiUrls.put("unamused", cdn + "1f612.png");
        emojiUrls.put("roll_eyes", cdn + "1f644.png");
        emojiUrls.put("grimacing", cdn + "1f62c.png");
        emojiUrls.put("relieved", cdn + "1f60c.png");
        emojiUrls.put("pensive", cdn + "1f614.png");
        emojiUrls.put("sleepy", cdn + "1f62a.png");
        emojiUrls.put("sleeping", cdn + "1f634.png");
        emojiUrls.put("mask", cdn + "1f637.png");
        emojiUrls.put("face_with_thermometer", cdn + "1f912.png");
        emojiUrls.put("nauseated_face", cdn + "1f922.png");
        emojiUrls.put("sneezing_face", cdn + "1f927.png");
        emojiUrls.put("hot_face", cdn + "1f975.png");
        emojiUrls.put("cold_face", cdn + "1f976.png");
        emojiUrls.put("woozy_face", cdn + "1f974.png");
        emojiUrls.put("dizzy_face", cdn + "1f635.png");
        emojiUrls.put("exploding_head", cdn + "1f92f.png");
        emojiUrls.put("cowboy_hat_face", cdn + "1f920.png");
        emojiUrls.put("partying_face", cdn + "1f973.png");
        emojiUrls.put("sunglasses", cdn + "1f60e.png");
        emojiUrls.put("nerd_face", cdn + "1f913.png");
        emojiUrls.put("confused", cdn + "1f615.png");
        emojiUrls.put("worried", cdn + "1f61f.png");
        emojiUrls.put("frowning_face", cdn + "2639.png");
        emojiUrls.put("open_mouth", cdn + "1f62e.png");
        emojiUrls.put("hushed", cdn + "1f62f.png");
        emojiUrls.put("astonished", cdn + "1f632.png");
        emojiUrls.put("flushed", cdn + "1f633.png");
        emojiUrls.put("pleading_face", cdn + "1f97a.png");
        emojiUrls.put("cry", cdn + "1f622.png");
        emojiUrls.put("sob", cdn + "1f62d.png");
        emojiUrls.put("scream", cdn + "1f631.png");
        emojiUrls.put("confounded", cdn + "1f616.png");
        emojiUrls.put("persevere", cdn + "1f623.png");
        emojiUrls.put("disappointed", cdn + "1f61e.png");
        emojiUrls.put("sweat", cdn + "1f613.png");
        emojiUrls.put("weary", cdn + "1f629.png");
        emojiUrls.put("tired_face", cdn + "1f62b.png");
        emojiUrls.put("yawning_face", cdn + "1f971.png");
        emojiUrls.put("triumph", cdn + "1f624.png");
        emojiUrls.put("rage", cdn + "1f621.png");
        emojiUrls.put("pout", cdn + "1f621.png");
        emojiUrls.put("angry", cdn + "1f620.png");
        emojiUrls.put("cursing_face", cdn + "1f92c.png");
        emojiUrls.put("smiling_imp", cdn + "1f608.png");
        emojiUrls.put("imp", cdn + "1f47f.png");
        emojiUrls.put("skull", cdn + "1f480.png");
        emojiUrls.put("skull_and_crossbones", cdn + "2620.png");
        emojiUrls.put("poop", cdn + "1f4a9.png");
        emojiUrls.put("clown_face", cdn + "1f921.png");
        emojiUrls.put("ghost", cdn + "1f47b.png");
        emojiUrls.put("alien", cdn + "1f47d.png");
        emojiUrls.put("robot", cdn + "1f916.png");
        emojiUrls.put("smiley_cat", cdn + "1f63a.png");
        emojiUrls.put("see_no_evil", cdn + "1f648.png");
        emojiUrls.put("hear_no_evil", cdn + "1f649.png");
        emojiUrls.put("speak_no_evil", cdn + "1f64a.png");

        // Hearts & Love
        emojiUrls.put("kiss", cdn + "1f48b.png");
        emojiUrls.put("love_letter", cdn + "1f48c.png");
        emojiUrls.put("cupid", cdn + "1f498.png");
        emojiUrls.put("gift_heart", cdn + "1f49d.png");
        emojiUrls.put("sparkling_heart", cdn + "1f496.png");
        emojiUrls.put("heartpulse", cdn + "1f497.png");
        emojiUrls.put("heartbeat", cdn + "1f493.png");
        emojiUrls.put("revolving_hearts", cdn + "1f49e.png");
        emojiUrls.put("two_hearts", cdn + "1f495.png");
        emojiUrls.put("broken_heart", cdn + "1f494.png");
        emojiUrls.put("heart", cdn + "2764.png");
        emojiUrls.put("red_heart", cdn + "2764.png");
        emojiUrls.put("orange_heart", cdn + "1f9e1.png");
        emojiUrls.put("yellow_heart", cdn + "1f49b.png");
        emojiUrls.put("green_heart", cdn + "1f49a.png");
        emojiUrls.put("blue_heart", cdn + "1f499.png");
        emojiUrls.put("purple_heart", cdn + "1f49c.png");
        emojiUrls.put("black_heart", cdn + "1f5a4.png");
        emojiUrls.put("white_heart", cdn + "1f90d.png");
        emojiUrls.put("100", cdn + "1f4af.png");
        emojiUrls.put("anger", cdn + "1f4a2.png");
        emojiUrls.put("boom", cdn + "1f4a5.png");
        emojiUrls.put("dizzy", cdn + "1f4ab.png");
        emojiUrls.put("sweat_drops", cdn + "1f4a6.png");
        emojiUrls.put("dash", cdn + "1f4a8.png");
        emojiUrls.put("bomb", cdn + "1f4a3.png");
        emojiUrls.put("zzz", cdn + "1f4a4.png");

        // Hands
        emojiUrls.put("wave", cdn + "1f44b.png");
        emojiUrls.put("hand", cdn + "270b.png");
        emojiUrls.put("raised_hand", cdn + "270b.png");
        emojiUrls.put("ok_hand", cdn + "1f44c.png");
        emojiUrls.put("pinched_fingers", cdn + "1f90c.png");
        emojiUrls.put("v", cdn + "270c.png");
        emojiUrls.put("crossed_fingers", cdn + "1f91e.png");
        emojiUrls.put("love_you_gesture", cdn + "1f91f.png");
        emojiUrls.put("metal", cdn + "1f918.png");
        emojiUrls.put("call_me_hand", cdn + "1f919.png");
        emojiUrls.put("point_left", cdn + "1f448.png");
        emojiUrls.put("point_right", cdn + "1f449.png");
        emojiUrls.put("point_up_2", cdn + "1f446.png");
        emojiUrls.put("middle_finger", cdn + "1f595.png");
        emojiUrls.put("point_down", cdn + "1f447.png");
        emojiUrls.put("point_up", cdn + "261d.png");
        emojiUrls.put("+1", cdn + "1f44d.png");
        emojiUrls.put("thumbsup", cdn + "1f44d.png");
        emojiUrls.put("thumbup", cdn + "1f44d.png");
        emojiUrls.put("-1", cdn + "1f44e.png");
        emojiUrls.put("thumbsdown", cdn + "1f44e.png");
        emojiUrls.put("fist", cdn + "270a.png");
        emojiUrls.put("facepunch", cdn + "1f44a.png");
        emojiUrls.put("punch", cdn + "1f44a.png");
        emojiUrls.put("clap", cdn + "1f44f.png");
        emojiUrls.put("raised_hands", cdn + "1f64c.png");
        emojiUrls.put("open_hands", cdn + "1f450.png");
        emojiUrls.put("handshake", cdn + "1f91d.png");
        emojiUrls.put("pray", cdn + "1f64f.png");
        emojiUrls.put("muscle", cdn + "1f4aa.png");
        emojiUrls.put("eyes", cdn + "1f440.png");
        emojiUrls.put("eye", cdn + "1f441.png");
        emojiUrls.put("tongue", cdn + "1f445.png");
        emojiUrls.put("lips", cdn + "1f444.png");

        // Animals
        emojiUrls.put("monkey_face", cdn + "1f435.png");
        emojiUrls.put("dog", cdn + "1f436.png");
        emojiUrls.put("wolf", cdn + "1f43a.png");
        emojiUrls.put("fox_face", cdn + "1f98a.png");
        emojiUrls.put("cat", cdn + "1f431.png");
        emojiUrls.put("lion", cdn + "1f981.png");
        emojiUrls.put("tiger", cdn + "1f42f.png");
        emojiUrls.put("horse", cdn + "1f434.png");
        emojiUrls.put("unicorn", cdn + "1f984.png");
        emojiUrls.put("cow", cdn + "1f42e.png");
        emojiUrls.put("pig", cdn + "1f437.png");
        emojiUrls.put("frog", cdn + "1f438.png");
        emojiUrls.put("monkey", cdn + "1f412.png");
        emojiUrls.put("chicken", cdn + "1f414.png");
        emojiUrls.put("penguin", cdn + "1f427.png");
        emojiUrls.put("bird", cdn + "1f426.png");
        emojiUrls.put("eagle", cdn + "1f985.png");
        emojiUrls.put("duck", cdn + "1f986.png");
        emojiUrls.put("owl", cdn + "1f989.png");
        emojiUrls.put("bat", cdn + "1f987.png");
        emojiUrls.put("shark", cdn + "1f988.png");
        emojiUrls.put("snake", cdn + "1f40d.png");
        emojiUrls.put("spider", cdn + "1f577.png");
        emojiUrls.put("scorpion", cdn + "1f982.png");
        emojiUrls.put("crab", cdn + "1f980.png");
        emojiUrls.put("butterfly", cdn + "1f98b.png");
        emojiUrls.put("bee", cdn + "1f41d.png");
        emojiUrls.put("bug", cdn + "1f41b.png");
        emojiUrls.put("ant", cdn + "1f41c.png");
        emojiUrls.put("dragon", cdn + "1f409.png");
        emojiUrls.put("dragon_face", cdn + "1f432.png");
        emojiUrls.put("turtle", cdn + "1f422.png");
        emojiUrls.put("crocodile", cdn + "1f40a.png");
        emojiUrls.put("whale", cdn + "1f433.png");
        emojiUrls.put("dolphin", cdn + "1f42c.png");
        emojiUrls.put("fish", cdn + "1f41f.png");
        emojiUrls.put("octopus", cdn + "1f419.png");
        emojiUrls.put("bear", cdn + "1f43b.png");
        emojiUrls.put("panda_face", cdn + "1f43c.png");
        emojiUrls.put("koala", cdn + "1f428.png");
        emojiUrls.put("rabbit", cdn + "1f430.png");
        emojiUrls.put("mouse", cdn + "1f42d.png");
        emojiUrls.put("hamster", cdn + "1f439.png");
        emojiUrls.put("elephant", cdn + "1f418.png");
        emojiUrls.put("gorilla", cdn + "1f98d.png");

        // Food
        emojiUrls.put("apple", cdn + "1f34e.png");
        emojiUrls.put("banana", cdn + "1f34c.png");
        emojiUrls.put("watermelon", cdn + "1f349.png");
        emojiUrls.put("grapes", cdn + "1f347.png");
        emojiUrls.put("strawberry", cdn + "1f353.png");
        emojiUrls.put("peach", cdn + "1f351.png");
        emojiUrls.put("cherries", cdn + "1f352.png");
        emojiUrls.put("pineapple", cdn + "1f34d.png");
        emojiUrls.put("avocado", cdn + "1f951.png");
        emojiUrls.put("eggplant", cdn + "1f346.png");
        emojiUrls.put("carrot", cdn + "1f955.png");
        emojiUrls.put("corn", cdn + "1f33d.png");
        emojiUrls.put("pizza", cdn + "1f355.png");
        emojiUrls.put("hamburger", cdn + "1f354.png");
        emojiUrls.put("fries", cdn + "1f35f.png");
        emojiUrls.put("hotdog", cdn + "1f32d.png");
        emojiUrls.put("taco", cdn + "1f32e.png");
        emojiUrls.put("burrito", cdn + "1f32f.png");
        emojiUrls.put("sushi", cdn + "1f363.png");
        emojiUrls.put("ramen", cdn + "1f35c.png");
        emojiUrls.put("cookie", cdn + "1f36a.png");
        emojiUrls.put("cake", cdn + "1f370.png");
        emojiUrls.put("birthday", cdn + "1f382.png");
        emojiUrls.put("ice_cream", cdn + "1f368.png");
        emojiUrls.put("doughnut", cdn + "1f369.png");
        emojiUrls.put("chocolate_bar", cdn + "1f36b.png");
        emojiUrls.put("candy", cdn + "1f36c.png");
        emojiUrls.put("coffee", cdn + "2615.png");
        emojiUrls.put("tea", cdn + "1f375.png");
        emojiUrls.put("beer", cdn + "1f37a.png");
        emojiUrls.put("beers", cdn + "1f37b.png");
        emojiUrls.put("wine_glass", cdn + "1f377.png");
        emojiUrls.put("cocktail", cdn + "1f378.png");
        emojiUrls.put("champagne", cdn + "1f37e.png");

        // Objects & Symbols
        emojiUrls.put("fire", cdn + "1f525.png");
        emojiUrls.put("star", cdn + "2b50.png");
        emojiUrls.put("star2", cdn + "1f31f.png");
        emojiUrls.put("sparkles", cdn + "2728.png");
        emojiUrls.put("zap", cdn + "26a1.png");
        emojiUrls.put("sunny", cdn + "2600.png");
        emojiUrls.put("cloud", cdn + "2601.png");
        emojiUrls.put("rainbow", cdn + "1f308.png");
        emojiUrls.put("snowflake", cdn + "2744.png");
        emojiUrls.put("snowman", cdn + "26c4.png");
        emojiUrls.put("moon", cdn + "1f319.png");
        emojiUrls.put("full_moon", cdn + "1f315.png");
        emojiUrls.put("sun_with_face", cdn + "1f31e.png");
        emojiUrls.put("rocket", cdn + "1f680.png");
        emojiUrls.put("airplane", cdn + "2708.png");
        emojiUrls.put("car", cdn + "1f697.png");
        emojiUrls.put("bike", cdn + "1f6b2.png");
        emojiUrls.put("trophy", cdn + "1f3c6.png");
        emojiUrls.put("medal", cdn + "1f3c5.png");
        emojiUrls.put("first_place_medal", cdn + "1f947.png");
        emojiUrls.put("crown", cdn + "1f451.png");
        emojiUrls.put("gem", cdn + "1f48e.png");
        emojiUrls.put("bell", cdn + "1f514.png");
        emojiUrls.put("gift", cdn + "1f381.png");
        emojiUrls.put("balloon", cdn + "1f388.png");
        emojiUrls.put("tada", cdn + "1f389.png");
        emojiUrls.put("confetti_ball", cdn + "1f38a.png");
        emojiUrls.put("video_game", cdn + "1f3ae.png");
        emojiUrls.put("joystick", cdn + "1f579.png");
        emojiUrls.put("computer", cdn + "1f4bb.png");
        emojiUrls.put("keyboard", cdn + "2328.png");
        emojiUrls.put("phone", cdn + "260e.png");
        emojiUrls.put("mobile_phone", cdn + "1f4f1.png");
        emojiUrls.put("camera", cdn + "1f4f7.png");
        emojiUrls.put("tv", cdn + "1f4fa.png");
        emojiUrls.put("guitar", cdn + "1f3b8.png");
        emojiUrls.put("microphone", cdn + "1f3a4.png");
        emojiUrls.put("headphones", cdn + "1f3a7.png");
        emojiUrls.put("musical_note", cdn + "1f3b5.png");
        emojiUrls.put("notes", cdn + "1f3b6.png");
        emojiUrls.put("money_with_wings", cdn + "1f4b8.png");
        emojiUrls.put("dollar", cdn + "1f4b5.png");
        emojiUrls.put("moneybag", cdn + "1f4b0.png");
        emojiUrls.put("credit_card", cdn + "1f4b3.png");
        emojiUrls.put("hammer", cdn + "1f528.png");
        emojiUrls.put("wrench", cdn + "1f527.png");
        emojiUrls.put("gear", cdn + "2699.png");
        emojiUrls.put("gun", cdn + "1f52b.png");
        emojiUrls.put("bomb", cdn + "1f4a3.png");
        emojiUrls.put("knife", cdn + "1f52a.png");
        emojiUrls.put("crossed_swords", cdn + "2694.png");
        emojiUrls.put("shield", cdn + "1f6e1.png");
        emojiUrls.put("key", cdn + "1f511.png");
        emojiUrls.put("lock", cdn + "1f512.png");
        emojiUrls.put("unlock", cdn + "1f513.png");
        emojiUrls.put("door", cdn + "1f6aa.png");
        emojiUrls.put("bulb", cdn + "1f4a1.png");
        emojiUrls.put("book", cdn + "1f4d6.png");
        emojiUrls.put("pencil", cdn + "270f.png");
        emojiUrls.put("memo", cdn + "1f4dd.png");
        emojiUrls.put("warning", cdn + "26a0.png");
        emojiUrls.put("no_entry", cdn + "26d4.png");
        emojiUrls.put("stop_sign", cdn + "1f6d1.png");
        emojiUrls.put("check", cdn + "2705.png");
        emojiUrls.put("white_check_mark", cdn + "2705.png");
        emojiUrls.put("x", cdn + "274c.png");
        emojiUrls.put("cross_mark", cdn + "274c.png");
        emojiUrls.put("question", cdn + "2753.png");
        emojiUrls.put("exclamation", cdn + "2757.png");
        emojiUrls.put("arrow_up", cdn + "2b06.png");
        emojiUrls.put("arrow_down", cdn + "2b07.png");
        emojiUrls.put("arrow_left", cdn + "2b05.png");
        emojiUrls.put("arrow_right", cdn + "27a1.png");
        emojiUrls.put("link", cdn + "1f517.png");

        System.out.println("[Voidium-Client] Loaded " + emojiUrls.size() + " built-in emojis");
    }
}
