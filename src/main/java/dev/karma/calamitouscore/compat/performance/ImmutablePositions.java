package dev.karma.calamitouscore.compat.performance;

import net.minecraft.core.BlockPos;

/** Bounded, thread-local memoization of immutable coordinate values; stores no world/chunk objects. */
public final class ImmutablePositions {
    private static final int MASK = 16383;
    private static final ThreadLocal<BlockPos[]> CACHE = ThreadLocal.withInitial(() -> new BlockPos[MASK + 2]);
    private ImmutablePositions() {}
    public static BlockPos at(int x, int y, int z) {
        BlockPos[] cache = CACHE.get(); BlockPos recent = cache[MASK + 1];
        if (matches(recent, x, y, z)) return recent;
        int tile = (x >> 4) * 73428767 ^ (y >> 6) * 912931 ^ (z >> 4) * 438289;
        // Permute a whole 16 x 64 x 16 tile without collisions inside it.
        int local = (y & 63) << 8 | (z & 15) << 4 | x & 15;
        int index = (tile ^ (tile >>> 16) ^ local) & MASK;
        BlockPos old = cache[index];
        if (matches(old, x, y, z)) { cache[MASK + 1] = old; return old; }
        BlockPos next = new BlockPos(x, y, z); cache[index] = next; cache[MASK + 1] = next; return next;
    }
    private static boolean matches(BlockPos pos, int x, int y, int z) { return pos != null && pos.getX() == x && pos.getY() == y && pos.getZ() == z; }
}

