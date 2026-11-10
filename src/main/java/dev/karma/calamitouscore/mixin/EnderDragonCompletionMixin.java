package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.dulling.EndDullingProgress;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragon.class)
public abstract class EnderDragonCompletionMixin {
    // The caller sees completion even when a mod cancels the vanilla fight method at HEAD.
    @Redirect(method = {"tickDeath", "kill"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/end/EndDragonFight;setDragonKilled(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;)V"),
            require = 2)
    private void calamitouscore$completeFight(EndDragonFight fight, EnderDragon dragon) {
        EndDullingProgress.completeDragonFight(fight, dragon);
    }
}
