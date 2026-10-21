package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.mcreator.thedulling.DullingCityBossStructureManager",remap=false)
public abstract class BossCityTickMixin {
    @Inject(method="onLevelTick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$skip(TickEvent.LevelTickEvent e,CallbackInfo ci) {
        if (DimensionGuard.blocked(e.level)) ci.cancel();
    }
}
