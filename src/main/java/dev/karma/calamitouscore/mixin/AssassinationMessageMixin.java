package dev.karma.calamitouscore.mixin;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.mcreator.thedulling.DullingTaskManager",remap=false)
public abstract class AssassinationMessageMixin {
    @Inject(method="announce",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$suppress(ServerLevel level, String message, CallbackInfo ci) {
        ci.cancel();
    }
}
