package dev.karma.calamitouscore.compat.difficulty;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Apply shipping mixins to the installed SRG classes before checking the edited instructions. */
public final class DifficultyCheck {
    public static void main(String[] args) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        for (String path : List.of("libs/difficulty_enhancement-2.0.20.jar","build/libs/calamitouscore-1.0.0.jar")) {
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
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("difficulty-check.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = ((StellaraTestMixinService)MixinService.getService()).transformer();
        for (String target : List.of("client.ui.Coffee", "client.ui.ModifierScreen", "compat.WaystonesBridge")) {
            String name = "com.misanthropy.linggango.difficulty_enhancement." + target;
            ClassNode node = new ClassNode();
            new ClassReader(transformer.transformClassBytes(name, name, classes.get(name))).accept(node, 0);
            for (String marker : target.endsWith("ModifierScreen") ? List.of("$theme", "$cardSwitch")
                    : List.of(target.endsWith("Coffee") ? "$theme" : "$applyArrivalPenalty")) {
                long sites = node.methods.stream().filter(m -> !m.name.contains("calamitouscore"))
                    .flatMap(m -> Arrays.stream(m.instructions.toArray()))
                    .filter(i -> i instanceof MethodInsnNode call && call.name.contains(marker)).count();
                if (sites == 0) throw new AssertionError(target + " missing hook " + marker);
                System.out.println(target + " " + marker + " call sites = " + sites);
            }
        }
    }
}
