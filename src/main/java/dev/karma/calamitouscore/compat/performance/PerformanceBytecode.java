package dev.karma.calamitouscore.compat.performance;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Two verified call-site changes; no global SavedData, light propagation or block behavior changes. */
public final class PerformanceBytecode implements Opcodes {
    private PerformanceBytecode() {}
    public static int oozeLookups(ClassNode node) {
        int changes = 0;
        for (MethodNode method : node.methods) if (method.name.equals("execute") && method.desc.equals("(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;)V")) {
            int map = method.maxLocals++, world = method.maxLocals++;
            InsnList init = new InsnList(); init.add(new InsnNode(ACONST_NULL)); init.add(new VarInsnNode(ASTORE, map)); init.add(new InsnNode(ACONST_NULL)); init.add(new VarInsnNode(ASTORE, world)); method.instructions.insert(init);
            for (var insn : method.instructions.toArray()) if (insn instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.name.equals("get") && call.owner.startsWith("net/mcreator/enlightened_end/network/EnlightenedEndModVariables$") && (call.owner.endsWith("$MapVariables") || call.owner.endsWith("$WorldVariables"))) {
                int local = call.owner.endsWith("$MapVariables") ? map : world;
                LabelNode miss = new LabelNode(), done = new LabelNode();
                InsnList before = new InsnList(); before.add(new VarInsnNode(ALOAD, local)); before.add(new JumpInsnNode(IFNULL, miss)); before.add(new InsnNode(POP)); before.add(new VarInsnNode(ALOAD, local)); before.add(new JumpInsnNode(GOTO, done)); before.add(miss);
                method.instructions.insertBefore(call, before);
                InsnList after = new InsnList(); after.add(new InsnNode(DUP)); after.add(new VarInsnNode(ASTORE, local)); after.add(done); method.instructions.insert(call, after);
                changes++;
            }
            method.maxStack += 2;
        }
        return changes;
    }
    public static int lightCacheHits(ClassNode node) {
        int changes = 0;
        for (MethodNode method : node.methods) {
            boolean lightMap = false;
            for (var insn : method.instructions) if (insn instanceof FieldInsnNode field && field.owner.equals("mod/adrenix/nostalgic/helper/candy/light/LightingHelper") && field.name.equals("LIGHT_BLOCK_CACHE")) lightMap = true;
            if (!lightMap) continue;
            for (var insn : method.instructions.toArray()) if (insn instanceof InvokeDynamicInsnNode dynamic && dynamic.desc.endsWith("I)Ljava/util/function/Function;")) {
                AbstractInsnNode loadInt = previous(dynamic), loadThis = previous(loadInt);
                if (!(loadInt instanceof VarInsnNode variable) || variable.getOpcode() != ILOAD || !(loadThis instanceof VarInsnNode receiver) || receiver.getOpcode() != ALOAD || receiver.var != 0) continue;
                LabelNode miss = new LabelNode(); int cached = method.maxLocals++;
                InsnList fast = new InsnList(); fast.add(new InsnNode(DUP2)); fast.add(new MethodInsnNode(INVOKEVIRTUAL,"java/util/concurrent/ConcurrentHashMap","get","(Ljava/lang/Object;)Ljava/lang/Object;",false)); fast.add(new InsnNode(DUP)); fast.add(new JumpInsnNode(IFNULL,miss));
                fast.add(new TypeInsnNode(CHECKCAST,"java/lang/Integer")); fast.add(new MethodInsnNode(INVOKEVIRTUAL,"java/lang/Integer","intValue","()I",false)); fast.add(new VarInsnNode(ISTORE,cached)); fast.add(new InsnNode(POP2)); fast.add(new VarInsnNode(ILOAD,cached)); fast.add(new InsnNode(IRETURN)); fast.add(miss); fast.add(new InsnNode(POP));
                method.instructions.insertBefore(loadThis,fast); method.maxStack+=3; changes++;
            }
        }
        return changes;
    }
    private static AbstractInsnNode previous(AbstractInsnNode node) {
        do {node=node.getPrevious();} while(node!=null && node.getOpcode()<0); return node;
    }
}
