package dev.karma.calamitouscore.mana;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the shared mana pool's cap in sync with Brutality's max mana and folds
 * legacy Brutality mana into the pool. All handlers run at {@link EventPriority#HIGHEST}
 * so the mirrored cap is in place before The Dulling's own login/respawn/tick
 * handlers clamp and sync the pool.
 */
@Mod.EventBusSubscriber(modid = "calamitouscore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManaBridgeEvents {

    private ManaBridgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer serverPlayer) {
            ManaBridge.applyMaxManaMirror(serverPlayer);
            ManaBridge.applyManaRegenBase(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ManaBridge.applyMaxManaMirror(serverPlayer);
            ManaBridge.applyManaRegenBase(serverPlayer);
            ManaBridge.bindOwner(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ManaBridge.applyMaxManaMirror(serverPlayer);
            ManaBridge.applyManaRegenBase(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ManaBridge.applyMaxManaMirror(serverPlayer);
            ManaBridge.applyManaRegenBase(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ManaBridge.clearFraction(event.getEntity().getUUID());
    }
}
