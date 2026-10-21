package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.mana.ManaBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Binds the owning player onto Brutality 0.8.5's mana capability before the
 * client mana bar is rendered, so the HUD reflects The Dulling's shared pool
 * (received from The Dulling's mana sync packet) instead of Brutality's own
 * unused float.
 */
@Pseudo
@Mixin(targets = "net.goo.brutality.gui.ModGui", remap = false)
public abstract class ModGuiMixin {

    @Inject(method = "renderManaBar", at = @At("HEAD"), remap = false)
    private static void calamitouscore$bindForHud(net.minecraft.client.gui.GuiGraphics graphics,
                                                   Player player, CallbackInfo ci) {
        ManaBridge.bindOwner(player);
    }
}
