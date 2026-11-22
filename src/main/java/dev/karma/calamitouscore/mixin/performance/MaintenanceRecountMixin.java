package dev.karma.calamitouscore.mixin.performance;

import net.mcreator.thedulling.init.ThedullingModBlocks;
import net.mcreator.thedulling.world.DullingWorldData.DullingChunkInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import dev.karma.calamitouscore.compat.performance.DullingRecount;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.world.DullingWorldData", remap = false)
public abstract class MaintenanceRecountMixin {
    @Shadow @Final private static TagKey<Block> DULLBLOCK_TAG;

    @Inject(method = "scanChunk", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$countSections(ServerLevel level, LevelChunk chunk, DullingChunkInfo info, CallbackInfo ci) {
        if (level.isDebug() || (level.getMinBuildHeight() & 15) != 0 || (level.getMaxBuildHeight() & 15) != 0
                || chunk.getClass() != LevelChunk.class) return;
        DullingRecount.count(chunk.getSections(), state -> state.is(DULLBLOCK_TAG), ThedullingModBlocks.DULLNODE.get(), info);
        ci.cancel();
    }
}

