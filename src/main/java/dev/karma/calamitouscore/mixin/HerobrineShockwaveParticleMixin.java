package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.ShockwaveAttackProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales the heavy shockwave-ring particle.
 *
 * <p>{@code ShockwaveTickProcedure} grows a ring and spawns
 * {@code 2*pi*radius} {@code particle explosion} points on it every tick; at
 * radius 64 that is roughly 400 commands per tick for many seconds. Each point
 * calls {@code ShockwaveAttackProcedure}, which dispatches the particle and
 * scans nearby entities (kept, it assigns the shockwave damage). Only the
 * particle count is reduced, using the heavy-effect multiplier.
 */
@Pseudo
@Mixin(value = ShockwaveAttackProcedure.class)
public abstract class HerobrineShockwaveParticleMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$scaleShockwaveParticles(Commands commands, CommandSourceStack source, String command) {
        return HerobrinePrimeOptimizations.runHeavy(commands, source, command);
    }
}
