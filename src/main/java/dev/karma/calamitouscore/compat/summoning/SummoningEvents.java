package dev.karma.calamitouscore.compat.summoning;

import com.fevzi.summonity.entity.MinionHelper;
import com.fevzi.summonity.registry.ModAttributes;
import java.util.List;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.paisleypug.terramitypaisleyssummoning.init.TerramityPaisleysSummoningModAttributes;

/** Registered only when both addons, and not the obsolete standalone bridge, are loaded. */
public final class SummoningEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void player(TickEvent.PlayerTickEvent event) {
        SharedSummonState.bind(event.player);
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        reconcile(event.player);
    }
    private static void reconcile(Player player) {
        SharedSummonState.bind(player);
        AttributeInstance legacy = player.getAttribute((Attribute) TerramityPaisleysSummoningModAttributes.SUMMONSLOTS.get());
        AttributeInstance shared = player.getAttribute((Attribute) ModAttributes.MINION_SLOTS.get());
        SummonEquipmentBridge.migrateSlots(legacy, shared);
        SummonEquipmentBridge.update(player); SharedSummonState.sync(player);
    }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void login(PlayerEvent.PlayerLoggedInEvent event) { reconcile(event.getEntity()); }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void respawn(PlayerEvent.PlayerRespawnEvent event) { reconcile(event.getEntity()); }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { reconcile(event.getEntity()); }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity(); if (entity.level().isClientSide) return;
        if (entity instanceof TamableAnimal animal) SummonCombatBridge.update(animal);
        if (SharedSummonState.nativeMinion(entity)) SummonEquipmentBridge.nativeDefenses(entity);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void safety(LivingAttackEvent event) {
        Entity actor = SharedSummonState.actor(event.getSource());
        if (SharedSummonState.paisley(actor)) {
            Player owner = SharedSummonState.owner(actor);
            if (owner == null || !MinionHelper.isValidTarget(owner, event.getEntity())) event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void damage(LivingHurtEvent event) {
        Entity actor = SharedSummonState.actor(event.getSource());
        if (SummonCombatBridge.primary(actor, event.getSource())) event.setAmount(SummonCombatBridge.damage(actor, event.getEntity(), event.getAmount()));
    }
    @SubscribeEvent public static void record(LivingDamageEvent event) {
        Entity actor = SharedSummonState.actor(event.getSource());
        if (!event.isCanceled() && SummonCombatBridge.primary(actor, event.getSource())) SummonCombatBridge.record(actor, event.getEntity(), event.getSource(), event.getAmount());
        if (!event.isCanceled() && event.getSource().getEntity() instanceof Player player && event.getSource().getDirectEntity() == player
                && player.level() instanceof net.minecraft.server.level.ServerLevel level && SummonEquipmentBridge.staff(player.getMainHandItem()) && MinionHelper.isValidTarget(player, event.getEntity()))
            MinionHelper.focusTarget(level, player, event.getEntity());
    }
}
