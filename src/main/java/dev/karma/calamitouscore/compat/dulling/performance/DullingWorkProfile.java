package dev.karma.calamitouscore.compat.dulling.performance;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;

/** Opt-in attribution of the report's inclusive onServerTick sample; no clocks when disabled. */
public final class DullingWorkProfile {
    public static final boolean ENABLED = Boolean.getBoolean("calamitouscore.dullingProfile");
    private static final long[] TOTAL = new long[3], MAX = new long[3];
    private static MinecraftServer current;
    private static int ticks;
    private DullingWorkProfile() {}
    public static void record(MinecraftServer server, int kind, long elapsed) {
        if (server != current) { current = server; ticks = 0; java.util.Arrays.fill(TOTAL, 0); java.util.Arrays.fill(MAX, 0); }
        TOTAL[kind] += elapsed; MAX[kind] = Math.max(MAX[kind], elapsed);
        if (kind == 2 && ++ticks == 1200) {
            LogUtils.getLogger().info("Dulling work profile (1200 ticks): resource index avg/max {} / {} ms, base {} / {} ms, progression {} / {} ms",
                    TOTAL[0] / 1200000000.0, MAX[0] / 1000000.0,
                    TOTAL[1] / 1200000000.0, MAX[1] / 1000000.0,
                    TOTAL[2] / 1200000000.0, MAX[2] / 1000000.0);
            ticks = 0; java.util.Arrays.fill(TOTAL, 0); java.util.Arrays.fill(MAX, 0);
        }
    }
    public static void clear() { current = null; ticks = 0; }
}
