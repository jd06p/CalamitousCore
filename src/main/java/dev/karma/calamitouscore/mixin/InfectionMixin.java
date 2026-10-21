package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.mcreator.thedulling.InfectionStateData",remap=false)
public abstract class InfectionMixin {
    @Inject(method="isInfectionActive",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$off(ServerLevel level,CallbackInfoReturnable<Boolean> cir) {
        if (DimensionGuard.blocked(level)) cir.setReturnValue(false);
    }
}
