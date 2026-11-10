package dev.karma.calamitouscore.compat.dulling;

import dev.karma.calamitouscore.DimensionGuard;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class EndDullingSpawns {
    private EndDullingSpawns() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockNewEntities(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() && event.getLevel() instanceof ServerLevel level
                && lockedEntity(level, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static boolean lockedEntity(ServerLevel level, Entity entity) {
        return Level.END.equals(level.dimension())
                && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("thedulling")
                && DimensionGuard.endLocked(level);
    }
}
