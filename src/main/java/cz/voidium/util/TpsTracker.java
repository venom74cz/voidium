package cz.voidium.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Queue;

public class TpsTracker {
    private static final TpsTracker INSTANCE = new TpsTracker();

    private final Queue<Long> tickTimes = new ArrayDeque<>();
    private long lastTickTime = 0;

    // Rolling average over last 100 ticks (5 seconds)
    private static final int SAMPLE_SIZE = 100;

    private TpsTracker() {
        NeoForge.EVENT_BUS.register(this);
    }

    public static TpsTracker getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long now = System.nanoTime();

        if (lastTickTime != 0) {
            long diff = now - lastTickTime;
            tickTimes.add(diff);

            while (tickTimes.size() > SAMPLE_SIZE) {
                tickTimes.poll();
            }
        }

        lastTickTime = now;
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        tickTimes.clear();
        lastTickTime = 0;
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        tickTimes.clear();
    }

    public double getTPS() {
        if (tickTimes.isEmpty())
            return 20.0;

        long sum = 0;
        for (long time : tickTimes) {
            sum += time;
        }

        double avgTickNanos = (double) sum / tickTimes.size();

        // Prevent division by zero or extremely small values
        if (avgTickNanos < 1000)
            return 20.0;

        double tps = 1_000_000_000.0 / avgTickNanos;

        return Math.min(20.0, tps);
    }

    public double getMSPT() {
        if (tickTimes.isEmpty())
            return 0.0;

        long sum = 0;
        for (long time : tickTimes) {
            sum += time;
        }

        return (double) sum / tickTimes.size() / 1_000_000.0;
    }
}
