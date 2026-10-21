package dev.karma.calamitouscore.mixin;

import net.mcreator.thedulling.client.ManaClientData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides The Dulling's mana HUD whenever the shared mana pool is full.
 *
 * <p>The mana bar is rendered by the {@code OVERLAY} lambda in
 * {@link net.mcreator.thedulling.client.ManaHudOverlay}, which has no
 * early-out for a full pool. Its body is cancelled at the head as soon as the
 * client-synchronised current mana reaches the client-synchronised maximum, so
 * the bar disappears once mana is topped up and reappears as soon as any mana is
 * spent.
 *
 * <p>Both values are integers delivered by The Dulling's own mana sync packet,
 * so the comparison is exact (no floating-point drift) and reflects the
 * authoritative server pool, including maximum-mana changes from equipment such
 * as the Brutality mirror and the Rune of Delta.
 */
@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.ManaHudOverlay", remap = false)
public abstract class ManaHudVisibilityMixin {

    @Inject(method = "lambda$static$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void calamitouscore$hideWhenFull(CallbackInfo ci) {
        int max = ManaClientData.getMaxMana();
        if (max > 0 && ManaClientData.getCurrentMana() >= max) {
            ci.cancel();
        }
    }
}
