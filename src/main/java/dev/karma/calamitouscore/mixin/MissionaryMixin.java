package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.mcreator.thedulling.GlobalMissionaryHijack",remap=false)
public abstract class MissionaryMixin {
    @Inject(method="onMobFinalizeSpawn",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$skip(MobSpawnEvent.FinalizeSpawn e,CallbackInfo ci) {
        if (DimensionGuard.blocked(e.getEntity().level())) ci.cancel();
    }
}
