package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.*;
import net.mcreator.thedulling.entity.GreatDevourerEntity;
import net.mcreator.thedulling.entity.SeraphimEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "calamitouscore", value = Dist.CLIENT)
public final class DullingFrameQueries {
    private static ClientLevel level;
    private static AABB seraphBox;
    private static List<SeraphimEntity> seraphs;
    private static Map<UUID, List<GreatDevourerEntity>> holders;
    private DullingFrameQueries() {}

    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            level = null; seraphBox = null; seraphs = null; holders = null;
            PortalSectionScan.clear();
        }
    }
    private static void checkLevel(ClientLevel current) {
        if (level != current) { level = current; seraphBox = null; seraphs = null; holders = null; }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> seraphs(Level world, Class<T> type, AABB box) {
        if (!(world instanceof ClientLevel client) || type != SeraphimEntity.class) return world.getEntitiesOfClass(type, box);
        checkLevel(client);
        if (seraphs == null || !box.equals(seraphBox)) { seraphBox = box; seraphs = client.getEntitiesOfClass(SeraphimEntity.class, box); }
        return (List<T>)(List<?>)seraphs;
    }

    public static GreatDevourerEntity holder(LivingEntity victim) {
        if (!(victim.level() instanceof ClientLevel client)) return null;
        checkLevel(client);
        if (holders == null) {
            holders = new HashMap<>();
            for (Entity entity : client.entitiesForRendering()) if (entity instanceof GreatDevourerEntity devourer)
                devourer.getSyncedDevouredTargetUuid().ifPresent(id -> holders.computeIfAbsent(id, ignored -> new ArrayList<>()).add(devourer));
        }
        List<GreatDevourerEntity> candidates = holders.get(victim.getUUID());
        if (candidates == null) return null;
        AABB box = victim.getBoundingBox().inflate(32);
        if (candidates.size() == 1) {
            GreatDevourerEntity devourer = candidates.get(0);
            return EntitySelector.NO_SPECTATORS.test(devourer) && devourer.getBoundingBox().intersects(box) ? devourer : null;
        }
        // Preserve native section query ordering if several devourers claim the same victim.
        for (GreatDevourerEntity devourer : client.getEntitiesOfClass(GreatDevourerEntity.class, box))
            if (devourer.getSyncedDevouredTargetUuid().filter(victim.getUUID()::equals).isPresent()) return devourer;
        return null;
    }
}
