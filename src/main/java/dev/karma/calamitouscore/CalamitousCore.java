package dev.karma.calamitouscore;

import dev.karma.calamitouscore.compat.herobrine.HerobrineConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod("calamitouscore")
public final class CalamitousCore {
    public CalamitousCore() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HerobrineConfig.SPEC, "calamitouscore-common.toml");
    }
}
