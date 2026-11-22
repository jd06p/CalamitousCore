package dev.karma.calamitouscore.mixin.performance;

import dev.karma.calamitouscore.compat.performance.HeightIntegers;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "journeymap.client.cartography.render.SurfaceRenderer", remap = false)
public abstract class JourneyMapHeightMixin {
    @Redirect(method = "getBlockHeight(Ljourneymap/client/model/chunk/ChunkMD;ILjava/lang/Integer;ILjava/lang/Integer;Ljava/lang/Integer;)Ljava/lang/Integer;",
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"))
    private Integer calamitouscore$heightBox(int value) { return HeightIntegers.valueOf(value); }
}
