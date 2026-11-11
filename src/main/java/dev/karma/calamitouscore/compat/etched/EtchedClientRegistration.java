package dev.karma.calamitouscore.compat.etched;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid = "calamitouscore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EtchedClientRegistration {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("sophisticatedcore"))
            event.enqueueWork(() -> MinecraftForge.EVENT_BUS.register(UpgradePlacementDiagnostics.class));
        if (ModList.get().isLoaded("etched") && ModList.get().isLoaded("sophisticatedcore"))
            event.enqueueWork(() -> MinecraftForge.EVENT_BUS.register(EtchedJukeboxClient.class));
    }
}
