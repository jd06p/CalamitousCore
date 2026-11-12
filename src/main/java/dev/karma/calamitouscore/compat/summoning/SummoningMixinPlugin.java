package dev.karma.calamitouscore.compat.summoning;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;

public final class SummoningMixinPlugin implements IMixinConfigPlugin {
    private static Boolean available;
    public static boolean enabled() {
        if (available == null) available = present("com.fevzi.summonity.entity.SummonedMinion")
                && present("net.paisleypug.terramitypaisleyssummoning.network.TerramityPaisleysSummoningModVariables")
                && !present("dev.summoning.compat.SummoningCompat");
        return available;
    }
    private static boolean present(String name) {
        try { MixinService.getService().getBytecodeProvider().getClassNode(name); return true; }
        catch (Exception ignored) { return false; }
    }
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return enabled(); }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith("PaisleyProcedureMixin")) SummoningProcedureTransformer.transform(node);
    }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
