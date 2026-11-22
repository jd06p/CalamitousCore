package dev.karma.calamitouscore.mixin.dullingperf;

import dev.karma.calamitouscore.compat.dulling.performance.DullingSiteEviction;
import java.util.Map;
import net.mcreator.thedulling.AnthroMindData.MemorySite;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.AnthroMindData", remap = false)
public abstract class AnthroMindDataMixin {
    @Inject(method = "trimSites", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$singleEviction(Map<Long, MemorySite> sites, CallbackInfoReturnable<Boolean> cir) {
        if (sites.size() <= 4096) cir.setReturnValue(false);
        else if (sites.size() == 4097) cir.setReturnValue(DullingSiteEviction.trimOne(sites));
        // Preserve the original stable bulk sort for unusual oversized/imported maps.
    }
}
