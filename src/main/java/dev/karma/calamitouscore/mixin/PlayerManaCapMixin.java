package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.mana.ManaBridge;
import dev.karma.calamitouscore.mana.ManaCapOwner;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes Brutality 0.8.5's player mana capability
 * ({@code net.goo.brutality.entity.capabilities.EntityCapabilities$PlayerManaCap})
 * onto The Dulling's shared pool via {@link ManaBridge}.
 *
 * <p>The capability instance is shared per player but does not store a reference
 * to its owner, so the owner is bound lazily whenever the capability is touched
 * from a context that has a {@link Player} in hand
 * ({@link PlayerManaCapMixin#calamitouscore$bind}). Until bound, the capability's
 * own float field is used so client-only early reads behave exactly as vanilla.
 */
@Pseudo
@Mixin(targets = "net.goo.brutality.entity.capabilities.EntityCapabilities$PlayerManaCap", remap = false)
public abstract class PlayerManaCapMixin implements ManaCapOwner {

    @Unique
    private Player calamitouscore$owner;

    @Override
    public void calamitouscore$bind(Player player) {
        if (player != null) {
            this.calamitouscore$owner = player;
        }
    }

    @Inject(method = "manaValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$manaValue(CallbackInfoReturnable<Float> cir) {
        if (this.calamitouscore$owner != null) {
            cir.setReturnValue(ManaBridge.getBrutalityMana(this.calamitouscore$owner));
        }
    }

    @Inject(method = "setManaValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$setManaValue(float value, CallbackInfo ci) {
        if (this.calamitouscore$owner != null) {
            ManaBridge.setMana(this.calamitouscore$owner, value);
            ci.cancel();
        }
    }

    @Inject(method = "incrementMana", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$incrementMana(float amount, CallbackInfo ci) {
        if (this.calamitouscore$owner != null) {
            ManaBridge.modify(this.calamitouscore$owner, amount);
            ci.cancel();
        }
    }

    @Inject(method = "decrementMana", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$decrementMana(float amount, CallbackInfo ci) {
        if (this.calamitouscore$owner != null) {
            ManaBridge.modify(this.calamitouscore$owner, -amount);
            ci.cancel();
        }
    }

    @Inject(method = "isMaxMana", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$isMaxMana(Player player, CallbackInfoReturnable<Boolean> cir) {
        calamitouscore$bind(player);
        cir.setReturnValue(ManaBridge.isMaxMana(player));
    }

    @Inject(method = "getCurrentManaRatio", at = @At("HEAD"), cancellable = true, remap = false)
    private void calamitouscore$getCurrentManaRatio(Player player, CallbackInfoReturnable<Float> cir) {
        calamitouscore$bind(player);
        cir.setReturnValue(ManaBridge.getCurrentManaRatio(player));
    }
}
