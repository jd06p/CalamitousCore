package dev.karma.calamitouscore.compat.performance;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class PerformanceRegistration {
    private PerformanceRegistration() {}
    public static void register() {
        com.mojang.logging.LogUtils.getLogger().info("CalamitousCore performance revision 2026-11-22-wave3 loaded; performance optimizations {}",
                Boolean.getBoolean("calamitouscore.performanceWave2.disabled") ? "disabled for comparison" : "enabled");
        if (!Boolean.getBoolean("calamitouscore.performanceWave2.disabled") && ModList.get().isLoaded("summonity")) MinecraftForge.EVENT_BUS.register(MinionSlotIndex.class);
    }
}

