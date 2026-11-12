package dev.karma.calamitouscore.compat.summoning;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import com.mojang.logging.LogUtils;

public final class SummoningCompatibility {
    private SummoningCompatibility() {}
    public static void register() {
        if (ModList.get().isLoaded("summonity") && ModList.get().isLoaded("terramity_paisleys_summoning")) {
            if (ModList.get().isLoaded("summoning_compat")) {
                LogUtils.getLogger().warn("CalamitousCore summoning bridge is inactive while the original summoning_compat mod is installed; remove that obsolete bridge to use this implementation.");
            } else MinecraftForge.EVENT_BUS.register(SummoningEvents.class);
        }
    }
}
