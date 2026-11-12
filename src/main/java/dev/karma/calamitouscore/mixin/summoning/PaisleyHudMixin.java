package dev.karma.calamitouscore.mixin.summoning;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = {"net.paisleypug.terramitypaisleyssummoning.client.screens.CurrentSummonOverlayOverlay", "net.paisleypug.terramitypaisleyssummoning.client.screens.SummonOverlayOverlay"}, remap = false)
public abstract class PaisleyHudMixin {
    @Inject(method = "eventHandler", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$shared(RenderGuiEvent.Pre event, CallbackInfo ci) { ci.cancel(); }
}
