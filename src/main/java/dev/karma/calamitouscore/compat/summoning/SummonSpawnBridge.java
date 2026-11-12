package dev.karma.calamitouscore.compat.summoning;

import com.fevzi.summonity.entity.*;
import com.fevzi.summonity.player.SummonityPlayerData;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import net.paisleypug.terramitypaisleyssummoning.procedures.*;
import top.theillusivec4.curios.api.SlotContext;

/** Carries the real caster through MCreator's synchronous spawn/finalizeSpawn procedure calls. */
public final class SummonSpawnBridge {
    private static final ThreadLocal<Player> CASTER = new ThreadLocal<>();
    private static final ThreadLocal<String> ACCESSORY = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ACCESSORY_BUDGET = new ThreadLocal<>();
    private static final String ACCESSORY_TAG = "CalamitousCoreSummonAccessory";
    private SummonSpawnBridge() {}
    public static Entity spawn(EntityType<?> type, ServerLevel level, BlockPos position, MobSpawnType reason, Entity source) {
        Player owner = source == null ? CASTER.get() : SharedSummonState.owner(source);
        var role = SummoningRoles.ENTITIES.get(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(type)));
        // Some artifact procedures also create vanilla lightning/effect entities. They are not minions.
        if (role == null) return type.spawn(level, position, reason);
        if (owner == null || !owner.isAlive() || owner.level() != level) return null;
        boolean accessory = ACCESSORY.get() != null && role.cost() == 0;
        if (accessory && ACCESSORY_BUDGET.get() != null && ACCESSORY_BUDGET.get() <= 0) return null;
        if (role.cost() > 0) {
            int cap = SummonityPlayerData.getMinionSlots(owner);
            if (cap < role.cost()) return null;
            MinionHelper.removeOldestMinions(level, owner, Math.max(0, SharedSummonState.used(owner) + role.cost() - cap));
        }
        Player previous = CASTER.get(); CASTER.set(owner);
        try {
            Entity result = type.spawn(level, position, reason);
            if (result instanceof TamableAnimal animal) animal.tame(owner);
            if (result instanceof SummonedMinion minion) MinionHelper.inheritFocus(level, owner, minion);
            if (result != null && ACCESSORY.get() != null) result.getPersistentData().putString(ACCESSORY_TAG, ACCESSORY.get());
            if (result != null && accessory && ACCESSORY_BUDGET.get() != null) ACCESSORY_BUDGET.set(ACCESSORY_BUDGET.get() - 1);
            SharedSummonState.sync(owner); return result;
        } finally { restore(CASTER, previous); }
    }
    private static <T> void restore(ThreadLocal<T> local, T previous) { if (previous == null) local.remove(); else local.set(previous); }
    public static void tame(TamableAnimal animal, Player nearest) {
        Player caster = CASTER.get(); if (caster != null) animal.tame(caster); else if (nearest != null) animal.tame(nearest);
    }
    public static boolean dismiss(Entity entity, String typeName) {
        if (!(entity instanceof Player player) || !player.isShiftKeyDown()) return false;
        if (player.level() instanceof ServerLevel level) {
            MinionHelper.getOwnedMinions(level, player).stream().filter(m -> ((Entity)m).getType().getDescriptionId().equals(typeName)).findFirst().ifPresent(m -> ((Entity)m).discard());
            SharedSummonState.sync(player);
        }
        return true;
    }
    public static void clear(Entity entity) {
        if (entity instanceof Player player && player.level() instanceof ServerLevel level) {
            MinionHelper.getOwnedMinions(level, player).stream().filter(m -> m.summonity$getSlotCost() > 0).forEach(m -> ((Entity)m).discard());
            SharedSummonState.sync(player);
        }
    }
    public static void accessory(LevelAccessor world, double x, double y, double z, SlotContext slot, String procedure) {
        if (!(slot.entity() instanceof Player owner) || !(owner.level() instanceof ServerLevel level)) return;
        boolean cube = procedure.startsWith("Cube"); String id = "terramity_paisleys_summoning:" + (cube ? "orbital" : "fuzzy");
        int count = procedure.startsWith("FuzzstersRingPlus") ? 2 : 1;
        String key = slot.identifier() + ":" + slot.index();
        var existing = MinionHelper.getOwnedMinions(level, owner).stream().map(m -> (Entity)m)
                .filter(e -> id.equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType())))).toList();
        if (procedure.contains("Unequipped")) {
            var tagged = existing.stream().filter(e -> key.equals(e.getPersistentData().getString(ACCESSORY_TAG))).toList();
            if (!tagged.isEmpty()) tagged.forEach(Entity::discard);
            else existing.stream().filter(e -> !e.getPersistentData().contains(ACCESSORY_TAG)).limit(count).forEach(Entity::discard);
            return;
        }
        long present = existing.stream().filter(e -> key.equals(e.getPersistentData().getString(ACCESSORY_TAG))).count();
        if (present >= count) return;
        // Adopt old unmarked companions on first equip after migration, rather than duplicating them.
        for (Entity old : existing) if (present < count && !old.getPersistentData().contains(ACCESSORY_TAG)) { old.getPersistentData().putString(ACCESSORY_TAG, key); present++; }
        if (present >= count) return;
        Player previous = CASTER.get(); String oldAccessory = ACCESSORY.get(); Integer oldBudget = ACCESSORY_BUDGET.get();
        CASTER.set(owner); ACCESSORY.set(key); ACCESSORY_BUDGET.set((int)(count - present));
        try {
            switch (procedure) {
                case "CubeOfMeatBaubleIsEquippedProcedure" -> CubeOfMeatBaubleIsEquippedProcedure.execute(world, x, y, z);
                case "FuzzsterRingBaubleIsEquippedProcedure" -> FuzzsterRingBaubleIsEquippedProcedure.execute(world, x, y, z);
                case "FuzzstersRingPlusBaubleIsEquippedProcedure" -> FuzzstersRingPlusBaubleIsEquippedProcedure.execute(world, x, y, z);
                default -> throw new IllegalArgumentException(procedure);
            }
        } finally { restore(CASTER, previous); restore(ACCESSORY, oldAccessory); restore(ACCESSORY_BUDGET, oldBudget); }
    }
}
