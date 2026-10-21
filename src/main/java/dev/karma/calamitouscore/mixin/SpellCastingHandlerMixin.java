package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.mana.ManaCapOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Binds the owning {@link Player} onto Brutality 0.8.5's mana capability before
 * it is read, so {@link PlayerManaCapMixin} can route server-side mana checks,
 * costs, grants and refunds onto The Dulling's shared pool.
 *
 * <p>This covers every server flow that funnels through
 * {@code SpellCastingHandler.getManaHandler}: {@code hasEnoughMana},
 * {@code handleSpellManaCost} and {@code addMana}.
 */
@Pseudo
@Mixin(targets = "net.goo.brutality.magic.SpellCastingHandler", remap = false)
public abstract class SpellCastingHandlerMixin {

    @Inject(method = "getManaHandler", at = @At("RETURN"), remap = false)
    private static void calamitouscore$bindManaHandler(Player player,
                                                       CallbackInfoReturnable<LazyOptional<?>> cir) {
        LazyOptional<?> lazy = cir.getReturnValue();
        if (lazy == null) return;
        lazy.resolve().ifPresent(cap -> {
            if (cap instanceof ManaCapOwner owner) {
                owner.calamitouscore$bind(player);
            }
        });
    }
}
