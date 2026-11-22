package dev.karma.calamitouscore.mixin.performance;

import dev.karma.calamitouscore.compat.dulling.performance.SparsePortalTraversal;
import net.mcreator.thedulling.block.AbstractFanBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.FanWindRenderer", remap = false)
public abstract class FanScanMixin {
    @Redirect(method = "scanActiveFans", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;", remap = true))
    private static Iterable<BlockPos> calamitouscore$fanCandidates(BlockPos min, BlockPos max, ClientLevel level, LocalPlayer player) {
        if (level.isDebug()) return BlockPos.betweenClosed(min, max);
        return SparsePortalTraversal.collect((cx, sy, cz) -> {
            var chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk == null) return null;
            var section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
            if (!section.maybeHas(state -> state.getBlock() instanceof AbstractFanBlock)) return null;
            return (x, y, z) -> section.getBlockState(x, y, z).getBlock() instanceof AbstractFanBlock;
        }, min.getX(), Math.max(min.getY(), level.getMinBuildHeight()), min.getZ(),
                max.getX(), Math.min(max.getY(), level.getMaxBuildHeight() - 1), max.getZ());
    }
}
