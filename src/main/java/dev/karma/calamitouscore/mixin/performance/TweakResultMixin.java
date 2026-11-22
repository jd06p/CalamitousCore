package dev.karma.calamitouscore.mixin.performance;

import java.util.function.Supplier;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "mod.adrenix.nostalgic.tweak.TweakResult", remap = false)
public abstract class TweakResultMixin {
    @Shadow @Final private Supplier<?> delegate;
    @Unique private volatile Object calamitouscore$value;
    @Unique private boolean calamitouscore$computing;
    /** @author CalamitousCore @reason The original map has exactly one key; preserve memoization without per-get callbacks. */
    @Overwrite
    public Object get() {
        Object value = calamitouscore$value;
        if (value == null) synchronized (this) {
            value = calamitouscore$value;
            if (value == null) {
                if (calamitouscore$computing) throw new IllegalStateException("Recursive update");
                calamitouscore$computing = true;
                try { value = delegate.get(); calamitouscore$value = value; }
                finally { calamitouscore$computing = false; }
            }
        }
        return value;
    }
    /** @author CalamitousCore @reason Invalidate the one cached result with the original get/clear serialization. */
    @Overwrite
    public void clear() {
        synchronized (this) { calamitouscore$value = null; }
    }
}
