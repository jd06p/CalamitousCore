package dev.karma.calamitouscore.mixin.stellara;

import java.util.List;
import java.util.Set;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxEntry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** The original combined sort is only invalidated by these two registry mutation paths. */
@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRegistry", remap = false)
public abstract class StellaraRegistryMixin {
    @Unique private static List<?> calamitouscore$snapshot;
    @Unique private static final Set<String> calamitouscore$stablePriorities = Set.of(
            "AuroraSkyContent", "ConstellationSkyContent", "RainbowSkyContent", "ShootingStarSkyContent",
            "SkyRibbonContent", "SkyTintContent", "StarFieldSkyContent", "TexturedPlanetSystemSkyContent");

    @Inject(method = "entries", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$reuse(CallbackInfoReturnable<List<?>> cir) {
        if (calamitouscore$snapshot != null) cir.setReturnValue(calamitouscore$snapshot);
    }
    @Inject(method = "entries", at = @At("RETURN"))
    private static void calamitouscore$remember(CallbackInfoReturnable<List<?>> cir) {
        // A third-party API content implementation may compute a changing priority. Keep its
        // original per-call sorting behavior rather than assuming it has Stellara's final fields.
        for (Object value : cir.getReturnValue()) {
            String name = ((SkyboxEntry) value).content().getClass().getName();
            if (!name.startsWith("it.hurts.shatterbyte.stellara.client.sky.content.")
                    || !calamitouscore$stablePriorities.contains(name.substring(name.lastIndexOf('.') + 1))) {
                calamitouscore$snapshot = null;
                return;
            }
        }
        calamitouscore$snapshot = cir.getReturnValue();
    }
    @Inject(method = "register(Lit/hurts/shatterbyte/stellara/client/sky/api/SkyboxContent;Lit/hurts/shatterbyte/stellara/client/sky/api/SkyboxCondition;F)V", at = @At("RETURN"))
    private static void calamitouscore$registered(CallbackInfo ci) { calamitouscore$snapshot = null; }
    @Inject(method = "replaceDataEntries", at = @At("RETURN"))
    private static void calamitouscore$reloaded(CallbackInfo ci) { calamitouscore$snapshot = null; }
}
