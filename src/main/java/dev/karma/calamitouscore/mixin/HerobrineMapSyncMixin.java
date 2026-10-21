package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrineConfig;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles the redundant per-tick map-variable sync during the Herobrine
 * fight.
 *
 * <p>{@code HerobrineAIProcedure.execute} calls {@code MapVariables.syncData}
 * two or more times every single tick, and each call serialises the whole map
 * variable set to NBT and broadcasts a packet to every player. That is a lot of
 * pointless network and serialisation traffic: the server owns that state and
 * the client never runs the AI.
 *
 * <p>This injection only throttles the sync while the boss is <em>idle</em>
 * ({@code attack == 0}); whenever the AI is in the middle of an attack or a
 * phase transition the sync is left untouched, so every state change that
 * matters is still propagated immediately. The idle sync is reduced to a few
 * times per second, which is more than enough for the client-side
 * representation.
 */
@Pseudo
@Mixin(targets = "net.mcreator.herobrineprime.network.HerobrinePrimeModVariables$MapVariables", remap = false)
public abstract class HerobrineMapSyncMixin {

    @Shadow
    public double attack;

    private static long calamitouscore$lastSyncTick = Long.MIN_VALUE;

    @Inject(method = "syncData", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$throttleIdleSync(LevelAccessor world, CallbackInfo ci) {
        if (!HerobrineConfig.optimizeTickWork()) {
            return;
        }
        // Always allow syncing during attacks and transitions.
        if (this.attack != 0.0D) {
            return;
        }
        long now = nowTicks();
        if (now < 0L) {
            return;
        }
        if (now - calamitouscore$lastSyncTick < 3L) {
            ci.cancel();
            return;
        }
        calamitouscore$lastSyncTick = now;
    }

    private static long nowTicks() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return -1L;
        }
        net.minecraft.server.level.ServerLevel overworld = server.overworld();
        return overworld == null ? -1L : overworld.getGameTime();
    }
}
