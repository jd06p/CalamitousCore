package dev.karma.calamitouscore.compat.fbt;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.util.RandomSource;

public final class TowerLootCheck {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        var boot = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
        boot.setAccessible(true);
        boot.setBoolean(null, true);
        Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        var constructor = TowerChestLoot.class.getDeclaredConstructor(LootItemCondition[].class);
        constructor.setAccessible(true);
        var modifier = constructor.newInstance((Object)new LootItemCondition[0]);
        var ctx = LootContext.class.getDeclaredConstructor(LootParams.class, RandomSource.class, LootDataResolver.class);
        ctx.setAccessible(true);
        for (String id : new String[]{"fbt:chests/last_floor", "fbt:chests/nether/last_floor", "fbt:chests/end/last_floor",
                "fbt:chests/obsidian_last_floor", "fbt:chests/megabattletower/boss_gold_chest", "minecraft:entities/wither",
                "minecraft:chests/ancient_city", "example:chests/last_floor", "fbt:entities/battle_tower_golem"}) {
            var context = ctx.newInstance(null, null, null);
            context.setQueriedLootTableId(new ResourceLocation(id));
            var first = new ItemStack(Items.DIAMOND, 3);
            var last = new ItemStack(Items.IRON_INGOT, 7);
            var loot = ObjectArrayList.of(new ItemStack(Items.NETHER_STAR), first, new ItemStack(Items.NETHER_STAR, 2), last);
            var result = modifier.apply(loot, context);
            boolean tower = id.startsWith("fbt:chests/");
            if (result != loot || result.size() != (tower ? 2 : 4) || result.get(tower ? 0 : 1) != first
                    || result.get(result.size() - 1) != last || first.getCount() != 3 || last.getCount() != 7)
                throw new AssertionError(id);
        }
        System.out.println("PASS: tower chest and unrelated loot contexts, stack identity, counts and order");
    }
}

