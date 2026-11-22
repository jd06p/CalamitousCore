package dev.karma.calamitouscore.compat.performance;

import java.util.function.Predicate;
import net.mcreator.thedulling.world.DullingWorldData.DullingChunkInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class DullingRecount {
    private DullingRecount() {}

    public static void count(LevelChunkSection[] sections, Predicate<BlockState> tagged, Block node, DullingChunkInfo info) {
        int blocks = 0;
        int nodes = 0;
        Predicate<BlockState> candidate = state -> tagged.test(state) || state.is(node);
        for (LevelChunkSection section : sections) {
            if (section.hasOnlyAir()) {
                // LevelChunk.getBlockState returns ordinary air for these sections.
                if (tagged.test(Blocks.AIR.defaultBlockState())) blocks += 4096;
                continue;
            }
            if (!section.maybeHas(candidate)) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var state = section.getBlockState(x, y, z);
                        if (tagged.test(state)) blocks++;
                        if (state.is(node)) nodes++;
                    }
                }
            }
        }
        info.dullBlockCount = blocks;
        info.dullNodeCount = nodes;
        info.isDulling = blocks >= 64 || nodes > 0;
    }
}

