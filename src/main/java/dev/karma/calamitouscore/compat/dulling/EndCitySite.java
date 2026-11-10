package dev.karma.calamitouscore.compat.dulling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class EndCitySite {
    // Dulling 0.3.x ActiveBuild's native foundation bounds.
    private static final int FOUNDATION_RADIUS = 17;
    private static final int FOUNDATION_DEPTH = 44;
    private static final int VOID_CITY_FLOOR = 64;
    // The tallest shipped city templates (spire and worldbreaker) are 48 blocks tall.
    private static final int CITY_TEMPLATE_HEADROOM = 48;

    private EndCitySite() {}

    public static int foundingFloor(ServerLevel level, int x, int z) {
        int floor = Math.max(VOID_CITY_FLOOR, level.getMinBuildHeight() + FOUNDATION_DEPTH);
        // The native caller treats max build height as a rejected candidate.
        if (floor + CITY_TEMPLATE_HEADROOM > level.getMaxBuildHeight()
                || !hasVoidFootprint(level, x, z, false)) {
            return level.getMaxBuildHeight();
        }
        return floor;
    }

    public static boolean canBuild(ServerLevel level, int x, int floor, int z) {
        return floor - FOUNDATION_DEPTH >= level.getMinBuildHeight()
                && floor + CITY_TEMPLATE_HEADROOM <= level.getMaxBuildHeight()
                && hasVoidFootprint(level, x, z, true);
    }

    private static boolean hasVoidFootprint(ServerLevel level, int x, int z, boolean allowCityBlocks) {
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int chunkX = (x - FOUNDATION_RADIUS) >> 4; chunkX <= (x + FOUNDATION_RADIUS) >> 4; chunkX++) {
            for (int chunkZ = (z - FOUNDATION_RADIUS) >> 4; chunkZ <= (z + FOUNDATION_RADIUS) >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) return false;
                int minX = Math.max(x - FOUNDATION_RADIUS, chunkX << 4);
                int maxX = Math.min(x + FOUNDATION_RADIUS, (chunkX << 4) + 15);
                int minZ = Math.max(z - FOUNDATION_RADIUS, chunkZ << 4);
                int maxZ = Math.min(z + FOUNDATION_RADIUS, (chunkZ << 4) + 15);
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, bx & 15, bz & 15);
                        while (y >= minY) {
                            var state = chunk.getBlockState(pos.set(bx, y, bz));
                            // The city builder is allowed to join and upgrade its own foundations.
                            if (!state.isAir() && (!allowCityBlocks || !BuiltInRegistries.BLOCK
                                    .getKey(state.getBlock()).getNamespace().equals("thedulling"))) {
                                return false;
                            }
                            y--;
                        }
                    }
                }
            }
        }
        return true;
    }
}
