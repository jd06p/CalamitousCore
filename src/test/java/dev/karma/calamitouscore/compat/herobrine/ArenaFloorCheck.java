package dev.karma.calamitouscore.compat.herobrine;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Apply shipping mixins to the installed SRG classes before checking the edited instructions. */
public final class ArenaFloorCheck {
    public static void main(String[] args) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        for (String path : List.of("libs/herobrine_prime.jar","build/libs/calamitouscore-1.0.0.jar")) {
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
        Mixins.addConfiguration("arena-floor-check.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = ((StellaraTestMixinService)MixinService.getService()).transformer();
        String name = "net.mcreator.herobrineprime.procedures.MakeSurePlayerIsInArenaProcedure";
        byte[] transformed = transformer.transformClassBytes(name, name, classes.get(name));
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        var subscriber = node.methods.stream().filter(m -> m.name.equals("onPlayerTick")).findFirst().orElseThrow();
        boolean guard = false;
        for (var op : subscriber.instructions) {
            if (op instanceof MethodInsnNode call && call.name.contains("skipAbsentArenaFloor")) guard = true;
            if (op instanceof FieldInsnNode && !guard) throw new AssertionError("Event accessed before guard");
        }
        if (!guard) throw new AssertionError("Missing floor guard");
        Class<?> probe = new ClassLoader() {
            Class<?> loadProbe() { return defineClass(name, transformed, 0, transformed.length); }
        }.loadProbe();
        var tick = probe.getMethod("onPlayerTick", net.minecraftforge.event.TickEvent.PlayerTickEvent.class);
        // The installed handler would dereference the event immediately if the cancellation failed.
        for (int i = 0; i < 100; i++) tick.invoke(null, new Object[]{null});
        System.out.println(args[0] + " verified 100 player-tick returns before arena lookup or teleport");
    }
}
