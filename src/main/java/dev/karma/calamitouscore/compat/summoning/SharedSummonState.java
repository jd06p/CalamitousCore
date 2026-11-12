package dev.karma.calamitouscore.compat.summoning;

import com.fevzi.summonity.entity.*;
import com.fevzi.summonity.player.SummonityPlayerData;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.registries.ForgeRegistries;
import net.paisleypug.terramitypaisleyssummoning.network.TerramityPaisleysSummoningModVariables;

public final class SharedSummonState {
    private static final Map<TerramityPaisleysSummoningModVariables.PlayerVariables, WeakReference<Player>> OWNERS = Collections.synchronizedMap(new WeakHashMap<>());
    private SharedSummonState() {}
    public static boolean paisley(Entity entity) {
        return entity instanceof SummonedMinion && SummoningRoles.ENTITIES.containsKey(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())));
    }
    public static boolean nativeMinion(Entity entity) {
        return entity instanceof SummonedMinion && "summonity".equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getNamespace());
    }
    public static Player owner(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof SummonedMinion minion) return minion.getOwner();
        return null;
    }
    public static boolean liveSummon(Entity entity) {
        return (paisley(entity) || nativeMinion(entity)) && owner(entity) != null && owner(entity).isAlive() && !entity.level().isClientSide;
    }
    public static boolean loadedOwner(Entity entity) {
        return !(entity instanceof SummonedMinion) || liveSummon(entity) && !SummonCombatBridge.secondary();
    }
    public static Entity actor(DamageSource source) {
        Entity direct = actor(source.getDirectEntity());
        if (direct instanceof SummonedMinion) return direct;
        return actor(source.getEntity());
    }
    public static Entity actor(Entity entity) {
        for (int depth = 0; depth < 4 && entity instanceof Projectile projectile; depth++) {
            Entity owner = projectile.getOwner(); if (owner == entity) break; entity = owner;
        }
        return entity;
    }
    public static int cost(Entity entity) {
        var role = SummoningRoles.ENTITIES.get(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())));
        return role == null ? 0 : role.cost();
    }
    public static void bind(Entity entity) {
        Player player = owner(entity);
        if (player != null) player.getCapability(TerramityPaisleysSummoningModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(vars -> OWNERS.put(vars, new WeakReference<>(player)));
    }
    public static int used(Player player) { return player.level() instanceof ServerLevel level ? MinionHelper.getUsedMinionSlots(level, player) : 0; }
    public static double read(TerramityPaisleysSummoningModVariables.PlayerVariables vars) {
        var reference = OWNERS.get(vars); Player player = reference == null ? null : reference.get();
        return player != null && !player.level().isClientSide ? used(player) : vars.currentSummons;
    }
    public static double admission(TerramityPaisleysSummoningModVariables.PlayerVariables vars, int cost) {
        var reference = OWNERS.get(vars); Player player = reference == null ? null : reference.get();
        if (player == null) return read(vars);
        int cap = SummonityPlayerData.getMinionSlots(player);
        return cap < cost ? cap : Math.min(read(vars), cap - cost);
    }
    public static void ignoreWrite(TerramityPaisleysSummoningModVariables.PlayerVariables vars, double value) {}
    public static void sync(Player player) {
        bind(player);
        if (!player.level().isClientSide) player.getCapability(TerramityPaisleysSummoningModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(vars -> {
            int count = used(player); if (vars.currentSummons != count) { vars.currentSummons = count; vars.syncPlayerVariables(player); }
        });
    }
    public static boolean killEligible(Entity entity) {
        return nativeMinion(entity) || entity instanceof net.paisleypug.terramitypaisleyssummoning.entity.DwarfGnomeEntity;
    }
    public static boolean hasOwner(SummonedMinion entity) { return entity.summonity$getOwnerId() != null && entity.getOwner() != null; }
}
