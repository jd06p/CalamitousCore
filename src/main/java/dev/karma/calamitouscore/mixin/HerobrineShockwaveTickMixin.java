package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.ShockwaveAttackProcedure;
import net.mcreator.herobrineprime.procedures.ShockwaveTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reduces the number of sampled points on Herobrine's expanding ground
 * shockwave ring.
 *
 * <p>{@code ShockwaveTickProcedure.execute} computes
 * {@code numParticles = 2*pi*radius} and, for each point, calls
 * {@code ShockwaveAttackProcedure.execute}, which spawns an explosion particle
 * and scans for players. At the maximum radius this is roughly 400 points per
 * tick for many seconds. This redirect skips a fraction of the points (see
 * {@link HerobrinePrimeOptimizations#shouldRunHeavyPoint()}), reducing both the
 * particle traffic and the entity scans, while the remaining points still cover
 * the ring densely so the shockwave damage is preserved.
 *
 * <p>The ring's expansion rate, maximum radius, lifetime and the slam sound are
 * unchanged.
 */
@Pseudo
@Mixin(value = ShockwaveTickProcedure.class)
public abstract class HerobrineShockwaveTickMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/mcreator/herobrineprime/procedures/ShockwaveAttackProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V"))
    private static void calamitouscore$thinShockwaveRing(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (HerobrinePrimeOptimizations.shouldRunHeavyPoint()) {
            ShockwaveAttackProcedure.execute(world, x, y, z, entity);
        }
    }
}
