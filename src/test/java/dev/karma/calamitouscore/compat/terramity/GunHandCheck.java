package dev.karma.calamitouscore.compat.terramity;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Apply shipping mixins to the installed SRG classes before checking the edited instructions. */
public final class GunHandCheck {
    public static void main(String[] args) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        for (String path : List.of("libs/terramity-0.9.8-forge-1.20.1.jar","build/libs/calamitouscore-1.0.0.jar")) {
            try (ZipFile zip = new ZipFile(path)) {
                for (var entry : Collections.list(zip.entries())) {
                    if (!entry.getName().endsWith(".class")) continue;
                    try (var in = zip.getInputStream(entry)) {
                        classes.put(entry.getName().replace('/', '.').replace(".class", ""), in.readAllBytes());
                    }
                }
            }
        }
        StellaraTestMixinService.fixtures = classes;
        System.setProperty("mixin.env.disableRefMap", "true");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.valueOf(args[0]));
        Mixins.addConfiguration("gun-hand-check.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = ((StellaraTestMixinService)MixinService.getService()).transformer();
        int entries = 0, charged = 0, warnings = 0;
        for (var pair : classes.entrySet()) {
            String name = pair.getKey();
            if (!name.startsWith("net.mcreator.terramity.procedures.")) continue;
            boolean notice = name.endsWith(".TwoHandedGunProcProcedure");
            boolean hud = name.endsWith(".TwoHandedConProcedure") && args[0].equals("CLIENT");
            if (!name.endsWith("RightclickedProcedure") && !notice && !hud) continue;
            ClassNode original = read(pair.getValue());
            ClassNode patched = read(transformer.transformClassBytes(name, name, pair.getValue()));
            for (MethodNode method : original.methods) {
                if (!method.name.equals("execute") && !method.name.startsWith("lambda$execute$")) continue;
                String marker = notice ? "$warningHand" : hud ? "$hudHand"
                    : method.name.equals("execute") ? "$availableHand" : "$chargedHand";
                List<String> before = operations(method);
                if (!before.contains("net/minecraft/world/entity/LivingEntity.m_21206_")) continue;
                int hand = before.indexOf("net/minecraft/world/entity/LivingEntity.m_21206_");
                // Only a direct offhand.getItem() == Blocks.AIR.asItem() guard qualifies.
                if (!before.subList(hand+1, before.size()).stream().limit(5).toList()
                        .contains("net/minecraft/world/level/block/Blocks.f_50016_")) continue;
                MethodNode after = patched.methods.stream().filter(m -> m.name.equals(method.name)
                    && m.desc.equals(method.desc)).findFirst().orElseThrow();
                List<String> expected = new ArrayList<>(before);
                expected.set(hand, "HAND_GATE");
                List<String> actual = operations(after);
                actual.replaceAll(call -> call.contains(marker) ? "HAND_GATE" : call);
                if (!actual.equals(expected)) throw new AssertionError(name + "." + method.name);
                if (!hud) {
                    MethodNode handler = patched.methods.stream().filter(m -> m.name.contains(marker)).findFirst().orElseThrow();
                    if (!operations(handler).equals(List.of("net/minecraft/world/item/ItemStack.f_41583_")))
                        throw new AssertionError("Unexpected hand substitute: " + name);
                }
                if (notice || hud) warnings++;
                else if (method.name.equals("execute")) entries++;
                else charged++;
            }
        }
        if (entries != 36 || charged != 4 || warnings != (args[0].equals("CLIENT") ? 2 : 1))
            throw new AssertionError("Unexpected coverage: " + entries + "/" + charged + "/" + warnings);
        System.out.println(args[0] + " passed: " + entries + " entry guards, " + charged + " delayed charge guards, " + warnings + " warning guards.");
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static List<String> operations(MethodNode method) {
        List<String> calls = new ArrayList<>();
        for (var op : method.instructions) {
            if (op instanceof MethodInsnNode m) calls.add(m.owner + "." + m.name);
            if (op instanceof FieldInsnNode f) calls.add(f.owner + "." + f.name);
        }
        return calls;
    }
}
