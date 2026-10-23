package dev.karma.calamitouscore;

import dev.karma.calamitouscore.compat.crc.CrcPortalNetwork;
import dev.karma.calamitouscore.compat.herobrine.HerobrineConfig;
import dev.karma.calamitouscore.compat.lunar.LunarConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("calamitouscore")
public final class CalamitousCore {
    public CalamitousCore() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HerobrineConfig.SPEC, "calamitouscore-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LunarConfig.SPEC, "calamitouscore-client.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CalamitousCore::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CrcPortalNetwork::register);
    }
}
