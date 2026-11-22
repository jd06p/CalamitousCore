package dev.karma.calamitouscore.compat.performance;

import com.fevzi.summonity.entity.SummonedMinion;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** Membership index, not a cached slot count: ownership, cost, range and accessibility are read live. */
public final class MinionSlotIndex {
    private static final Map<ServerLevel, WeakHashMap<Mob, Boolean>> LEVELS = new WeakHashMap<>();
    private MinionSlotIndex() {}
    private static WeakHashMap<Mob, Boolean> members(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> {
            WeakHashMap<Mob, Boolean> result = new WeakHashMap<>();
            for (Entity entity : level.getAllEntities()) if (entity instanceof Mob mob && mob instanceof SummonedMinion) result.put(mob, true);
            return result;
        });
    }
    public static int used(ServerLevel level, Player owner) {
        var box = owner.getBoundingBox().inflate(128);
        UUID id = owner.getUUID(); int used = 0;
        for (Mob mob : members(level).keySet()) {
            if (!mob.isRemoved() && level.getEntity(mob.getId()) == mob && mob.getBoundingBox().intersects(box)) {
                SummonedMinion minion = (SummonedMinion)mob;
                if (id.equals(minion.summonity$getOwnerId())) used += minion.summonity$getSlotCost();
            }
        }
        return used;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob && mob instanceof SummonedMinion) onServer(level, () -> members(level).put(mob, true));
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob) {
            onServer(level, () -> { var members = LEVELS.get(level); if (members != null) members.remove(mob); });
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) onServer(level, () -> LEVELS.remove(level)); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { LEVELS.clear(); }
    private static void onServer(ServerLevel level, Runnable change) {
        var server = level.getServer();
        if (server != null && !server.isSameThread()) server.execute(change); else change.run();
    }
}
