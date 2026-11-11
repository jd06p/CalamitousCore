package dev.karma.calamitouscore.ownership;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative protection based on explicit ownership and damage attribution. */
@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class OwnerDamageProtection {
    private OwnerDamageProtection() {}

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!event.isCanceled() && protects(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    // Some modded hurt overrides fire this hook without first firing LivingAttackEvent.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!event.isCanceled() && protects(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    static boolean protects(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide || victim instanceof Player) return false;
        UUID owner = ownerUUID(victim);
        if (owner == null) return false;

        // The causing entity is authoritative. Only fall back when it was not supplied.
        Entity responsible = source.getEntity();
        if (responsible == null) responsible = source.getDirectEntity();
        Player player = responsiblePlayer(responsible);
        return player != null && owner.equals(player.getUUID());
    }

    private static UUID ownerUUID(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null) return owner;
            if (ownable.getOwner() instanceof Player player) return player.getUUID();
        }
        if (entity instanceof TraceableEntity traceable && traceable.getOwner() instanceof Player player) {
            return player.getUUID();
        }
        return null;
    }

    private static Player responsiblePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (!(entity instanceof TraceableEntity)) return null;

        Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (entity instanceof TraceableEntity traceable && visited.add(entity)) {
            entity = traceable.getOwner();
            if (entity instanceof Player player) return player;
        }
        return null;
    }
}
