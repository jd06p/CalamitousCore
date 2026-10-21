package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import net.mcreator.thedulling.world.DullingWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.mcreator.thedulling.DullingSpawnBrain",remap=false)
public abstract class SpawnBrainMixin {
    @Inject(method="seedActiveChunksByRareScan",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$off(ServerLevel level, DullingWorldData data, ResourceLocation dim, CallbackInfo ci) {
        if (DimensionGuard.blocked(level)) ci.cancel();
    }
}
