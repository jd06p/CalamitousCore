package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.ExplosionAttackProcedure;
import net.mcreator.herobrineprime.procedures.ExplosionTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reduces the number of sampled points on Herobrine's expanding explosion
 * sphere.
 *
 * <p>{@code ExplosionTickProcedure.execute} computes
 * {@code numParticles = 4*pi*radius^2*8} and, for each point, calls
 * {@code ExplosionAttackProcedure.execute}, which spawns a particle and scans
 * for players. At the maximum radius this is over 25,000 points in a single
 * tick, and the count grows quadratically as the sphere expands. This redirect
 * skips a fraction of the points (see
 * {@link HerobrinePrimeOptimizations#shouldRunHeavyPoint()}) so both the
 * particle traffic and the entity scans are reduced proportionally, while the
 * remaining points still densely cover the sphere so the explosion damage is
 * preserved.
 *
 * <p>The sphere's expansion rate, maximum radius, lifetime and the explosion
 * sound are unchanged.
 */
@Pseudo
@Mixin(value = ExplosionTickProcedure.class)
public abstract class HerobrineExplosionTickMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/mcreator/herobrineprime/procedures/ExplosionAttackProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V"))
    private static void calamitouscore$thinExplosionSphere(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (HerobrinePrimeOptimizations.shouldRunHeavyPoint()) {
            ExplosionAttackProcedure.execute(world, x, y, z, entity);
        }
    }
}
