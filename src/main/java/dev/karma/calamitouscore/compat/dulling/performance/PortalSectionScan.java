package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.*;
import java.util.function.Predicate;
import net.mcreator.thedulling.block.SeamlessPortalBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only sparse scan. A cache never outlives its rendered frame or level. */
public final class PortalSectionScan {
    private static final Predicate<BlockState> PORTAL = state -> state.getBlock() instanceof SeamlessPortalBlock;
    private static ClientLevel cachedLevel;
    private static int cachedFrame;
    private static BlockPos cachedMin, cachedMax;
    private static List<BlockPos> cached = List.of();
    private PortalSectionScan() {}

    public static Iterable<BlockPos> portals(ClientLevel level, BlockPos from, BlockPos to, int frame) {
        if (level == null) return List.of();
        if (level == cachedLevel && frame == cachedFrame && from.equals(cachedMin) && to.equals(cachedMax)) return cached;
        int minY = Math.max(Math.min(from.getY(), to.getY()), level.getMinBuildHeight());
        int maxY = Math.min(Math.max(from.getY(), to.getY()), level.getMaxBuildHeight() - 1);
        int minX = Math.min(from.getX(), to.getX()), maxX = Math.max(from.getX(), to.getX());
        int minZ = Math.min(from.getZ(), to.getZ()), maxZ = Math.max(from.getZ(), to.getZ());
        List<BlockPos> result = SparsePortalTraversal.collect((cx, sy, cz) -> {
            // Never request or generate a missing chunk. Empty palettes need no per-block reads.
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) return null;
            LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
            if (!section.maybeHas(PORTAL)) return null;
            return (x, y, z) -> PORTAL.test(section.getBlockState(x, y, z));
        }, minX, minY, minZ, maxX, maxY, maxZ);
        cachedLevel = level; cachedFrame = frame; cachedMin = from.immutable(); cachedMax = to.immutable(); cached = result;
        return result;
    }

    public static void clear() { cachedLevel = null; cachedMin = null; cachedMax = null; cached = List.of(); }
}
