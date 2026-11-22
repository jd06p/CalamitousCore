package dev.karma.calamitouscore.mixin.performance;

import dev.karma.calamitouscore.compat.performance.ImmutablePositions;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "journeymap.client.model.chunk.ChunkMD", remap = false)
public abstract class JourneyMapPositionsMixin {
    @Shadow public abstract int toWorldX(int localX);
    @Shadow public abstract int toWorldZ(int localZ);
    /** @author CalamitousCore @reason Reuse immutable coordinate values without allocating an injection callback. */
    @Overwrite
    public BlockPos getBlockPos(int x, int y, int z) {
        return ImmutablePositions.at(toWorldX(x), y, toWorldZ(z));
    }
}
