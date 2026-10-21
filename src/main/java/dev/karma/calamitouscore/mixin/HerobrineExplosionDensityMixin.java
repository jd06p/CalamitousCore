package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrineConfig;
import net.mcreator.herobrineprime.procedures.ExplosionTickProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Reduces the sampling density of Herobrine's expanding explosion sphere.
 *
 * <p>{@code ExplosionTickProcedure.execute} samples
 * {@code numParticles = 4*pi*radius^2*density} points every tick. The density
 * constant is a hard-coded {@code 8.0}, so at the maximum radius the loop runs
 * over 25,000 iterations in a single tick, each performing an entity query. The
 * count grows quadratically, making the "CRUSH" attack (which spawns an
 * explosion) the worst single-tick workload in the encounter.
 *
 * <p>This modifies that one constant so the density scales by the configured
 * heavy-effect multiplier (default 0.3). Because the loop count itself is
 * reduced, both the per-point particle commands and the per-point entity scans
 * shrink at the source, while the remaining points still uniformly cover the
 * sphere so the explosion damage is preserved.
 *
 * <p>The sphere's expansion rate, maximum radius and lifetime are unchanged.
 */
@Pseudo
@Mixin(value = ExplosionTickProcedure.class, remap = false)
public abstract class HerobrineExplosionDensityMixin {

    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 8.0D), remap = false)
    private static double calamitouscore$scaleExplosionDensity(double original) {
        double multiplier = HerobrineConfig.heavyEffectMultiplier();
        if (multiplier >= 1.0D) {
            return original;
        }
        if (multiplier <= 0.0D) {
            // Keep a minimal density so the sphere does not vanish entirely.
            return Math.min(original, 0.25D);
        }
        return original * multiplier;
    }
}
