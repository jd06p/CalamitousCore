package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.ExplosionAttackProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales the heavy explosion-sphere particle.
 *
 * <p>{@code ExplosionTickProcedure} grows a sphere and spawns
 * {@code 4*pi*radius^2*8} points on it every tick; at radius 16 that is over
 * 25,000 {@code particle} commands in a single tick, growing quadratically.
 * Each point calls {@code ExplosionAttackProcedure}, which dispatches the
 * particle and scans nearby entities (kept, it assigns the explosion damage).
 * Only the particle count is reduced, using the (stronger) heavy-effect
 * multiplier.
 */
@Pseudo
@Mixin(value = ExplosionAttackProcedure.class)
public abstract class HerobrineExplosionParticleMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$scaleExplosionParticles(Commands commands, CommandSourceStack source, String command) {
        return HerobrinePrimeOptimizations.runHeavy(commands, source, command);
    }
}
