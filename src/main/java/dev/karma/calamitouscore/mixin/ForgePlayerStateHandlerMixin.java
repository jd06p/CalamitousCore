package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.mana.ManaBridge;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Binds the owning player onto Brutality 0.8.5's mana capability before its
 * per-tick mana regeneration runs, so Brutality's own regen (which reads the
 * {@code brutality:mana_regen} attribute, including curio bonuses such as the
 * Rune of Delta) is routed onto The Dulling's shared pool by
 * {@link PlayerManaCapMixin}.
 */
@Pseudo
@Mixin(targets = "net.goo.brutality.event.forge.ForgePlayerStateHandler", remap = false)
public abstract class ForgePlayerStateHandlerMixin {

    @Inject(method = "onPlayerTick", at = @At("HEAD"), remap = false)
    private static void calamitouscore$bindForRegen(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
        if (event.phase == TickEvent.Phase.START) {
            ManaBridge.bindOwner(event.player);
        }
    }
}
