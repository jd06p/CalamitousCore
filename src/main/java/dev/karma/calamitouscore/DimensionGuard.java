package dev.karma.calamitouscore;

import dev.karma.calamitouscore.compat.dulling.EndDullingProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class DimensionGuard {
    private DimensionGuard() {}
    public static boolean blocked(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (endLocked(serverLevel)) return true;
        ResourceLocation id = level.dimension().location();
        return id.toString().equals("crc:the_future")
            || id.toString().equals("roaring:dark_world")
            || id.toString().equals("forgottenman:tree_room");
    }

    public static boolean endLocked(ServerLevel level) {
        if (!Level.END.equals(level.dimension())) return false;
        return !EndDullingProgress.isUnlocked(level);
    }
}
