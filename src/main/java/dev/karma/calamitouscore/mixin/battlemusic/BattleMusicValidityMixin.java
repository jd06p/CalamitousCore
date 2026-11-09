package dev.karma.calamitouscore.mixin.battlemusic;

import dev.karma.calamitouscore.compat.battlemusic.BattleMusicLifecycle;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.nekotune.battlemusic.BattleMusic", remap = false)
public abstract class BattleMusicValidityMixin {
    @Inject(method = "validEntity", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$rejectStaleTarget(Mob mob, boolean toStart, CallbackInfoReturnable<Boolean> ci) {
        if (BattleMusicLifecycle.invalidTarget(mob)) ci.setReturnValue(false);
    }
}
