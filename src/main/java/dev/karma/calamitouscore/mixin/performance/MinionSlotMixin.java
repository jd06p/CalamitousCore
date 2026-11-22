package dev.karma.calamitouscore.mixin.performance;

import dev.karma.calamitouscore.compat.performance.MinionSlotIndex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import com.fevzi.summonity.entity.SummonedMinion;
import java.util.List;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "com.fevzi.summonity.entity.MinionHelper", remap = false)
public abstract class MinionSlotMixin {
    @Shadow public static List<SummonedMinion> getOwnedMinions(ServerLevel level, Player owner) { throw new AssertionError(); }
    /** @author CalamitousCore @reason Count live indexed members; preserve the native path for off-thread callers. */
    @Overwrite
    public static int getUsedMinionSlots(ServerLevel level, Player owner) {
        return level.getServer().isSameThread() ? MinionSlotIndex.used(level, owner) : getOwnedMinions(level, owner).stream().mapToInt(SummonedMinion::summonity$getSlotCost).sum();
    }
}
