package dev.karma.calamitouscore.mixin.performance;

import java.util.*;
import mezz.jei.common.util.ImmutableRect2i;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "mezz.jei.common.util.MathUtil", remap = false)
public abstract class JeiIntersectionsMixin {
    /** @author CalamitousCore @reason Keep ordered short-circuit semantics without stream/callback allocation. */
    @Overwrite
    public static boolean intersects(Collection<ImmutableRect2i> areas, ImmutableRect2i comparison) {
        Objects.requireNonNull(areas); Objects.requireNonNull(comparison);
        for (ImmutableRect2i area : areas) if (comparison.intersects(area)) return true;
        return false;
    }
}
