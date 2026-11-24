package dev.karma.calamitouscore.mixin;

import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Companion to HerobrineArenaSuppressMixin: the ritual creates no platform to return players to. */
@Pseudo
@Mixin(targets = "net.mcreator.herobrineprime.procedures.MakeSurePlayerIsInArenaProcedure", remap = false)
public abstract class HerobrineArenaFloorMixin {
    @Inject(method = "onPlayerTick(Lnet/minecraftforge/event/TickEvent$PlayerTickEvent;)V",
        at = @At("HEAD"), cancellable = true, require = 1)
    private static void calamitouscore$skipAbsentArenaFloor(TickEvent.PlayerTickEvent event, CallbackInfo info) {
        info.cancel();
    }
}
