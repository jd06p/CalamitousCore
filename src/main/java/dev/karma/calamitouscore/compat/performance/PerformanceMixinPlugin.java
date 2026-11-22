package dev.karma.calamitouscore.compat.performance;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;

public final class PerformanceMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (Boolean.getBoolean("calamitouscore.performanceWave2.disabled")) return false;
        if (!mixin.endsWith("LightLookupMixin")) return true;
        try { MixinService.getService().getBytecodeProvider().getClassNode("mod.adrenix.nostalgic.helper.candy.light.LightingHelper", false); return true; }
        catch (Exception missing) { return false; }
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith("OozeLookupsMixin") && PerformanceBytecode.oozeLookups(node) == 0) throw new IllegalStateException("Unrecognized installed Ooze Rain procedure");
        if (mixin.endsWith("LightLookupMixin") && PerformanceBytecode.lightCacheHits(node) != 1)
            com.mojang.logging.LogUtils.getLogger().warn("CalamitousCore: Nostalgic light lookup shape not present; retaining its native lighting handler.");
    }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
