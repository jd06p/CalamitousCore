package dev.karma.calamitouscore.compat.performance;

import java.util.Arrays;
import net.mcreator.thedulling.world.DullingWorldData.DullingChunkInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

final class RecountFixture {
    static void run() throws Exception {
        var empty = section(Blocks.AIR);
        var stone = section(Blocks.STONE);
        var mixed = section(Blocks.STONE);
        var sections = new LevelChunkSection[24];
        Arrays.fill(sections, stone);
        sections[0] = empty;
        sections[1] = mixed;
        var result = new DullingChunkInfo();
        for (int i = 0; i <= 64; i++) {
            DullingRecount.count(sections, state -> state.is(Blocks.GOLD_BLOCK), Blocks.EMERALD_BLOCK, result);
            PerformanceCheck.require(result.dullBlockCount == i && result.dullNodeCount == 0 && result.isDulling == (i >= 64), "Changed infection threshold");
            mixed.setBlockState(i & 15, 0, i >> 4, Blocks.GOLD_BLOCK.defaultBlockState());
        }
        mixed.setBlockState(0, 1, 0, Blocks.EMERALD_BLOCK.defaultBlockState());
        DullingRecount.count(sections, state -> state.is(Blocks.EMERALD_BLOCK), Blocks.EMERALD_BLOCK, result);
        PerformanceCheck.require(result.dullBlockCount == 1 && result.dullNodeCount == 1 && result.isDulling, "Node/tag overlap");
        DullingRecount.count(sections, state -> state.is(Blocks.AIR), Blocks.EMERALD_BLOCK, result);
        PerformanceCheck.require(result.dullBlockCount == 4096 && result.dullNodeCount == 1, "Air section");
        PerformanceCheck.bench("24_section_recount_one_matching_section", () -> {
            int blocks = 0, nodes = 0;
            for (var section : sections) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                var state = section.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : section.getBlockState(x, y, z);
                if (state.is(Blocks.GOLD_BLOCK)) blocks++;
                if (state.is(Blocks.EMERALD_BLOCK)) nodes++;
            }
            if (blocks != 65 || nodes != 1) throw new AssertionError("Reference recount");
        }, () -> {
            DullingRecount.count(sections, state -> state.is(Blocks.GOLD_BLOCK), Blocks.EMERALD_BLOCK, result);
            if (result.dullBlockCount != 65 || result.dullNodeCount != 1) throw new AssertionError("Section recount");
        }, 128);
    }
    private static LevelChunkSection section(Block block) {
        return new LevelChunkSection(new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, block.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES), null);
    }
}
