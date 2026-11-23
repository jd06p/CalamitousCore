package dev.karma.calamitouscore.compat.difficulty;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the installed clock instructions with a supplied time, NBT and spawn result. */
final class BossRushClockCheck implements Opcodes {
    private static final String NEXT = "difficulty_enhancement.boss_rush_at";
    private static final String WARN = "difficulty_enhancement.boss_rush_warned";

    static void verify(ClassNode node) throws Exception {
        var known = java.util.Set.of("minecraft:warden", "minecraft:wither");
        java.util.function.Function<net.minecraft.resources.ResourceLocation,String> registry =
            id -> known.contains(id.toString()) ? id.toString() : null;
        check(BossRushConfig.resolve(java.util.List.of("minecraft:warden", "minecraft:wither", "minecraft:warden",
            "warden", ":warden", "BAD ID", "absent:boss"), registry).equals(java.util.List.of("minecraft:warden", "minecraft:wither")));
        check(BossRushConfig.resolve(java.util.List.of("minecraft:wither"), registry).equals(java.util.List.of("minecraft:wither")));
        check(BossRushConfig.resolve(java.util.List.of(), registry).isEmpty());
        MethodNode tick = node.methods.stream().filter(m -> m.name.equals("onTick")).findFirst().orElseThrow();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V17, ACC_PUBLIC, "BossRushClockProbe", null, "java/lang/Object", null);
        MethodVisitor out = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "tick",
            "(ZZLnet/minecraft/nbt/CompoundTag;J)V", null, null);
        // Locals 2, 3/4 and 5/6 correspond to the native tag, current time and deadline.
        // Substitute only external inputs and warning delivery. The three date comparisons,
        // NBT writes, spawn-success branch and all native interval constants are copied.
        boolean copying = false, skipMessage = false;
        Map<String,String> names = Map.of("m_128454_", "getLong", "m_128356_", "putLong",
            "m_128471_", "getBoolean", "m_128379_", "putBoolean");
        var labels = new java.util.HashMap<LabelNode,LabelNode>();
        for (var ins : tick.instructions) if (ins instanceof LabelNode l) labels.put(l, new LabelNode());
        Label active = new Label();
        out.visitVarInsn(ILOAD, 0);
        out.visitJumpInsn(IFNE, active);
        out.visitInsn(RETURN);
        out.visitLabel(active);
        for (var ins : tick.instructions) {
            if (!copying) {
                if (ins instanceof VarInsnNode v && v.getOpcode() == LSTORE && v.var == 3) copying = true;
                continue;
            }
            if (ins instanceof FrameNode || ins instanceof LineNumberNode) continue;
            if (skipMessage && ins instanceof VarInsnNode v && v.getOpcode() == LLOAD && v.var == 3)
                skipMessage = false;
            if (skipMessage && !(ins instanceof LabelNode)) continue;
            if (ins instanceof MethodInsnNode m && names.containsKey(m.name)) {
                out.visitMethodInsn(INVOKEVIRTUAL, m.owner, names.get(m.name), m.desc, false);
                if (m.name.equals("m_128379_") && ins.getPrevious().getOpcode() == ICONST_1) skipMessage = true;
            } else if (ins instanceof VarInsnNode v && v.getOpcode() == ALOAD && v.var == 1) {
                out.visitVarInsn(ILOAD, 1);
            } else if (ins instanceof VarInsnNode v && v.var >= 7 && (v.getOpcode() == ASTORE || v.getOpcode() == ALOAD)) {
                // Mixin spills and reloads the player argument around the redirect.
                out.visitVarInsn(v.getOpcode() == ASTORE ? ISTORE : ILOAD, v.var);
            } else if (!(ins instanceof MethodInsnNode m && m.name.contains("$encounter"))) {
                if (ins instanceof LabelNode l) labels.get(l).accept(out);
                else ins.clone(labels).accept(out);
            }
        }
        out.visitMaxs(0,0);
        out.visitEnd();
        writer.visitEnd();
        Class<?> probe = new ClassLoader() {
            Class<?> load(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
        }.load(writer.toByteArray());
        var run = probe.getMethod("tick", boolean.class, boolean.class, CompoundTag.class, long.class);
        var data = new CompoundTag();
        run.invoke(null,true,true,data,100L);
        check(data.getLong(NEXT) == 72100);
        run.invoke(null,true,true,data,70899L);
        check(!data.getBoolean(WARN));
        run.invoke(null,true,true,data,70900L);
        check(data.getBoolean(WARN));
        var scheduleConstructor = BossRushSchedule.class.getDeclaredConstructor(CompoundTag.class);
        scheduleConstructor.setAccessible(true);
        data = scheduleConstructor.newInstance(data).save(new CompoundTag());
        run.invoke(null,true,true,data,72099L);
        run.invoke(null,false,true,data,72100L);
        run.invoke(null,true,false,data,72100L);
        check(data.getLong(NEXT) == 72100 && data.getBoolean(WARN));
        run.invoke(null,true,true,data,72101L);
        check(data.getLong(NEXT) == 144101 && !data.getBoolean(WARN));
        run.invoke(null,true,true,data,72102L);
        check(data.getLong(NEXT) == 144101);
        System.out.println("Boss Rush native clock: initialization, warning, NBT round-trip, disabled input, failed retry and shared deadline passed.");
    }

    private static void check(boolean result) {
        if (!result) throw new AssertionError("Boss Rush clock");
    }
}
