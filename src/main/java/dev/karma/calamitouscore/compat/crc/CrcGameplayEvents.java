package dev.karma.calamitouscore.compat.crc;

import com.TBK.crc.server.StructureData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class CrcGameplayEvents {
    private static final String EXPIRY = "CalamitousCorePortalExpiry";
    private static final ResourceLocation PORTAL = new ResourceLocation("crc", "portal");
    private static final java.util.Set<Entity> PORTALS = new java.util.HashSet<>();
    private static boolean portal(Entity e) { return PORTAL.equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType())); }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity e = event.getEntity();
        if (portal(e)) {
            long now = level.getServer().overworld().getGameTime();
            if (!e.getPersistentData().contains(EXPIRY)) e.getPersistentData().putLong(EXPIRY, now + 1200);
            PORTALS.add(e);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!event.isCanceled()) remove(event.getEntity());
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide) PORTALS.remove(event.getEntity());
        Entity.RemovalReason reason = event.getEntity().getRemovalReason();
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) remove(event.getEntity());
    }
    private static void remove(Entity e) {
        if (e.level() instanceof ServerLevel level && e.getPersistentData().hasUUID(CrcRaidData.OWNER))
            CrcRaidData.get(level.getServer()).remove(e.getPersistentData().getUUID(CrcRaidData.OWNER), e.getUUID());
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModList.get().isLoaded("crc")) return;
        // A single, authoritative server tick, independent of CRC's generic TickEvent listener.
        if (event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation("crc", "the_future"))) != null)
            ((CrcFightTimer) StructureData.get().getCyberChickenFight()).calamitouscore$serverTick();
        long now = event.getServer().overworld().getGameTime();
        for (Entity e : java.util.List.copyOf(PORTALS))
            if (!e.isRemoved() && e.getPersistentData().getLong(EXPIRY) <= now) e.discard();
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event) { PORTALS.clear(); }
}
