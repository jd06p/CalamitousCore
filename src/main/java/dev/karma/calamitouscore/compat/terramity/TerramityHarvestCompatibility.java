package dev.karma.calamitouscore.compat.terramity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Complements Terramity's item allowlists; never replaces its per-block tier checks. */
@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class TerramityHarvestCompatibility {
    private static final TagKey<Item> COMPATIBLE_PICKAXES = ItemTags.create(
            new ResourceLocation("calamitouscore", "terramity/compatible_pickaxes"));
    private static final TagKey<Block> NEEDS_NETHERITE = BlockTags.create(
            new ResourceLocation("forge", "needs_netherite_tool"));

    private TerramityHarvestCompatibility() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void harvest(PlayerEvent.HarvestCheck event) {
        if (event.canHarvest()) return;
        BlockState state = event.getTargetBlock();
        if (!"terramity".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())
                || !state.is(BlockTags.MINEABLE_WITH_PICKAXE) || !state.is(NEEDS_NETHERITE)) return;
        ItemStack tool = event.getEntity().getMainHandItem();
        if (!tool.is(COMPATIBLE_PICKAXES) || tool.isCorrectToolForDrops(state)) return;
        // Forge 47.4.10 invokes this inside the superclass harvest check. Terramity's
        // canHarvestBlock overrides subsequently AND this with their original tier tag.
        // Some assigned tools have native level 3; do not mutate their shared Tier or stats.
        event.setCanHarvest(true);
    }
}
