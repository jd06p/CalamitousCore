package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

/** Launch-time A/B switch for this optimization only; profiling remains available in both runs. */
public final class DullingPerformancePlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        return mixin.endsWith("AnthroWorkProfileMixin") || !Boolean.getBoolean("calamitouscore.dullingPerformance.disabled");
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
