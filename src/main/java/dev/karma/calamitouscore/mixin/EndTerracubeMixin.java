package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.DimensionGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.thedulling.TerracubeStructures", remap = false)
public abstract class EndTerracubeMixin {
    @Inject(method = "buildRandomStructures", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$guardEndBuild(ServerLevel level, BlockPos pos, int radius,
            int count, int tier, CallbackInfo ci) {
        if (DimensionGuard.endLocked(level)) ci.cancel();
    }
}
