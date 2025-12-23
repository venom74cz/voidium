package cz.voidium.server.chat;

import cz.voidium.discord.DiscordManager;
import cz.voidium.network.PacketSyncEmojis;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages syncing Discord server emojis to Minecraft clients.
 */
public class EmojiSyncService {
    private static EmojiSyncService instance;
    private Map<String, String> cachedEmojis = new HashMap<>();
    private long lastFetch = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    private EmojiSyncService() {
    }

    public static synchronized EmojiSyncService getInstance() {
        if (instance == null) {
            instance = new EmojiSyncService();
        }
        return instance;
    }

    /**
     * Send emoji data to a player on login.
     */
    public void syncToPlayer(ServerPlayer player) {
        System.out.println("[Voidium] syncToPlayer called for: " + player.getName().getString());
        Map<String, String> emojis = getEmojis();
        System.out.println("[Voidium] Got " + emojis.size() + " emojis to sync");
        if (!emojis.isEmpty()) {
            try {
                PacketDistributor.sendToPlayer(player, new PacketSyncEmojis(emojis));
                System.out.println("[Voidium] Sent emoji sync packet to " + player.getName().getString());
            } catch (UnsupportedOperationException e) {
                System.out.println("[Voidium] Client doesn't have Voidium mod: " + player.getName().getString());
            }
        } else {
            System.out.println("[Voidium] No emojis to sync!");
        }
    }

    /**
     * Get cached emojis, refreshing if stale.
     */
    public Map<String, String> getEmojis() {
        long now = System.currentTimeMillis();
        if (cachedEmojis.isEmpty() || (now - lastFetch) > CACHE_DURATION) {
            refreshEmojis();
        }
        return cachedEmojis;
    }

    /**
     * Fetch emojis from Discord guild.
     */
    private void refreshEmojis() {
        System.out.println("[Voidium] refreshEmojis called");
        try {
            var jda = DiscordManager.getInstance().getJda();
            if (jda == null) {
                System.out.println("[Voidium] JDA is null - Discord not connected!");
                addFallbackEmojis();
                return;
            }

            String guildId = cz.voidium.config.DiscordConfig.getInstance().getGuildId();
            System.out.println("[Voidium] Guild ID: " + guildId);

            var guild = jda.getGuildById(guildId);
            if (guild == null) {
                System.out.println("[Voidium] Guild is null for ID: " + guildId);
                addFallbackEmojis();
                return;
            }

            System.out.println("[Voidium] Found guild: " + guild.getName());
            Map<String, String> newEmojis = new HashMap<>();

            // Get all custom emojis from the guild
            var guildEmojis = guild.getEmojis();
            System.out.println("[Voidium] Guild has " + guildEmojis.size() + " custom emojis");
            for (var emoji : guildEmojis) {
                String name = emoji.getName();
                String url = emoji.getImageUrl();
                newEmojis.put(name, url);
            }

            // Add built-in standard Discord emojis
            addBuiltInEmojis(newEmojis);

            cachedEmojis = newEmojis;
            lastFetch = System.currentTimeMillis();

            System.out.println("[Voidium] Total emojis cached: " + newEmojis.size());
        } catch (Exception e) {
            System.err.println("[Voidium] Failed to sync Discord emojis: " + e.getMessage());
            e.printStackTrace();
            addFallbackEmojis();
        }
    }

    /**
     * Add fallback emojis when Discord is unavailable.
     */
    private void addFallbackEmojis() {
        if (!cachedEmojis.isEmpty())
            return;

        System.out.println("[Voidium] Adding fallback emojis");
        String cdn = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/";
        cachedEmojis.put("smile", cdn + "1f604.png");
        cachedEmojis.put("heart", cdn + "2764.png");
        cachedEmojis.put("eyes", cdn + "1f440.png");
        cachedEmojis.put("thumbsup", cdn + "1f44d.png");
        cachedEmojis.put("+1", cdn + "1f44d.png");
        cachedEmojis.put("fire", cdn + "1f525.png");
        cachedEmojis.put("100", cdn + "1f4af.png");
        cachedEmojis.put("skull", cdn + "1f480.png");
        cachedEmojis.put("sob", cdn + "1f62d.png");
        cachedEmojis.put("joy", cdn + "1f602.png");
        cachedEmojis.put("thinking", cdn + "1f914.png");
        cachedEmojis.put("wave", cdn + "1f44b.png");
        cachedEmojis.put("clap", cdn + "1f44f.png");
        cachedEmojis.put("sunglasses", cdn + "1f60e.png");
        cachedEmojis.put("rocket", cdn + "1f680.png");
        cachedEmojis.put("star", cdn + "2b50.png");
        cachedEmojis.put("check", cdn + "2705.png");
        cachedEmojis.put("x", cdn + "274c.png");

        lastFetch = System.currentTimeMillis();
        System.out.println("[Voidium] Added " + cachedEmojis.size() + " fallback emojis");
    }

    /**
     * Add built-in standard Discord emoji mappings.
     * Uses jdecked/twemoji CDN (Discord's emoji source).
     */
    private void addBuiltInEmojis(Map<String, String> map) {
        String cdn = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/";

        // Smileys & Emotion
        map.put("grinning", cdn + "1f600.png");
        map.put("smile", cdn + "1f604.png");
        map.put("grin", cdn + "1f601.png");
        map.put("laughing", cdn + "1f606.png");
        map.put("satisfied", cdn + "1f606.png");
        map.put("sweat_smile", cdn + "1f605.png");
        map.put("rofl", cdn + "1f923.png");
        map.put("joy", cdn + "1f602.png");
        map.put("slightly_smiling_face", cdn + "1f642.png");
        map.put("upside_down_face", cdn + "1f643.png");
        map.put("wink", cdn + "1f609.png");
        map.put("blush", cdn + "1f60a.png");
        map.put("innocent", cdn + "1f607.png");
        map.put("smiling_face_with_3_hearts", cdn + "1f970.png");
        map.put("heart_eyes", cdn + "1f60d.png");
        map.put("star_struck", cdn + "1f929.png");
        map.put("kissing_heart", cdn + "1f618.png");
        map.put("kissing", cdn + "1f617.png");
        map.put("relaxed", cdn + "263a.png");
        map.put("kissing_closed_eyes", cdn + "1f61a.png");
        map.put("kissing_smiling_eyes", cdn + "1f619.png");
        map.put("smiling_face_with_tear", cdn + "1f972.png");
        map.put("yum", cdn + "1f60b.png");
        map.put("stuck_out_tongue", cdn + "1f61b.png");
        map.put("stuck_out_tongue_winking_eye", cdn + "1f61c.png");
        map.put("zany_face", cdn + "1f92a.png");
        map.put("stuck_out_tongue_closed_eyes", cdn + "1f61d.png");
        map.put("money_mouth_face", cdn + "1f911.png");
        map.put("hugs", cdn + "1f917.png");
        map.put("hand_over_mouth", cdn + "1f92d.png");
        map.put("shushing_face", cdn + "1f92b.png");
        map.put("thinking", cdn + "1f914.png");
        map.put("thinking_face", cdn + "1f914.png");
        map.put("zipper_mouth_face", cdn + "1f910.png");
        map.put("raised_eyebrow", cdn + "1f928.png");
        map.put("neutral_face", cdn + "1f610.png");
        map.put("expressionless", cdn + "1f611.png");
        map.put("no_mouth", cdn + "1f636.png");
        map.put("smirk", cdn + "1f60f.png");
        map.put("unamused", cdn + "1f612.png");
        map.put("roll_eyes", cdn + "1f644.png");
        map.put("grimacing", cdn + "1f62c.png");
        map.put("lying_face", cdn + "1f925.png");
        map.put("relieved", cdn + "1f60c.png");
        map.put("pensive", cdn + "1f614.png");
        map.put("sleepy", cdn + "1f62a.png");
        map.put("drooling_face", cdn + "1f924.png");
        map.put("sleeping", cdn + "1f634.png");
        map.put("mask", cdn + "1f637.png");
        map.put("face_with_thermometer", cdn + "1f912.png");
        map.put("face_with_head_bandage", cdn + "1f915.png");
        map.put("nauseated_face", cdn + "1f922.png");
        map.put("vomiting_face", cdn + "1f92e.png");
        map.put("sneezing_face", cdn + "1f927.png");
        map.put("hot_face", cdn + "1f975.png");
        map.put("cold_face", cdn + "1f976.png");
        map.put("woozy_face", cdn + "1f974.png");
        map.put("dizzy_face", cdn + "1f635.png");
        map.put("exploding_head", cdn + "1f92f.png");
        map.put("cowboy_hat_face", cdn + "1f920.png");
        map.put("partying_face", cdn + "1f973.png");
        map.put("disguised_face", cdn + "1f978.png");
        map.put("sunglasses", cdn + "1f60e.png");
        map.put("nerd_face", cdn + "1f913.png");
        map.put("monocle_face", cdn + "1f9d0.png");
        map.put("confused", cdn + "1f615.png");
        map.put("worried", cdn + "1f61f.png");
        map.put("slightly_frowning_face", cdn + "1f641.png");
        map.put("frowning_face", cdn + "2639.png");
        map.put("open_mouth", cdn + "1f62e.png");
        map.put("hushed", cdn + "1f62f.png");
        map.put("astonished", cdn + "1f632.png");
        map.put("flushed", cdn + "1f633.png");
        map.put("pleading_face", cdn + "1f97a.png");
        map.put("frowning", cdn + "1f626.png");
        map.put("anguished", cdn + "1f627.png");
        map.put("fearful", cdn + "1f628.png");
        map.put("cold_sweat", cdn + "1f630.png");
        map.put("disappointed_relieved", cdn + "1f625.png");
        map.put("cry", cdn + "1f622.png");
        map.put("sob", cdn + "1f62d.png");
        map.put("scream", cdn + "1f631.png");
        map.put("confounded", cdn + "1f616.png");
        map.put("persevere", cdn + "1f623.png");
        map.put("disappointed", cdn + "1f61e.png");
        map.put("sweat", cdn + "1f613.png");
        map.put("weary", cdn + "1f629.png");
        map.put("tired_face", cdn + "1f62b.png");
        map.put("yawning_face", cdn + "1f971.png");
        map.put("triumph", cdn + "1f624.png");
        map.put("rage", cdn + "1f621.png");
        map.put("pout", cdn + "1f621.png");
        map.put("angry", cdn + "1f620.png");
        map.put("cursing_face", cdn + "1f92c.png");
        map.put("smiling_imp", cdn + "1f608.png");
        map.put("imp", cdn + "1f47f.png");
        map.put("skull", cdn + "1f480.png");
        map.put("skull_and_crossbones", cdn + "2620.png");
        map.put("poop", cdn + "1f4a9.png");
        map.put("hankey", cdn + "1f4a9.png");
        map.put("clown_face", cdn + "1f921.png");
        map.put("japanese_ogre", cdn + "1f479.png");
        map.put("japanese_goblin", cdn + "1f47a.png");
        map.put("ghost", cdn + "1f47b.png");
        map.put("alien", cdn + "1f47d.png");
        map.put("space_invader", cdn + "1f47e.png");
        map.put("robot", cdn + "1f916.png");
        map.put("smiley_cat", cdn + "1f63a.png");
        map.put("smile_cat", cdn + "1f638.png");
        map.put("joy_cat", cdn + "1f639.png");
        map.put("heart_eyes_cat", cdn + "1f63b.png");
        map.put("smirk_cat", cdn + "1f63c.png");
        map.put("kissing_cat", cdn + "1f63d.png");
        map.put("scream_cat", cdn + "1f640.png");
        map.put("crying_cat_face", cdn + "1f63f.png");
        map.put("pouting_cat", cdn + "1f63e.png");
        map.put("see_no_evil", cdn + "1f648.png");
        map.put("hear_no_evil", cdn + "1f649.png");
        map.put("speak_no_evil", cdn + "1f64a.png");

        // Gestures & Body
        map.put("kiss", cdn + "1f48b.png");
        map.put("love_letter", cdn + "1f48c.png");
        map.put("cupid", cdn + "1f498.png");
        map.put("gift_heart", cdn + "1f49d.png");
        map.put("sparkling_heart", cdn + "1f496.png");
        map.put("heartpulse", cdn + "1f497.png");
        map.put("heartbeat", cdn + "1f493.png");
        map.put("revolving_hearts", cdn + "1f49e.png");
        map.put("two_hearts", cdn + "1f495.png");
        map.put("heart_decoration", cdn + "1f49f.png");
        map.put("heart_exclamation", cdn + "2763.png");
        map.put("broken_heart", cdn + "1f494.png");
        map.put("heart", cdn + "2764.png");
        map.put("red_heart", cdn + "2764.png");
        map.put("orange_heart", cdn + "1f9e1.png");
        map.put("yellow_heart", cdn + "1f49b.png");
        map.put("green_heart", cdn + "1f49a.png");
        map.put("blue_heart", cdn + "1f499.png");
        map.put("purple_heart", cdn + "1f49c.png");
        map.put("brown_heart", cdn + "1f90e.png");
        map.put("black_heart", cdn + "1f5a4.png");
        map.put("white_heart", cdn + "1f90d.png");
        map.put("100", cdn + "1f4af.png");
        map.put("anger", cdn + "1f4a2.png");
        map.put("boom", cdn + "1f4a5.png");
        map.put("collision", cdn + "1f4a5.png");
        map.put("dizzy", cdn + "1f4ab.png");
        map.put("sweat_drops", cdn + "1f4a6.png");
        map.put("dash", cdn + "1f4a8.png");
        map.put("hole", cdn + "1f573.png");
        map.put("bomb", cdn + "1f4a3.png");
        map.put("speech_balloon", cdn + "1f4ac.png");
        map.put("eye_speech_bubble", cdn + "1f441-200d-1f5e8.png");
        map.put("thought_balloon", cdn + "1f4ad.png");
        map.put("zzz", cdn + "1f4a4.png");

        // Hands
        map.put("wave", cdn + "1f44b.png");
        map.put("raised_back_of_hand", cdn + "1f91a.png");
        map.put("hand", cdn + "270b.png");
        map.put("raised_hand", cdn + "270b.png");
        map.put("vulcan_salute", cdn + "1f596.png");
        map.put("ok_hand", cdn + "1f44c.png");
        map.put("pinched_fingers", cdn + "1f90c.png");
        map.put("pinching_hand", cdn + "1f90f.png");
        map.put("v", cdn + "270c.png");
        map.put("crossed_fingers", cdn + "1f91e.png");
        map.put("love_you_gesture", cdn + "1f91f.png");
        map.put("metal", cdn + "1f918.png");
        map.put("call_me_hand", cdn + "1f919.png");
        map.put("point_left", cdn + "1f448.png");
        map.put("point_right", cdn + "1f449.png");
        map.put("point_up_2", cdn + "1f446.png");
        map.put("middle_finger", cdn + "1f595.png");
        map.put("point_down", cdn + "1f447.png");
        map.put("point_up", cdn + "261d.png");
        map.put("+1", cdn + "1f44d.png");
        map.put("thumbsup", cdn + "1f44d.png");
        map.put("thumbup", cdn + "1f44d.png");
        map.put("-1", cdn + "1f44e.png");
        map.put("thumbsdown", cdn + "1f44e.png");
        map.put("thumbdown", cdn + "1f44e.png");
        map.put("fist", cdn + "270a.png");
        map.put("fist_raised", cdn + "270a.png");
        map.put("facepunch", cdn + "1f44a.png");
        map.put("punch", cdn + "1f44a.png");
        map.put("fist_oncoming", cdn + "1f44a.png");
        map.put("fist_left", cdn + "1f91b.png");
        map.put("fist_right", cdn + "1f91c.png");
        map.put("clap", cdn + "1f44f.png");
        map.put("raised_hands", cdn + "1f64c.png");
        map.put("open_hands", cdn + "1f450.png");
        map.put("palms_up_together", cdn + "1f932.png");
        map.put("handshake", cdn + "1f91d.png");
        map.put("pray", cdn + "1f64f.png");
        map.put("writing_hand", cdn + "270d.png");
        map.put("nail_care", cdn + "1f485.png");
        map.put("selfie", cdn + "1f933.png");
        map.put("muscle", cdn + "1f4aa.png");
        map.put("mechanical_arm", cdn + "1f9be.png");
        map.put("leg", cdn + "1f9b5.png");
        map.put("mechanical_leg", cdn + "1f9bf.png");
        map.put("foot", cdn + "1f9b6.png");
        map.put("ear", cdn + "1f442.png");
        map.put("nose", cdn + "1f443.png");
        map.put("brain", cdn + "1f9e0.png");
        map.put("eyes", cdn + "1f440.png");
        map.put("eye", cdn + "1f441.png");
        map.put("tongue", cdn + "1f445.png");
        map.put("lips", cdn + "1f444.png");

        // Animals & Nature
        map.put("monkey_face", cdn + "1f435.png");
        map.put("monkey", cdn + "1f412.png");
        map.put("gorilla", cdn + "1f98d.png");
        map.put("orangutan", cdn + "1f9a7.png");
        map.put("dog", cdn + "1f436.png");
        map.put("dog2", cdn + "1f415.png");
        map.put("guide_dog", cdn + "1f9ae.png");
        map.put("poodle", cdn + "1f429.png");
        map.put("wolf", cdn + "1f43a.png");
        map.put("fox_face", cdn + "1f98a.png");
        map.put("raccoon", cdn + "1f99d.png");
        map.put("cat", cdn + "1f431.png");
        map.put("cat2", cdn + "1f408.png");
        map.put("lion", cdn + "1f981.png");
        map.put("tiger", cdn + "1f42f.png");
        map.put("tiger2", cdn + "1f405.png");
        map.put("leopard", cdn + "1f406.png");
        map.put("horse", cdn + "1f434.png");
        map.put("racehorse", cdn + "1f40e.png");
        map.put("unicorn", cdn + "1f984.png");
        map.put("zebra", cdn + "1f993.png");
        map.put("deer", cdn + "1f98c.png");
        map.put("cow", cdn + "1f42e.png");
        map.put("ox", cdn + "1f402.png");
        map.put("water_buffalo", cdn + "1f403.png");
        map.put("cow2", cdn + "1f404.png");
        map.put("pig", cdn + "1f437.png");
        map.put("pig2", cdn + "1f416.png");
        map.put("boar", cdn + "1f417.png");
        map.put("pig_nose", cdn + "1f43d.png");
        map.put("ram", cdn + "1f40f.png");
        map.put("sheep", cdn + "1f411.png");
        map.put("goat", cdn + "1f410.png");
        map.put("dromedary_camel", cdn + "1f42a.png");
        map.put("camel", cdn + "1f42b.png");
        map.put("llama", cdn + "1f999.png");
        map.put("giraffe", cdn + "1f992.png");
        map.put("elephant", cdn + "1f418.png");
        map.put("rhinoceros", cdn + "1f98f.png");
        map.put("hippopotamus", cdn + "1f99b.png");
        map.put("mouse", cdn + "1f42d.png");
        map.put("mouse2", cdn + "1f401.png");
        map.put("rat", cdn + "1f400.png");
        map.put("hamster", cdn + "1f439.png");
        map.put("rabbit", cdn + "1f430.png");
        map.put("rabbit2", cdn + "1f407.png");
        map.put("chipmunk", cdn + "1f43f.png");
        map.put("hedgehog", cdn + "1f994.png");
        map.put("bat", cdn + "1f987.png");
        map.put("bear", cdn + "1f43b.png");
        map.put("koala", cdn + "1f428.png");
        map.put("panda_face", cdn + "1f43c.png");
        map.put("sloth", cdn + "1f9a5.png");
        map.put("otter", cdn + "1f9a6.png");
        map.put("skunk", cdn + "1f9a8.png");
        map.put("kangaroo", cdn + "1f998.png");
        map.put("badger", cdn + "1f9a1.png");
        map.put("feet", cdn + "1f43e.png");
        map.put("paw_prints", cdn + "1f43e.png");
        map.put("turkey", cdn + "1f983.png");
        map.put("chicken", cdn + "1f414.png");
        map.put("rooster", cdn + "1f413.png");
        map.put("hatching_chick", cdn + "1f423.png");
        map.put("baby_chick", cdn + "1f424.png");
        map.put("hatched_chick", cdn + "1f425.png");
        map.put("bird", cdn + "1f426.png");
        map.put("penguin", cdn + "1f427.png");
        map.put("dove", cdn + "1f54a.png");
        map.put("eagle", cdn + "1f985.png");
        map.put("duck", cdn + "1f986.png");
        map.put("swan", cdn + "1f9a2.png");
        map.put("owl", cdn + "1f989.png");
        map.put("flamingo", cdn + "1f9a9.png");
        map.put("peacock", cdn + "1f99a.png");
        map.put("parrot", cdn + "1f99c.png");
        map.put("frog", cdn + "1f438.png");
        map.put("crocodile", cdn + "1f40a.png");
        map.put("turtle", cdn + "1f422.png");
        map.put("lizard", cdn + "1f98e.png");
        map.put("snake", cdn + "1f40d.png");
        map.put("dragon_face", cdn + "1f432.png");
        map.put("dragon", cdn + "1f409.png");
        map.put("sauropod", cdn + "1f995.png");
        map.put("t_rex", cdn + "1f996.png");
        map.put("whale", cdn + "1f433.png");
        map.put("whale2", cdn + "1f40b.png");
        map.put("dolphin", cdn + "1f42c.png");
        map.put("fish", cdn + "1f41f.png");
        map.put("tropical_fish", cdn + "1f420.png");
        map.put("blowfish", cdn + "1f421.png");
        map.put("shark", cdn + "1f988.png");
        map.put("octopus", cdn + "1f419.png");
        map.put("shell", cdn + "1f41a.png");
        map.put("snail", cdn + "1f40c.png");
        map.put("butterfly", cdn + "1f98b.png");
        map.put("bug", cdn + "1f41b.png");
        map.put("ant", cdn + "1f41c.png");
        map.put("bee", cdn + "1f41d.png");
        map.put("honeybee", cdn + "1f41d.png");
        map.put("beetle", cdn + "1fab2.png");
        map.put("lady_beetle", cdn + "1f41e.png");
        map.put("cricket", cdn + "1f997.png");
        map.put("spider", cdn + "1f577.png");
        map.put("spider_web", cdn + "1f578.png");
        map.put("scorpion", cdn + "1f982.png");
        map.put("mosquito", cdn + "1f99f.png");
        map.put("microbe", cdn + "1f9a0.png");

        // Food & Drink
        map.put("apple", cdn + "1f34e.png");
        map.put("green_apple", cdn + "1f34f.png");
        map.put("pear", cdn + "1f350.png");
        map.put("tangerine", cdn + "1f34a.png");
        map.put("orange", cdn + "1f34a.png");
        map.put("lemon", cdn + "1f34b.png");
        map.put("banana", cdn + "1f34c.png");
        map.put("watermelon", cdn + "1f349.png");
        map.put("grapes", cdn + "1f347.png");
        map.put("strawberry", cdn + "1f353.png");
        map.put("melon", cdn + "1f348.png");
        map.put("cherries", cdn + "1f352.png");
        map.put("peach", cdn + "1f351.png");
        map.put("mango", cdn + "1f96d.png");
        map.put("pineapple", cdn + "1f34d.png");
        map.put("coconut", cdn + "1f965.png");
        map.put("kiwi_fruit", cdn + "1f95d.png");
        map.put("tomato", cdn + "1f345.png");
        map.put("eggplant", cdn + "1f346.png");
        map.put("avocado", cdn + "1f951.png");
        map.put("broccoli", cdn + "1f966.png");
        map.put("carrot", cdn + "1f955.png");
        map.put("corn", cdn + "1f33d.png");
        map.put("hot_pepper", cdn + "1f336.png");
        map.put("cucumber", cdn + "1f952.png");
        map.put("leafy_green", cdn + "1f96c.png");
        map.put("potato", cdn + "1f954.png");
        map.put("sweet_potato", cdn + "1f360.png");
        map.put("chestnut", cdn + "1f330.png");
        map.put("peanuts", cdn + "1f95c.png");
        map.put("bread", cdn + "1f35e.png");
        map.put("croissant", cdn + "1f950.png");
        map.put("baguette_bread", cdn + "1f956.png");
        map.put("pretzel", cdn + "1f968.png");
        map.put("bagel", cdn + "1f96f.png");
        map.put("pancakes", cdn + "1f95e.png");
        map.put("waffle", cdn + "1f9c7.png");
        map.put("cheese", cdn + "1f9c0.png");
        map.put("meat_on_bone", cdn + "1f356.png");
        map.put("poultry_leg", cdn + "1f357.png");
        map.put("bacon", cdn + "1f953.png");
        map.put("hamburger", cdn + "1f354.png");
        map.put("fries", cdn + "1f35f.png");
        map.put("pizza", cdn + "1f355.png");
        map.put("hotdog", cdn + "1f32d.png");
        map.put("sandwich", cdn + "1f96a.png");
        map.put("taco", cdn + "1f32e.png");
        map.put("burrito", cdn + "1f32f.png");
        map.put("egg", cdn + "1f95a.png");
        map.put("cooking", cdn + "1f373.png");
        map.put("stew", cdn + "1f372.png");
        map.put("bowl_with_spoon", cdn + "1f963.png");
        map.put("salad", cdn + "1f957.png");
        map.put("popcorn", cdn + "1f37f.png");
        map.put("butter", cdn + "1f9c8.png");
        map.put("salt", cdn + "1f9c2.png");
        map.put("canned_food", cdn + "1f96b.png");
        map.put("bento", cdn + "1f371.png");
        map.put("rice_cracker", cdn + "1f358.png");
        map.put("rice_ball", cdn + "1f359.png");
        map.put("rice", cdn + "1f35a.png");
        map.put("curry", cdn + "1f35b.png");
        map.put("ramen", cdn + "1f35c.png");
        map.put("spaghetti", cdn + "1f35d.png");
        map.put("oden", cdn + "1f362.png");
        map.put("sushi", cdn + "1f363.png");
        map.put("fried_shrimp", cdn + "1f364.png");
        map.put("fish_cake", cdn + "1f365.png");
        map.put("moon_cake", cdn + "1f96e.png");
        map.put("dango", cdn + "1f361.png");
        map.put("dumpling", cdn + "1f95f.png");
        map.put("fortune_cookie", cdn + "1f960.png");
        map.put("takeout_box", cdn + "1f961.png");
        map.put("crab", cdn + "1f980.png");
        map.put("lobster", cdn + "1f99e.png");
        map.put("shrimp", cdn + "1f990.png");
        map.put("squid", cdn + "1f991.png");
        map.put("oyster", cdn + "1f9aa.png");
        map.put("icecream", cdn + "1f366.png");
        map.put("shaved_ice", cdn + "1f367.png");
        map.put("ice_cream", cdn + "1f368.png");
        map.put("doughnut", cdn + "1f369.png");
        map.put("cookie", cdn + "1f36a.png");
        map.put("birthday", cdn + "1f382.png");
        map.put("cake", cdn + "1f370.png");
        map.put("cupcake", cdn + "1f9c1.png");
        map.put("pie", cdn + "1f967.png");
        map.put("chocolate_bar", cdn + "1f36b.png");
        map.put("candy", cdn + "1f36c.png");
        map.put("lollipop", cdn + "1f36d.png");
        map.put("custard", cdn + "1f36e.png");
        map.put("honey_pot", cdn + "1f36f.png");
        map.put("baby_bottle", cdn + "1f37c.png");
        map.put("milk_glass", cdn + "1f95b.png");
        map.put("coffee", cdn + "2615.png");
        map.put("tea", cdn + "1f375.png");
        map.put("sake", cdn + "1f376.png");
        map.put("champagne", cdn + "1f37e.png");
        map.put("wine_glass", cdn + "1f377.png");
        map.put("cocktail", cdn + "1f378.png");
        map.put("tropical_drink", cdn + "1f379.png");
        map.put("beer", cdn + "1f37a.png");
        map.put("beers", cdn + "1f37b.png");
        map.put("clinking_glasses", cdn + "1f942.png");
        map.put("tumbler_glass", cdn + "1f943.png");
        map.put("cup_with_straw", cdn + "1f964.png");
        map.put("beverage_box", cdn + "1f9c3.png");
        map.put("mate", cdn + "1f9c9.png");
        map.put("ice_cube", cdn + "1f9ca.png");
        map.put("chopsticks", cdn + "1f962.png");
        map.put("plate_with_cutlery", cdn + "1f37d.png");
        map.put("fork_and_knife", cdn + "1f374.png");
        map.put("spoon", cdn + "1f944.png");
        map.put("knife", cdn + "1f52a.png");
        map.put("amphora", cdn + "1f3fa.png");

        // Activities & Objects
        map.put("soccer", cdn + "26bd.png");
        map.put("basketball", cdn + "1f3c0.png");
        map.put("football", cdn + "1f3c8.png");
        map.put("baseball", cdn + "26be.png");
        map.put("softball", cdn + "1f94e.png");
        map.put("tennis", cdn + "1f3be.png");
        map.put("volleyball", cdn + "1f3d0.png");
        map.put("rugby_football", cdn + "1f3c9.png");
        map.put("flying_disc", cdn + "1f94f.png");
        map.put("golf", cdn + "26f3.png");
        map.put("video_game", cdn + "1f3ae.png");
        map.put("joystick", cdn + "1f579.png");
        map.put("game_die", cdn + "1f3b2.png");
        map.put("chess_pawn", cdn + "265f.png");
        map.put("dart", cdn + "1f3af.png");
        map.put("bowling", cdn + "1f3b3.png");
        map.put("slot_machine", cdn + "1f3b0.png");
        map.put("8ball", cdn + "1f3b1.png");

        // Travel & Places
        map.put("car", cdn + "1f697.png");
        map.put("taxi", cdn + "1f695.png");
        map.put("bus", cdn + "1f68c.png");
        map.put("truck", cdn + "1f69a.png");
        map.put("fire_engine", cdn + "1f692.png");
        map.put("ambulance", cdn + "1f691.png");
        map.put("police_car", cdn + "1f693.png");
        map.put("motorcycle", cdn + "1f3cd.png");
        map.put("bike", cdn + "1f6b2.png");
        map.put("airplane", cdn + "2708.png");
        map.put("rocket", cdn + "1f680.png");
        map.put("flying_saucer", cdn + "1f6f8.png");
        map.put("helicopter", cdn + "1f681.png");
        map.put("ship", cdn + "1f6a2.png");
        map.put("anchor", cdn + "2693.png");
        map.put("sailboat", cdn + "26f5.png");
        map.put("boat", cdn + "26f5.png");
        map.put("house", cdn + "1f3e0.png");
        map.put("office", cdn + "1f3e2.png");
        map.put("hospital", cdn + "1f3e5.png");
        map.put("bank", cdn + "1f3e6.png");
        map.put("hotel", cdn + "1f3e8.png");
        map.put("school", cdn + "1f3eb.png");
        map.put("church", cdn + "26ea.png");
        map.put("castle", cdn + "1f3f0.png");
        map.put("european_castle", cdn + "1f3f0.png");
        map.put("moon", cdn + "1f319.png");
        map.put("full_moon", cdn + "1f315.png");
        map.put("new_moon", cdn + "1f311.png");
        map.put("sun_with_face", cdn + "1f31e.png");
        map.put("star", cdn + "2b50.png");
        map.put("star2", cdn + "1f31f.png");
        map.put("stars", cdn + "1f320.png");
        map.put("cloud", cdn + "2601.png");
        map.put("sunny", cdn + "2600.png");
        map.put("rainbow", cdn + "1f308.png");
        map.put("umbrella", cdn + "2602.png");
        map.put("snowflake", cdn + "2744.png");
        map.put("snowman", cdn + "26c4.png");
        map.put("zap", cdn + "26a1.png");
        map.put("fire", cdn + "1f525.png");
        map.put("droplet", cdn + "1f4a7.png");
        map.put("ocean", cdn + "1f30a.png");

        // Symbols
        map.put("warning", cdn + "26a0.png");
        map.put("no_entry", cdn + "26d4.png");
        map.put("stop_sign", cdn + "1f6d1.png");
        map.put("radioactive", cdn + "2622.png");
        map.put("biohazard", cdn + "2623.png");
        map.put("arrow_up", cdn + "2b06.png");
        map.put("arrow_down", cdn + "2b07.png");
        map.put("arrow_left", cdn + "2b05.png");
        map.put("arrow_right", cdn + "27a1.png");
        map.put("recycle", cdn + "267b.png");
        map.put("check", cdn + "2705.png");
        map.put("white_check_mark", cdn + "2705.png");
        map.put("x", cdn + "274c.png");
        map.put("cross_mark", cdn + "274c.png");
        map.put("question", cdn + "2753.png");
        map.put("exclamation", cdn + "2757.png");
        map.put("interrobang", cdn + "2049.png");
        map.put("bangbang", cdn + "203c.png");
        map.put("copyright", cdn + "a9.png");
        map.put("registered", cdn + "ae.png");
        map.put("tm", cdn + "2122.png");
        map.put("infinity", cdn + "267e.png");
        map.put("sparkle", cdn + "2747.png");
        map.put("sparkles", cdn + "2728.png");
        map.put("eight_spoked_asterisk", cdn + "2733.png");
        map.put("eight_pointed_black_star", cdn + "2734.png");
        map.put("hash", cdn + "0023-20e3.png");
        map.put("keycap_star", cdn + "002a-20e3.png");
        map.put("zero", cdn + "0030-20e3.png");
        map.put("one", cdn + "0031-20e3.png");
        map.put("two", cdn + "0032-20e3.png");
        map.put("three", cdn + "0033-20e3.png");
        map.put("four", cdn + "0034-20e3.png");
        map.put("five", cdn + "0035-20e3.png");
        map.put("six", cdn + "0036-20e3.png");
        map.put("seven", cdn + "0037-20e3.png");
        map.put("eight", cdn + "0038-20e3.png");
        map.put("nine", cdn + "0039-20e3.png");
        map.put("keycap_ten", cdn + "1f51f.png");

        // Misc popular
        map.put("gift", cdn + "1f381.png");
        map.put("balloon", cdn + "1f388.png");
        map.put("tada", cdn + "1f389.png");
        map.put("confetti_ball", cdn + "1f38a.png");
        map.put("ribbon", cdn + "1f380.png");
        map.put("trophy", cdn + "1f3c6.png");
        map.put("medal", cdn + "1f3c5.png");
        map.put("first_place_medal", cdn + "1f947.png");
        map.put("second_place_medal", cdn + "1f948.png");
        map.put("third_place_medal", cdn + "1f949.png");
        map.put("crown", cdn + "1f451.png");
        map.put("gem", cdn + "1f48e.png");
        map.put("ring", cdn + "1f48d.png");
        map.put("bell", cdn + "1f514.png");
        map.put("no_bell", cdn + "1f515.png");
        map.put("musical_note", cdn + "1f3b5.png");
        map.put("notes", cdn + "1f3b6.png");
        map.put("microphone", cdn + "1f3a4.png");
        map.put("headphones", cdn + "1f3a7.png");
        map.put("guitar", cdn + "1f3b8.png");
        map.put("violin", cdn + "1f3bb.png");
        map.put("trumpet", cdn + "1f3ba.png");
        map.put("drum", cdn + "1f941.png");
        map.put("movie_camera", cdn + "1f3a5.png");
        map.put("clapper", cdn + "1f3ac.png");
        map.put("tv", cdn + "1f4fa.png");
        map.put("camera", cdn + "1f4f7.png");
        map.put("phone", cdn + "260e.png");
        map.put("telephone", cdn + "260e.png");
        map.put("mobile_phone", cdn + "1f4f1.png");
        map.put("iphone", cdn + "1f4f1.png");
        map.put("computer", cdn + "1f4bb.png");
        map.put("keyboard", cdn + "2328.png");
        map.put("desktop_computer", cdn + "1f5a5.png");
        map.put("printer", cdn + "1f5a8.png");
        map.put("mouse_computer", cdn + "1f5b1.png");
        map.put("trackball", cdn + "1f5b2.png");
        map.put("cd", cdn + "1f4bf.png");
        map.put("dvd", cdn + "1f4c0.png");
        map.put("floppy_disk", cdn + "1f4be.png");
        map.put("minidisc", cdn + "1f4bd.png");
        map.put("battery", cdn + "1f50b.png");
        map.put("electric_plug", cdn + "1f50c.png");
        map.put("bulb", cdn + "1f4a1.png");
        map.put("flashlight", cdn + "1f526.png");
        map.put("candle", cdn + "1f56f.png");
        map.put("fire_extinguisher", cdn + "1f9ef.png");
        map.put("wastebasket", cdn + "1f5d1.png");
        map.put("oil_drum", cdn + "1f6e2.png");
        map.put("money_with_wings", cdn + "1f4b8.png");
        map.put("dollar", cdn + "1f4b5.png");
        map.put("yen", cdn + "1f4b4.png");
        map.put("euro", cdn + "1f4b6.png");
        map.put("pound", cdn + "1f4b7.png");
        map.put("moneybag", cdn + "1f4b0.png");
        map.put("credit_card", cdn + "1f4b3.png");
        map.put("gem", cdn + "1f48e.png");
        map.put("balance_scale", cdn + "2696.png");
        map.put("wrench", cdn + "1f527.png");
        map.put("hammer", cdn + "1f528.png");
        map.put("hammer_and_wrench", cdn + "1f6e0.png");
        map.put("pick", cdn + "26cf.png");
        map.put("nut_and_bolt", cdn + "1f529.png");
        map.put("gear", cdn + "2699.png");
        map.put("chains", cdn + "26d3.png");
        map.put("magnet", cdn + "1f9f2.png");
        map.put("gun", cdn + "1f52b.png");
        map.put("pistol", cdn + "1f52b.png");
        map.put("bomb", cdn + "1f4a3.png");
        map.put("knife", cdn + "1f52a.png");
        map.put("dagger", cdn + "1f5e1.png");
        map.put("crossed_swords", cdn + "2694.png");
        map.put("shield", cdn + "1f6e1.png");
        map.put("smoking", cdn + "1f6ac.png");
        map.put("coffin", cdn + "26b0.png");
        map.put("skull", cdn + "1f480.png");
        map.put("key", cdn + "1f511.png");
        map.put("old_key", cdn + "1f5dd.png");
        map.put("lock", cdn + "1f512.png");
        map.put("unlock", cdn + "1f513.png");
        map.put("door", cdn + "1f6aa.png");
        map.put("bed", cdn + "1f6cf.png");
        map.put("couch_and_lamp", cdn + "1f6cb.png");
        map.put("toilet", cdn + "1f6bd.png");
        map.put("shower", cdn + "1f6bf.png");
        map.put("bathtub", cdn + "1f6c1.png");
        map.put("hourglass", cdn + "231b.png");
        map.put("hourglass_flowing_sand", cdn + "23f3.png");
        map.put("watch", cdn + "231a.png");
        map.put("alarm_clock", cdn + "23f0.png");
        map.put("stopwatch", cdn + "23f1.png");
        map.put("timer_clock", cdn + "23f2.png");
        map.put("clock", cdn + "1f570.png");
        map.put("calendar", cdn + "1f4c5.png");
        map.put("date", cdn + "1f4c5.png");
        map.put("spiral_calendar", cdn + "1f5d3.png");
        map.put("scroll", cdn + "1f4dc.png");
        map.put("page_facing_up", cdn + "1f4c4.png");
        map.put("newspaper", cdn + "1f4f0.png");
        map.put("bookmark", cdn + "1f516.png");
        map.put("label", cdn + "1f3f7.png");
        map.put("envelope", cdn + "2709.png");
        map.put("email", cdn + "1f4e7.png");
        map.put("incoming_envelope", cdn + "1f4e8.png");
        map.put("inbox_tray", cdn + "1f4e5.png");
        map.put("outbox_tray", cdn + "1f4e4.png");
        map.put("package", cdn + "1f4e6.png");
        map.put("mailbox", cdn + "1f4eb.png");
        map.put("mailbox_closed", cdn + "1f4ea.png");
        map.put("mailbox_with_mail", cdn + "1f4ec.png");
        map.put("mailbox_with_no_mail", cdn + "1f4ed.png");
        map.put("postbox", cdn + "1f4ee.png");
        map.put("pencil", cdn + "270f.png");
        map.put("pencil2", cdn + "270f.png");
        map.put("black_nib", cdn + "2712.png");
        map.put("fountain_pen", cdn + "1f58b.png");
        map.put("pen", cdn + "1f58a.png");
        map.put("paintbrush", cdn + "1f58c.png");
        map.put("crayon", cdn + "1f58d.png");
        map.put("memo", cdn + "1f4dd.png");
        map.put("briefcase", cdn + "1f4bc.png");
        map.put("file_folder", cdn + "1f4c1.png");
        map.put("open_file_folder", cdn + "1f4c2.png");
        map.put("card_index_dividers", cdn + "1f5c2.png");
        map.put("clipboard", cdn + "1f4cb.png");
        map.put("pushpin", cdn + "1f4cc.png");
        map.put("round_pushpin", cdn + "1f4cd.png");
        map.put("paperclip", cdn + "1f4ce.png");
        map.put("paperclips", cdn + "1f587.png");
        map.put("straight_ruler", cdn + "1f4cf.png");
        map.put("triangular_ruler", cdn + "1f4d0.png");
        map.put("scissors", cdn + "2702.png");
        map.put("card_file_box", cdn + "1f5c3.png");
        map.put("file_cabinet", cdn + "1f5c4.png");
        map.put("link", cdn + "1f517.png");

        System.out.println("[Voidium] Added " + map.size() + " built-in Discord emojis");
    }
}
