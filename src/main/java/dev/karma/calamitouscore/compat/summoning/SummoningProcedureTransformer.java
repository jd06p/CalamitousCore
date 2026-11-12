package dev.karma.calamitouscore.compat.summoning;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Small, version-verified call-site adaptations of MCreator-generated procedures. No global entity rewriting. */
public final class SummoningProcedureTransformer implements Opcodes {
    public static final String ROOT = "net/paisleypug/terramitypaisleyssummoning/";
    private static final String VARS = ROOT + "network/TerramityPaisleysSummoningModVariables$PlayerVariables";
    private static final String MINION = "com/fevzi/summonity/entity/SummonedMinion";
    private static final String BRIDGE = "dev/karma/calamitouscore/compat/summoning/";
    private static final String ENTITY = "Lnet/minecraft/world/entity/Entity;";
    private static final String TAME = "net/minecraft/world/entity/TamableAnimal";
    private SummoningProcedureTransformer() {}
    private static MethodInsnNode call(String type, String name, String desc) { return new MethodInsnNode(INVOKESTATIC, BRIDGE + type, name, desc, false); }
    public static void transform(ClassNode node) {
        if (!SummoningRoles.TARGETS.contains(node.name.replace('/', '.'))) throw new IllegalArgumentException("Unverified procedure target " + node.name);
        boolean combat = node.name.endsWith("/NewSummonTagHitProcedure") || node.name.endsWith("/SummonDamagedProcedure") || node.name.endsWith("/SummonKillsProcedure");
        for (MethodNode method : node.methods) {
            int local = (method.access & ACC_STATIC) == 0 ? 1 : 0, first = -1, entity = -1, slot = -1;
            InsnList before = new InsnList();
            for (Type arg : Type.getArgumentTypes(method.desc)) {
                if (arg.getDescriptor().equals(ENTITY)) {
                    if (first < 0) first = local; entity = local;
                    before.add(new VarInsnNode(ALOAD, local)); before.add(call("SharedSummonState", "bind", "(" + ENTITY + ")V"));
                }
                if (arg.getDescriptor().equals("Ltop/theillusivec4/curios/api/SlotContext;")) slot = local;
                local += arg.getSize();
            }
            if (entity >= 0 && Type.getReturnType(method.desc).getSort() == Type.VOID && (combat || node.name.endsWith("EntityDiesProcedure") || node.name.endsWith("OnEntityTickUpdateProcedure"))) {
                LabelNode proceed = new LabelNode(); before.add(new VarInsnNode(ALOAD, node.name.endsWith("/SummonDamagedProcedure") ? first : entity));
                before.add(call("SharedSummonState", "loadedOwner", "(" + ENTITY + ")Z")); before.add(new JumpInsnNode(IFNE, proceed)); before.add(new InsnNode(RETURN)); before.add(proceed);
            }
            boolean staff = node.name.contains("Rightclicked") || node.name.endsWith("RightClickProcedure");
            int cost = node.name.endsWith("/GnomeKingStaffRightclickedProcedure") || node.name.endsWith("/TrialStaffRightclickedProcedure") ? 2 : 1;
            if (staff && entity >= 0 && method.name.equals("execute") && Type.getReturnType(method.desc).getSort() == Type.VOID) {
                String type = null;
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction instanceof FieldInsnNode field && field.owner.equals(ROOT + "init/TerramityPaisleysSummoningModEntities")) type = field.name.toLowerCase(Locale.ROOT);
                if (type != null) {
                    LabelNode normal = new LabelNode(); before.add(new VarInsnNode(ALOAD, entity)); before.add(new LdcInsnNode("entity.terramity_paisleys_summoning." + type));
                    before.add(call("SummonSpawnBridge", "dismiss", "(" + ENTITY + "Ljava/lang/String;)Z")); before.add(new JumpInsnNode(IFEQ, normal)); before.add(new InsnNode(RETURN)); before.add(normal);
                }
            }
            int oldReads = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field) {
                    if (field.getOpcode() == GETSTATIC && field.owner.equals(ROOT + "init/TerramityPaisleysSummoningModAttributes") && field.name.equals("SUMMONSLOTS")) { field.owner = "com/fevzi/summonity/registry/ModAttributes"; field.name = "MINION_SLOTS"; }
                    if (field.owner.equals(VARS) && field.name.equals("currentSummons")) {
                        if (field.getOpcode() == PUTFIELD) method.instructions.set(field, call("SharedSummonState", "ignoreWrite", "(L" + VARS + ";D)V"));
                        else if (field.getOpcode() == GETFIELD) {
                            // These two old self-despawn branches conflict with Summonity's central limiter,
                            // especially for zero-cost accessory/temporary minions. Keep the central limiter only.
                            if (node.name.endsWith("/SummonedFairyOnEntityTickUpdateProcedure") && method.name.equals("execute") && oldReads < 2) {
                                InsnList constant = new InsnList(); constant.add(new InsnNode(POP)); constant.add(new InsnNode(oldReads++ == 0 ? DCONST_1 : DCONST_0));
                                method.instructions.insertBefore(field, constant); method.instructions.remove(field);
                            } else {
                                if (staff) method.instructions.insertBefore(field, new InsnNode(cost == 2 ? ICONST_2 : ICONST_1));
                                method.instructions.set(field, call("SharedSummonState", staff ? "admission" : "read", "(L" + VARS + ";" + (staff ? "I" : "") + ")D"));
                            }
                        }
                    }
                }
                if (combat && instruction instanceof TypeInsnNode type && type.desc.equals(TAME)) type.desc = MINION;
                if (combat && node.name.endsWith("/SummonKillsProcedure") && instruction instanceof TypeInsnNode type && type.getOpcode() == INSTANCEOF && type.desc.equals(ROOT + "entity/DwarfGnomeEntity")) method.instructions.set(type, call("SharedSummonState", "killEligible", "(" + ENTITY + ")Z"));
                if (instruction instanceof MethodInsnNode invoke) {
                    if (combat && invoke.owner.equals("net/minecraft/world/damagesource/DamageSource") && (invoke.name.equals("getEntity") || invoke.name.equals("m_7639_"))) {
                        method.instructions.set(invoke, call("SharedSummonState", "actor", "(Lnet/minecraft/world/damagesource/DamageSource;)" + ENTITY)); continue;
                    }
                    if (combat && invoke.owner.equals(TAME)) {
                        if (invoke.name.equals("getOwner") || invoke.name.equals("m_269323_")) method.instructions.set(invoke, new MethodInsnNode(INVOKEINTERFACE, MINION, "getOwner", "()Lnet/minecraft/world/entity/player/Player;", true));
                        else if (invoke.name.equals("isTame") || invoke.name.equals("m_21824_")) method.instructions.set(invoke, call("SharedSummonState", "hasOwner", "(L" + MINION + ";)Z"));
                        else throw new IllegalStateException("Unexpected tame operation in combat adapter " + invoke.name);
                        continue;
                    }
                    boolean fairyTick = node.name.endsWith("/SummonedFairyOnEntityTickUpdateProcedure");
                    if (fairyTick && invoke.owner.equals("net/minecraft/world/entity/Entity") && (invoke.name.equals("setDeltaMovement") || invoke.name.equals("m_20256_")) && invoke.desc.equals("(Lnet/minecraft/world/phys/Vec3;)V")) {
                        method.instructions.set(invoke, call("SummonCombatBridge", "velocity", "(" + ENTITY + "Lnet/minecraft/world/phys/Vec3;)V"));
                    } else if (fairyTick && invoke.owner.equals("net/minecraft/util/Mth") && invoke.desc.equals("(Lnet/minecraft/util/RandomSource;II)I") && entity >= 0) {
                        method.instructions.insertBefore(invoke, new VarInsnNode(ALOAD, entity)); method.instructions.set(invoke, call("SummonCombatBridge", "attackRoll", "(Lnet/minecraft/util/RandomSource;II" + ENTITY + ")I"));
                    } else if (slot >= 0 && invoke.owner.startsWith(ROOT + "procedures/") && invoke.desc.equals("(Lnet/minecraft/world/level/LevelAccessor;DDD)V") && (invoke.owner.contains("CubeOfMeatBaubleIs") || invoke.owner.contains("FuzzsterRingBaubleIs") || invoke.owner.contains("FuzzstersRingPlusBaubleIs"))) {
                        method.instructions.insertBefore(invoke, new VarInsnNode(ALOAD, slot)); method.instructions.insertBefore(invoke, new LdcInsnNode(invoke.owner.substring(invoke.owner.lastIndexOf('/') + 1)));
                        method.instructions.set(invoke, call("SummonSpawnBridge", "accessory", "(Lnet/minecraft/world/level/LevelAccessor;DDDLtop/theillusivec4/curios/api/SlotContext;Ljava/lang/String;)V"));
                    } else if (invoke.owner.equals("net/minecraft/world/entity/EntityType") && invoke.desc.equals("(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;)" + ENTITY)) {
                        method.instructions.insertBefore(invoke, entity < 0 ? new InsnNode(ACONST_NULL) : new VarInsnNode(ALOAD, entity));
                        method.instructions.set(invoke, call("SummonSpawnBridge", "spawn", "(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;" + ENTITY + ")" + ENTITY));
                    } else if (invoke.owner.equals(TAME) && (invoke.name.equals("tame") || invoke.name.equals("m_21828_"))) method.instructions.set(invoke, call("SummonSpawnBridge", "tame", "(L" + TAME + ";Lnet/minecraft/world/entity/player/Player;)V"));
                }
                if (combat && instruction instanceof FrameNode frame) { replaceFrames(frame.local); replaceFrames(frame.stack); }
            }
            if (node.name.endsWith("/ClearSummonsOnKeyPressedProcedure") && method.name.equals("execute") && entity >= 0) {
                method.instructions.clear(); method.tryCatchBlocks.clear(); if (method.localVariables != null) method.localVariables.clear();
                method.instructions.add(new VarInsnNode(ALOAD, entity)); method.instructions.add(call("SummonSpawnBridge", "clear", "(" + ENTITY + ")V")); method.instructions.add(new InsnNode(RETURN));
            } else if (before.size() > 0) method.instructions.insert(before);
            if (node.name.endsWith("/SummonedFairyOnEntityTickUpdateProcedure") && method.name.equals("execute") && entity >= 0) for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction.getOpcode() == RETURN) {
                InsnList cleanup = new InsnList(); cleanup.add(new VarInsnNode(ALOAD, entity)); cleanup.add(call("SummonEquipmentBridge", "stripLegacy", "(" + ENTITY + ")V")); method.instructions.insertBefore(instruction, cleanup);
            }
            method.maxStack += 12;
        }
    }
    private static void replaceFrames(List<Object> entries) { if (entries != null) for (int i = 0; i < entries.size(); i++) if (TAME.equals(entries.get(i))) entries.set(i, MINION); }
}
