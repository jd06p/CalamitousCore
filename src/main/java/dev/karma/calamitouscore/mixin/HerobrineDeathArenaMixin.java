package dev.karma.calamitouscore.mixin;

import net.mcreator.herobrineprime.procedures.HerobrineDeathTimeIsReachedProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevents the post-fight cobblestone platform from generating.
 *
 * <p>This is a <em>separate</em> routine from the initial summon arena handled by
 * {@link HerobrineArenaSuppressMixin}. When Herobrine's death animation finishes
 * (420 ticks after death), {@code HerobrineDeathTimeIsReachedProcedure.execute}
 * runs a series of cleanup commands. Among them is:
 *
 * <pre>fill ~90 ~-3 ~90 ~-90 ~-3 ~-90 cobblestone</pre>
 *
 * <p>a single command that places a 181 x 181 (32,761 block) cobblestone
 * platform in one tick at the summon location. That enormous block operation is
 * both the source of the post-fight arena and a large single-tick server
 * workload.
 *
 * <p>This redirect drops only that command. Every other operation in the
 * procedure is preserved exactly, including:
 * <ul>
 *   <li>the shockwave entity spawned on death completion,</li>
 *   <li>the banner {@code title} commands,</li>
 *   <li>the worldborder restore,</li>
 *   <li>{@code gamerule doImmediateRespawn false},</li>
 *   <li>the {@code herobrine_prime:herobrine_head} reward drop.</li>
 * </ul>
 *
 * <p>The command is filtered by content, not by position, so unrelated
 * {@code fill} commands (or any other command) are unaffected, and no other mod
 * or vanilla terrain work is touched.
 */
@Pseudo
@Mixin(value = HerobrineDeathTimeIsReachedProcedure.class)
public abstract class HerobrineDeathArenaMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$skipDeathArenaFill(Commands commands, CommandSourceStack source, String command) {
        if (isDeathArenaFill(command)) {
            return 0;
        }
        return commands.performPrefixedCommand(source, command);
    }

    /**
     * Whether this command is the post-fight cobblestone platform fill. Matched
     * by content so only that single command is dropped.
     */
    private static boolean isDeathArenaFill(String command) {
        if (command == null) return false;
        String trimmed = command.trim();
        return trimmed.startsWith("fill ")
                && trimmed.endsWith(" cobblestone");
    }
}
