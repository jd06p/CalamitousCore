package dev.karma.calamitouscore.compat.fbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class TowerChestLoot extends LootModifier {
    private static final Codec<TowerChestLoot> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, TowerChestLoot::new));

    public static void register(IEventBus bus) {
        DeferredRegister<Codec<? extends IGlobalLootModifier>> modifiers =
                DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "calamitouscore");
        modifiers.register("tower_chest_loot", () -> CODEC);
        modifiers.register(bus);
    }

    private TowerChestLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<TowerChestLoot> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if ("fbt".equals(table.getNamespace()) && table.getPath().startsWith("chests/")) {
            loot.removeIf(stack -> stack.is(Items.NETHER_STAR));
        }
        return loot;
    }
}
