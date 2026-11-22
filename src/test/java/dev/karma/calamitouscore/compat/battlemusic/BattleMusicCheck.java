package dev.karma.calamitouscore.compat.battlemusic;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Apply shipping mixins to the installed SRG classes before checking the edited instructions. */
public final class BattleMusicCheck {
    public static void main(String[] args) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        for (String path : List.of("libs/battlemusic-1.20.1-1.1.1.jar","build/libs/calamitouscore-1.0.0.jar")) {
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
        Mixins.addConfiguration("battle-music-check.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = ((StellaraTestMixinService)MixinService.getService()).transformer();
        Map<String, ClassNode> nodes = new HashMap<>();
        for (String suffix : List.of("BattleMusic","BattleMusicInstance","BattleMusic$ForgeEvents")) {
            String name = "com.nekotune.battlemusic." + suffix;
            ClassNode node = new ClassNode();
            new ClassReader(transformer.transformClassBytes(name, name, classes.get(name))).accept(node, 0);
            nodes.put(suffix, node);
        }
        ClassNode sound = nodes.get("BattleMusicInstance"), main = nodes.get("BattleMusic");
        MethodNode gain = method(sound, "m_7769_");
        require(calls(gain, "m_92147_") == 1 && calls(gain, "m_7769_") == 1, "Music slider and native gain");
        require(Arrays.stream(gain.instructions.toArray()).filter(i -> i.getOpcode()==Opcodes.FMUL).count()==1, "Single volume multiplier");
        MethodNode set = method(main, "setVolume");
        require(calls(set, "sliderHandledAtPlayback")==1 && calls(set,"readFadeGain")==2, "Legacy scaling and fade gain");
        require(calls(method(main,"validEntity"),"musicRange")==1, "Range hook");
        require(calls(method(main,"updateEntitySoundData"),"repairPackEntry")==1, "Config parser hook");
        // Execute the merged row repair on the JVM independently of the Forge registry bootstrap.
        MethodNode repair = main.methods.stream().filter(m -> m.name.contains("repairPackEntry")).findFirst().orElseThrow();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,"RowProbe",null,"java/lang/Object",null);
        repair.accept(writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"repair",repair.desc,null,null));
        writer.visitEnd();
        Class<?> probe = new ClassLoader(){Class<?> define(byte[] bytes){return defineClass(null,bytes,0,bytes.length);}}.define(writer.toByteArray());
        for (String row : List.of("thedulling:war:summit;6","thedulling:war;battlemusic:other;8","fbt:battle_tower_golem;battlemusic:kermetic;3")) {
            Object result = probe.getMethod("repair",Iterator.class).invoke(null,List.of(row).iterator());
            require(result.equals(row.equals("thedulling:war:summit;6") ? "thedulling:war;battlemusic:summit;6" : row), "Config row " + row);
        }
        System.out.println("PASS: three shipping mixins applied to installed Battle Music, volume/range call sites and config parser verified.");
    }
    private static MethodNode method(ClassNode node,String name){return node.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    private static long calls(MethodNode m,String name){return Arrays.stream(m.instructions.toArray()).filter(i->i instanceof MethodInsnNode call && call.name.contains(name)).count();}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
