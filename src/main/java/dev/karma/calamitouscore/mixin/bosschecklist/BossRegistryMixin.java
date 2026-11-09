package dev.karma.calamitouscore.mixin.bosschecklist;

import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.yori3o.boss_checklist.common.client.data.BossRegistry", remap = false)
public abstract class BossRegistryMixin {
    @Shadow @Final
    private static Map<String, ?> DEFINITIONS;

    @Inject(method = "reload()V", at = @At(value = "INVOKE",
            target = "Lcom/yori3o/boss_checklist/common/client/data/BossNameCache;rebuild()V"))
    private static void calamitouscore$hideNamelessGuardian(CallbackInfo ci) {
        DEFINITIONS.remove("eeeabsmobs:nameless_guardian");
    }
}
