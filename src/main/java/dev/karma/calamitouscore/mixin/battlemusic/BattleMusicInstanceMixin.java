package dev.karma.calamitouscore.mixin.battlemusic;

import com.nekotune.battlemusic.BattleMusicInstance;
import dev.karma.calamitouscore.compat.battlemusic.BattleMusicLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.nekotune.battlemusic.BattleMusicInstance", remap = false)
public abstract class BattleMusicInstanceMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void calamitouscore$track(CallbackInfo ci) {
        BattleMusicLifecycle.created((BattleMusicInstance) (Object) this);
    }
    @Inject(method = "destroy", at = @At("TAIL"))
    private void calamitouscore$releaseTarget(CallbackInfo ci) {
        BattleMusicLifecycle.destroyed((BattleMusicInstance) (Object) this);
    }
    @Inject(method = "fade", at = @At("TAIL"))
    private void calamitouscore$boundFade(float seconds, CallbackInfo ci) {
        BattleMusicLifecycle.fading((BattleMusicInstance) (Object) this, seconds);
    }
    @Inject(method = "m_7788_", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$guardSoundTick(CallbackInfo ci) {
        if (BattleMusicLifecycle.stopIfInvalid((BattleMusicInstance) (Object) this)) ci.cancel();
    }
}
