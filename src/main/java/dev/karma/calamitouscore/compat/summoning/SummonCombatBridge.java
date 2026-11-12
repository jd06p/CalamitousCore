package dev.karma.calamitouscore.compat.summoning;

import com.fevzi.summonity.entity.*;
import com.fevzi.summonity.event.*;
import com.fevzi.summonity.item.GildedScarabItem;
import com.fevzi.summonity.player.SummonityPlayerData;
import com.fevzi.summonity.registry.ModDamageTypes;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class SummonCombatBridge {
    private static final ThreadLocal<Boolean> SECONDARY = ThreadLocal.withInitial(() -> false);
    private static final UUID SPEED = UUID.fromString("8c15e28d-9587-447a-9a94-18c9caad8204");
    private static final UUID RANGE = UUID.fromString("dc4a58dc-3dba-4fe9-8abc-abd4b64d72b9");
    private SummonCombatBridge() {}
    public static boolean secondary() { return SECONDARY.get(); }
    public static boolean primary(Entity actor, DamageSource source) { return SharedSummonState.paisley(actor) && !SECONDARY.get() && !source.is(ModDamageTypes.MINION_ATTACK); }
    public static float damage(Entity actor, LivingEntity target, float amount) {
        Player owner = SharedSummonState.owner(actor);
        if (owner == null) return amount;
        float result = (float)(amount * SummonityPlayerData.getMinionDamage(owner));
        result = WheelOfArcanaEvents.modifyMinionDamage(owner, target, result);
        result = EmberLocketEvents.modifyMinionDamage(owner, target, result);
        return owner.level() instanceof ServerLevel level ? GildedScarabItem.tryCriticalHit(level, owner, target, result) : result;
    }
    public static void record(Entity actor, LivingEntity target, DamageSource source, float amount) {
        if (!(amount > 0) || !(actor instanceof SummonedMinion minion) || !(actor.level() instanceof ServerLevel level)) return;
        Player owner = SharedSummonState.owner(actor); if (owner == null || SECONDARY.get()) return;
        SECONDARY.set(true);
        try {
            GildedScarabItem.knockBackTarget(owner, (LivingEntity)actor, target);
            ConductiveBatteryEvents.recordMinionHit(level, target, owner, source, amount);
            FuelRodEvents.recordMinionHit(level, target, owner);
            EmberLocketEvents.recordMinionHit(level, target, owner, source, amount);
            SummonerMaskEvents.recordMinionHit(level, target, owner, source, amount);
            WheelOfArcanaEvents.recordMinionHit(level, target, owner, source, amount);
            SpectralWhistleEvents.recordMinionHit(level, target, owner, minion, amount);
            PrismaticLanternEvents.recordMinionHit(level, target, owner);
        } finally { SECONDARY.remove(); }
    }
    public static void update(TamableAnimal minion) {
        if (!SharedSummonState.paisley(minion)) return;
        Player owner = SharedSummonState.owner(minion);
        if (owner == null || !owner.isAlive()) { minion.discard(); return; }
        SummonEquipmentBridge.reconcile(minion.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, "Shared summon movement", SummonityPlayerData.getMinionMovementSpeed(owner) - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        AttributeInstance range = minion.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) SummonEquipmentBridge.reconcile(range, RANGE, "Shared summon range", SummonityPlayerData.getMinionRange(owner) - range.getBaseValue(), AttributeModifier.Operation.ADDITION);
        SummonedMinion shared = (SummonedMinion) minion;
        LivingEntity focus = shared.getCommandedTarget();
        if (focus != null && MinionHelper.isValidTarget(owner, focus) && minion.distanceToSqr(focus) <= Math.pow(SummonityPlayerData.getMinionRange(owner), 2)) minion.setTarget(focus);
        else if (minion.getTarget() != null && (!MinionHelper.isValidTarget(owner, minion.getTarget()) || minion.distanceToSqr(minion.getTarget()) > Math.pow(SummonityPlayerData.getMinionRange(owner),2))) minion.setTarget(null);
    }
    public static int cooldown(PathfinderMob mob, int original) { Player owner = SharedSummonState.owner(mob); return SharedSummonState.paisley(mob) && owner != null ? MinionHelper.adjustAttackCooldown(owner, original) : original; }
    public static int attackRoll(RandomSource random, int min, int max, Entity entity) {
        Player owner = SharedSummonState.owner(entity);
        if (owner != null && min == 1 && max > 2) return random.nextDouble() < Math.min(1, SummonityPlayerData.getMinionAttackSpeed(owner) / (max - min + 1)) ? 2 : 1;
        return Mth.nextInt(random, min, max);
    }
    public static void velocity(Entity entity, Vec3 velocity) {
        Player owner = SharedSummonState.owner(entity); var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (owner != null && id != null && (id.getPath().equals("summoned_fairy") || id.getPath().equals("flare_fairy"))) velocity = velocity.scale(SummonityPlayerData.getMinionMovementSpeed(owner));
        entity.setDeltaMovement(velocity);
    }
}
