package dev.karma.calamitouscore.mixin.dullingperf;

import dev.karma.calamitouscore.compat.dulling.performance.PortalSectionScan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.SeamlessPortalClientRenderer", remap = false)
public abstract class PortalScanMixin {
    @Shadow private static int clientFrameCounter;
    @Redirect(method = {"rescanBlackFallbackPortals", "rescanUnlinkedPortalGizmos"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;", remap = true))
    private static Iterable<BlockPos> calamitouscore$sparseScan(BlockPos min, BlockPos max) {
        return PortalSectionScan.portals(Minecraft.getInstance().level, min, max, clientFrameCounter);
    }
}
