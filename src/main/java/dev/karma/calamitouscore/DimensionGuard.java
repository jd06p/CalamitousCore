package dev.karma.calamitouscore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class DimensionGuard {
    private DimensionGuard() {}
    public static boolean blocked(Level level) {
        if (!(level instanceof ServerLevel)) return false;
        ResourceLocation id = level.dimension().location();
        return id.toString().equals("crc:the_future")
            || id.toString().equals("roaring:dark_world")
            || id.toString().equals("forgottenman:tree_room");
    }
}
