package dev.karma.calamitouscore.mixin.dullingperf;

import dev.karma.calamitouscore.compat.dulling.performance.DullingFrameQueries;
import net.mcreator.thedulling.entity.GreatDevourerEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.GreatDevourerDevourRenderHandler", remap = false)
public abstract class DevourerQueryMixin {
    @Inject(method = "findDevourerHolding", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$frameIndex(LivingEntity victim, CallbackInfoReturnable<GreatDevourerEntity> cir) {
        cir.setReturnValue(DullingFrameQueries.holder(victim));
    }
}
