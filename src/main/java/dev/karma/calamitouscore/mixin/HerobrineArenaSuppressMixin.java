package dev.karma.calamitouscore.mixin;

import net.mcreator.herobrineprime.procedures.SummonProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Disables Herobrine Prime's automatic ritual-arena generation without
 * cancelling the ritual or the boss spawn.
 *
 * <p>Herobrine Prime's {@code SummonProcedure.execute} builds its arena by
 * running a fixed sequence of vanilla commands through
 * {@link Commands#performPrefixedCommand}. In order those are:
 * <pre>worldborder center ~0.5 ~0.5
 * worldborder set 181
 * fill ~90 ~-3 ~90 ~-90 ~-3 ~-90 bedrock
 * fill ~90 ~-2 ~90 ~-90 ~-2 ~-90 air
 * fill ~1 ~-2 ~1 ~-1 ~-2 ~-1 gold_block
 * setblock ~ ~-2 ~ mossy_cobblestone
 * fill ~90 ~-1 ~90 ~-90 ~-1 ~-90 air
 * setblock ... redstone_torch (x4)
 * setblock ~ ~-1 ~ netherrack
 * fill ~90 ~ ~90 ~-90 ~ ~-90 air
 * setblock ~ ~ ~ fire
 * kill @e[type=item,nbt={Item:{id:"minecraft:redstone_torch"}},limit=4,sort=nearest]
 * fill ~90 ~i ~90 ~-90 ~i ~-90 air  (repeated up to y=320)</pre>
 *
 * <p>These commands are what flatten the terrain, place the bedrock floor, the
 * gold platform, the torches/fire and clear a huge volume of air. They are also
 * a major source of lag. The boss itself is spawned afterwards by plain Java
 * code ({@code EntityType.spawn(...)}), never by a command, so skipping the
 * command calls preserves the ritual completion and the Herobrine spawn while
 * leaving the surrounding terrain completely untouched.
 *
 * <p>The redirect is scoped to {@code SummonProcedure.execute} only, so no other
 * mod or vanilla block placement is affected, and existing arenas are not
 * touched (this only affects future rituals).
 */
@Pseudo
@Mixin(value = SummonProcedure.class)
public abstract class HerobrineArenaSuppressMixin {

    @Redirect(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private static int calamitouscore$skipArenaCommands(Commands commands, CommandSourceStack source, String command) {
        if (isArenaCommand(command)) {
            return 0;
        }
        return commands.performPrefixedCommand(source, command);
    }

    /**
     * Whether a ritual command belongs to the automatic arena generation and
     * must therefore be skipped. Everything Herobrine Prime issues from the
     * summon procedure is arena-related, but the prefix check keeps the hook
     * tightly scoped in case the mod later adds unrelated commands.
     */
    private static boolean isArenaCommand(String command) {
        if (command == null) return false;
        String trimmed = command.trim();
        return trimmed.startsWith("fill ")
                || trimmed.startsWith("setblock ")
                || trimmed.startsWith("worldborder ")
                || trimmed.startsWith("kill ");
    }
}
