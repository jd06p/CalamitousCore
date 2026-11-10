package dev.karma.calamitouscore.mixin;
import dev.karma.calamitouscore.DimensionGuard;
import dev.karma.calamitouscore.compat.dulling.EndCitySite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.mcreator.thedulling.DullingCityData;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.mcreator.thedulling.DullingCityManager",remap=false)
public abstract class CityTickMixin {
    @Shadow
    private static int findFirstAirFromBottom(ServerLevel level, int x, int z) {
        throw new AssertionError();
    }

    @Inject(method="onLevelTick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void calamitouscore$skip(TickEvent.LevelTickEvent e,CallbackInfo ci) {
        if (DimensionGuard.blocked(e.level)) ci.cancel();
    }

    @Redirect(method = "tryStartDimensionCity", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/thedulling/DullingCityManager;findFirstAirFromBottom(Lnet/minecraft/server/level/ServerLevel;II)I"))
    private static int calamitouscore$chooseEndCityFloor(ServerLevel level, int x, int z) {
        if (Level.END.equals(level.dimension())) return EndCitySite.foundingFloor(level, x, z);
        return findFirstAirFromBottom(level, x, z);
    }

    @Inject(method = "startBuild", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$checkEndCell(ServerLevel level, DullingCityData.City city,
            @Coerce Object request, CallbackInfoReturnable<Boolean> cir) {
        if (!Level.END.equals(level.dimension())) return;
        CityBuildRequestAccessor grid = (CityBuildRequestAccessor) request;
        int x = city.originX() + grid.calamitouscore$getGridX() * 18 + 9;
        int z = city.originZ() + grid.calamitouscore$getGridZ() * 18 + 9;
        if (!EndCitySite.canBuild(level, x, city.floorY(), z)) cir.setReturnValue(false);
    }
}
