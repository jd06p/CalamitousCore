package dev.karma.calamitouscore.mixin.stellara;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.SkyRibbonContent$Ribbon", remap = false)
public interface StellaraRibbonDataAccessor {
    @Accessor("thicknessScale") float calamitouscore$thicknessScale();
    @Accessor("phaseA") float calamitouscore$phaseA();
    @Accessor("phaseB") float calamitouscore$phaseB();
    @Accessor("phaseC") float calamitouscore$phaseC();
}
