package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrineConfig;
import net.mcreator.herobrineprime.procedures.LineGeneratorProcedure;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reduces the point density of Herobrine's death-phase beams.
 *
 * <p>During the death sequence {@code LineGeneratorProcedure.drawBeams} draws
 * up to 15 beams per tick. Each beam is rendered by {@code spawnLine(...)} at a
 * hard-coded point density of {@code 5.0} points per block, and every point
 * dispatches a separate {@code particle} command. This redirect scales that
 * density by the configured ordinary particle multiplier so the beams keep
 * their shape and direction but are drawn with proportionally fewer points.
 *
 * <p>Only the density argument is changed; the number of beams, their origin,
 * their endpoints and the beam sound are untouched.
 */
@Pseudo
@Mixin(value = LineGeneratorProcedure.class)
public abstract class HerobrineBeamMixin {

    @Redirect(method = "drawBeams", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/mcreator/herobrineprime/procedures/LineGeneratorProcedure;spawnLine(Lnet/minecraft/world/level/LevelAccessor;[D[DD)V"))
    private static void calamitouscore$scaleBeamDensity(LevelAccessor world, double[] start, double[] end, double density) {
        double multiplier = HerobrineConfig.particleMultiplier();
        double scaled = density * multiplier;
        if (scaled < 1.0D) {
            scaled = 1.0D;
        }
        LineGeneratorProcedure.spawnLine(world, start, end, scaled);
    }
}
