package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.HerobrineAIProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Optimizes the commands Herobrine's AI loop dispatches every game tick.
 *
 * <p>{@code HerobrineAIProcedure.execute} runs every tick while the boss is
 * alive and re-issues several commands unconditionally. Through the single
 * redirect below those are filtered by
 * {@link HerobrinePrimeOptimizations#runAiCommand}:
 * <ul>
 *   <li>{@code spawnpoint @a ~ ~ ~} is executed every tick – dropped, since the
 *       spawn point never moves.</li>
 *   <li>{@code stopsound @a music} is executed every tick – dropped.</li>
 *   <li>{@code bossbar set ... value <n>} is refreshed every tick – throttled to
 *       a few times per second (the client bossbar interpolates).</li>
 *   <li>the phase-2 {@code particle soul_fire_flame ... 64 force} aura is issued
 *       every tick at a very large count – throttled and count-scaled.</li>
 * </ul>
 *
 * <p>Every other command the AI issues (chat {@code tellraw}, {@code playsound},
 * {@code bossbar add/set color/set max/set players}, the intro lightning,
 * etc.) is passed through completely unchanged, so all behaviour, dialogue,
 * music cues and bossbar progression are preserved.
 */
@Pseudo
@Mixin(value = HerobrineAIProcedure.class)
public abstract class HerobrineAiCommandMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$optimizeAiCommand(Commands commands, CommandSourceStack source, String command) {
        return HerobrinePrimeOptimizations.runAiCommand(commands, source, command);
    }
}
