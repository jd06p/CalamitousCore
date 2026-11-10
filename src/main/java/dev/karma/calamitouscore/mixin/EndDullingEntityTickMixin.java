package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.dulling.EndDullingSpawns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class EndDullingEntityTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$holdSavedEntity(Entity entity, CallbackInfo ci) {
        if (EndDullingSpawns.lockedEntity((ServerLevel) (Object) this, entity)) ci.cancel();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$holdSavedPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (EndDullingSpawns.lockedEntity((ServerLevel) (Object) this, passenger)) ci.cancel();
    }
}
