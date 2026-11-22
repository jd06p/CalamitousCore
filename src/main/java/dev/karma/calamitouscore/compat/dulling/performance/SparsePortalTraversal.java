package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.*;
import net.minecraft.core.BlockPos;

/** Primitive section traversal shared by the client adapter and constructor-free fixture tests. */
public final class SparsePortalTraversal {
    @FunctionalInterface public interface Blocks { boolean portal(int localX, int localY, int localZ); }
    @FunctionalInterface public interface Sections { Blocks portals(int chunkX, int sectionY, int chunkZ); }
    private static final Comparator<BlockPos> ORIGINAL_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getZ())
            .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX);
    private SparsePortalTraversal() {}
    public static List<BlockPos> collect(Sections sections, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List<BlockPos> result = new ArrayList<>();
        if (minY > maxY) return result;
        for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++)
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++)
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    Blocks blocks = sections.portals(cx, sy, cz);
                    if (blocks == null) continue;
                    for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++)
                        for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++)
                            for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++)
                                if (blocks.portal(x & 15, y & 15, z & 15)) result.add(new BlockPos(x, y, z));
                }
        result.sort(ORIGINAL_ORDER);
        return result;
    }
}
