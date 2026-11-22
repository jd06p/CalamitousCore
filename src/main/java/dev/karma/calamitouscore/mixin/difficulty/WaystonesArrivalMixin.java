package dev.karma.calamitouscore.mixin.difficulty;

import com.misanthropy.linggango.difficulty_enhancement.server.Fate;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.misanthropy.linggango.difficulty_enhancement.compat.WaystonesBridge", remap = false)
public abstract class WaystonesArrivalMixin {
    @Redirect(method = "onArrive", at = @At(value = "INVOKE",
        target = "Lcom/misanthropy/linggango/difficulty_enhancement/server/Fate;nauseate(Lnet/minecraft/world/entity/player/Player;)V"))
    private static void calamitouscore$applyArrivalPenalty(Player player) {
        if (!player.isCreative()) Fate.nauseate(player);
    }
}
