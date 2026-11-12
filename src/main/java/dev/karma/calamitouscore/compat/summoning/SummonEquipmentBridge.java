package dev.karma.calamitouscore.compat.summoning;

import com.fevzi.summonity.entity.MinionHelper;
import com.fevzi.summonity.player.SummonityPlayerData;
import com.fevzi.summonity.registry.ModAttributes;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

public final class SummonEquipmentBridge {
    public static final UUID[] LEGACY_DAMAGE = { UUID.fromString("535db642-9933-4847-9011-39b081602cca"), UUID.fromString("1bc76ec4-d877-4a9a-916b-a053552b6e10"), UUID.fromString("3ced3747-2ec2-4ed1-b1c2-eb6939b0269b"), UUID.fromString("a5ddedf8-28c6-4e28-9365-12c3e400c400") };
    private static final Set<String> HUD_ITEMS = Set.of("cube_of_meat", "fuzzsters_ring", "fuzzsters_ring_plus", "tiny_bracelets", "typos_golden_bana", "typos_prismatic_banana", "typos_evil_banana", "banana_necklace", "gnome_king_set_helmet", "gnome_king_set_chestplate", "gnome_king_set_leggings", "gnome_king_set_boots");
    public static final UUID WEAPON_PENALTY = UUID.fromString("cd0117dc-a97d-4f87-9c48-816c4338b5de");
    private static final UUID HEALTH = UUID.fromString("b30b2d26-2b41-4787-8859-554e1f31017c"), RESISTANCE = UUID.fromString("d68fd4fb-2937-44bd-a625-8406580586f1"), KNOCKBACK = UUID.fromString("d509f0e5-11b7-45ef-847c-9c0f1b8ea223");
    private SummonEquipmentBridge() {}
    public static boolean equipped(Player player, String path) {
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("terramity_paisleys_summoning", path));
        return item != null && CuriosApi.getCuriosInventory(player).map(inventory -> inventory.isEquipped(item)).orElse(false);
    }
    public static boolean staff(ItemStack stack) { return !stack.isEmpty() && SummoningRoles.STAFFS.contains(stack.getItem().getClass().getName()); }
    public static boolean hud(ItemStack stack) {
        if (staff(stack)) return true;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("terramity_paisleys_summoning") && HUD_ITEMS.contains(id.getPath());
    }
    public static void update(Player player) {
        double[] amounts = {equipped(player,"emblem_of_power") ? .15 : 0, equipped(player,"emblem_of_power_plus") ? .25 : 0,
                equipped(player,"arcane_ironslag") ? .25 : 0, equipped(player,"pyrite_ferrofragment") ? .15 * Math.max(0, SummonityPlayerData.getMinionSlots(player) - SharedSummonState.used(player)) : 0};
        AttributeInstance damage = player.getAttribute((Attribute) ModAttributes.MINION_DAMAGE.get());
        for (int i = 0; i < amounts.length; i++) reconcile(damage, LEGACY_DAMAGE[i], "Paisley shared minion damage", amounts[i], AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (staff(player.getMainHandItem()) && damage != null) damage.removeModifier(WEAPON_PENALTY);
    }
    public static void stripLegacy(Entity entity) {
        if (SharedSummonState.paisley(entity) && entity instanceof LivingEntity living) {
            var damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) for (UUID id : LEGACY_DAMAGE) damage.removeModifier(id);
        }
    }
    public static void nativeDefenses(LivingEntity minion) {
        Player owner = SharedSummonState.owner(minion);
        // These three general-purpose bonuses already apply to Paisley entities in their native tick procedure.
        reconcile(minion.getAttribute(Attributes.MAX_HEALTH), HEALTH, "Gnome King's Emblem summon health", owner != null && equipped(owner,"gnome_kings_emblem") ? 20 : 0, AttributeModifier.Operation.ADDITION);
        reconcile(minion.getAttribute(Attributes.KNOCKBACK_RESISTANCE), RESISTANCE, "Trial Emblem summon resistance", owner != null && equipped(owner,"trial_emblem") ? .1 : 0, AttributeModifier.Operation.ADDITION);
        reconcile(minion.getAttribute(Attributes.ATTACK_KNOCKBACK), KNOCKBACK, "Echo Emblem summon knockback", owner != null && equipped(owner,"echo_emblem") ? .2 : 0, AttributeModifier.Operation.ADDITION);
        if (minion.getHealth() > minion.getMaxHealth()) minion.setHealth(minion.getMaxHealth());
    }
    public static void reconcile(AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (current != null && current.getAmount() == amount && current.getOperation() == operation && amount != 0) return;
        if (current != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }
    public static void migrateSlots(AttributeInstance legacy, AttributeInstance shared) {
        if (legacy == null || shared == null) return;
        for (AttributeModifier modifier : java.util.List.copyOf(legacy.getModifiers())) {
            if (shared.getModifier(modifier.getId()) == null) shared.addTransientModifier(modifier);
            legacy.removeModifier(modifier.getId());
        }
    }
}
