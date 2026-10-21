package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.AttackProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales the decorative soul-fire particle that {@code AttackProcedure} emits
 * for every segment of an arc/melee attack.
 *
 * <p>{@code ArcAttackProcedure} steps an arc up to ~240 times and calls
 * {@code AttackProcedure.execute} once per segment; each call both scans nearby
 * entities (kept, it assigns damage) and dispatches a
 * {@code particle soul_fire_flame ... 30 force} command. The entity scan and
 * the damage assignment are preserved exactly; only the particle count is
 * reduced by the configured multiplier.
 */
@Pseudo
@Mixin(value = AttackProcedure.class)
public abstract class HerobrineAttackParticleMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$scaleAttackParticles(Commands commands, CommandSourceStack source, String command) {
        return HerobrinePrimeOptimizations.runOrdinary(commands, source, command);
    }
}
