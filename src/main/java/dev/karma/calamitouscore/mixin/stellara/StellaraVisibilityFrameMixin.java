package dev.karma.calamitouscore.mixin.stellara;

import dev.karma.calamitouscore.compat.stellara.SkyRenderDiagnostics;
import dev.karma.calamitouscore.compat.stellara.SkyVisibility;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.StellaraSkyboxRenderer", remap = false)
public abstract class StellaraVisibilityFrameMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lit/hurts/shatterbyte/stellara/client/sky/api/SkyboxRenderContext;)V", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$begin(CallbackInfo ci) {
        if (SkyRenderDiagnostics.skipSky()) { ci.cancel(); return; }
        SkyVisibility.begin(); SkyRenderDiagnostics.beginSky();
    }
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lit/hurts/shatterbyte/stellara/client/sky/api/SkyboxRenderContext;)V", at = @At("RETURN"))
    private static void calamitouscore$end(CallbackInfo ci) { SkyRenderDiagnostics.endSky(); SkyVisibility.end(); }
}
