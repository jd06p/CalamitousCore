package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.mcreator.thedulling.DullingSpawnEventManager",remap=false)
public abstract class SpawnScheduleMixin {
    @Inject(method="schedule",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$skip(ServerLevel level, BlockPos pos, int a, int b,
        List<EntityType<? extends Mob>> types, int c, CallbackInfo ci) {
        if (DimensionGuard.blocked(level)) ci.cancel();
    }
}
