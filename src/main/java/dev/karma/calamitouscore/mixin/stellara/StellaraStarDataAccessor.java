package dev.karma.calamitouscore.mixin.stellara;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.StarFieldSkyContent$Star", remap = false)
public interface StellaraStarDataAccessor {
    @Accessor("textureIndex") int calamitouscore$textureIndex();
}
