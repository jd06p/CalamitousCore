package dev.karma.calamitouscore.compat.summoning;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.world.entity.ai.attributes.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

/** Real installed-JAR Mixin transformation and modifier checks, without a game/world. */
public final class SummoningCompatCheck {
    private static final String PAISLEY = "net/paisleypug/terramitypaisleyssummoning/";
    private static int checks;
    private static final Map<String, byte[]> FIXTURES = new HashMap<>(), OUTPUT = new HashMap<>();
    private static IMixinTransformer transformer;
    public static void main(String[] args) throws Exception {
        Map<String,String> methods = new HashMap<>(), fields = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("build/createMcpToSrg/output.tsrg"))) {
            if (!line.startsWith("\t") || line.startsWith("\t\t")) continue;
            String[] part = line.trim().split(" +");
            if (part.length >= 3 && part[1].startsWith("(")) methods.put(part[2], part[0]);
            else if (part.length == 2) fields.put(part[1], part[0]);
        }
        for (String file : List.of("summonity-0.1.7.1.jar", "terramity_paisleys_summoning-0.4.1-forge-1.20.1.jar", "geckolib-forge-1.20.1-4.8.4.jar", "terramity-0.9.8-forge-1.20.1.jar")) {
            try (ZipFile zip = new ZipFile("libs/" + file)) {
                for (ZipEntry entry : Collections.list(zip.entries())) if (entry.getName().endsWith(".class")) {
                    byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                    ClassWriter writer = new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassRemapper(writer, new Remapper() {
                        @Override public String mapMethodName(String owner, String name, String desc) { return methods.getOrDefault(name, name); }
                        @Override public String mapFieldName(String owner, String name, String desc) { return fields.getOrDefault(name, name); }
                    }), 0);
                    FIXTURES.put(entry.getName().replace('/', '.').replace(".class", ""), writer.toByteArray());
                }
            }
        }
        StellaraTestMixinService.fixtures = FIXTURES;
        System.setProperty("mixin.env.disableRefMap", "true");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        optionalScenarios();
        Mixins.addConfiguration("calamitouscore.summoning.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class); phase.setAccessible(true); phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        transformer = ((StellaraTestMixinService) MixinService.getService()).transformer();
        int procedureCount = 0, bridgeCalls = 0;
        for (String name : SummoningRoles.TARGETS) {
            byte[] input = FIXTURES.get(name); require(input != null, "Missing installed target: " + name);
            byte[] bytes = transformer.transformClassBytes(name, name, input); OUTPUT.put(name, bytes);
            ClassNode node = node(bytes);
            for (MethodNode method : node.methods) for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode field) {
                    require(!(field.owner.equals(PAISLEY + "network/TerramityPaisleysSummoningModVariables$PlayerVariables") && field.name.equals("currentSummons")), "Unadapted old count " + name);
                    require(!(field.owner.equals(PAISLEY + "init/TerramityPaisleysSummoningModAttributes") && field.name.equals("SUMMONSLOTS")), "Unadapted slot attribute " + name);
                }
                if (insn instanceof MethodInsnNode invoke && invoke.owner.startsWith("dev/karma/calamitouscore/compat/summoning/")) bridgeCalls++;
            }
            if (name.contains(".procedures.")) procedureCount++;
        }
        require(bridgeCalls > 150, "Procedure transformations were not applied");
        Loader loader = new Loader();
        for (var role : SummoningRoles.ENTITIES.values()) {
            ClassNode entity = node(OUTPUT.get(role.className()));
            require(entity.interfaces.contains("com/fevzi/summonity/entity/SummonedMinion"), "Missing minion interface " + role.className());
            Class<?> loaded = loader.loadClass(role.className());
            require(!java.lang.reflect.Modifier.isAbstract(loaded.getModifiers()), "Adapted entity became abstract");
            require(Arrays.stream(loaded.getMethods()).anyMatch(m -> m.getName().equals("summonity$getSlotCost")), "No slot-cost method");
        }
        for (String name : List.of("net.paisleypug.terramitypaisleyssummoning.client.screens.CurrentSummonOverlayOverlay", "net.paisleypug.terramitypaisleyssummoning.client.screens.SummonOverlayOverlay", "com.fevzi.summonity.client.SummonHudOverlay")) {
            byte[] bytes = transformer.transformClassBytes(name, name, FIXTURES.get(name)); OUTPUT.put(name, bytes);
            require(Arrays.stream(node(bytes).methods.toArray(MethodNode[]::new)).anyMatch(m -> m.name.contains("calamitouscore$")), "HUD hook missing " + name);
        }
        byte[] goal; try (var in = SummoningCompatCheck.class.getResourceAsStream("/net/minecraft/world/entity/ai/goal/MeleeAttackGoal.class")) { goal = in.readAllBytes(); }
        byte[] transformedGoal = transformer.transformClassBytes("net.minecraft.world.entity.ai.goal.MeleeAttackGoal", "net.minecraft.world.entity.ai.goal.MeleeAttackGoal", goal);
        require(node(transformedGoal).methods.stream().anyMatch(m -> m.name.contains("calamitouscore$speed")), "Melee speed hook missing");
        for (String simple : List.of("NewSummonTagHitProcedure", "SummonDamagedProcedure", "SummonKillsProcedure")) {
            String name = PAISLEY.replace('/', '.') + "procedures." + simple;
            ClassNode adapted = node(OUTPUT.get(name));
            for (MethodNode method : adapted.methods) for (AbstractInsnNode insn : method.instructions) {
                require(!(insn instanceof TypeInsnNode type && type.desc.equals("net/minecraft/world/entity/TamableAnimal")), "Pet classification remains in adapted combat " + simple);
                require(!(insn instanceof MethodInsnNode invoke && invoke.owner.equals("net/minecraft/world/entity/TamableAnimal")), "Old tame ownership call remains " + simple);
            }
        }
        // The JVM links the actual adapted entities; ASM checks stack types for the modified procedures.
        for (String name : SummoningRoles.TARGETS) {
            var errors = new StringWriter(); CheckClassAdapter.verify(new ClassReader(OUTPUT.get(name)), loader, false, new PrintWriter(errors));
            require(errors.toString().isBlank(), "Bytecode verification: " + name + "\n" + errors);
        }
        modifierChecks();
        require(SummoningRoles.ENTITIES.size() == 18 && SummoningRoles.STAFFS.size() == 13, "Role/staff catalog count");
        require(SummoningRoles.ENTITIES.values().stream().filter(r -> r.cost() == 0).count() == 5, "Free companions changed");
        require(SummoningRoles.ENTITIES.values().stream().filter(r -> r.cost() == 2).count() == 2, "Two-slot summons changed");
        System.out.println("PASS: " + checks + " checks; " + SummoningRoles.TARGETS.size() + " installed targets (" + procedureCount + " procedures), " + bridgeCalls + " adapted call sites; 18 linked minion types; HUD hooks; pet exclusion; optional-addon gates; stable modifier migration/removal.");
        System.out.println("Code/bytecode checks only. Actual staff use, damage procs, Curios callbacks, HUD appearance and multiplayer require an in-game test.");
    }
    private static void optionalScenarios() throws Exception {
        var available = SummoningMixinPlugin.class.getDeclaredField("available"); available.setAccessible(true);
        String minion = "com.fevzi.summonity.entity.SummonedMinion", paisley = "net.paisleypug.terramitypaisleyssummoning.network.TerramityPaisleysSummoningModVariables";
        byte[] minionBytes = FIXTURES.get(minion), paisleyBytes = FIXTURES.get(paisley);
        available.set(null, null); require(SummoningMixinPlugin.enabled(), "Both-addon gate");
        FIXTURES.put(minion, new byte[0]); available.set(null, null); require(!SummoningMixinPlugin.enabled(), "Paisley-only gate");
        FIXTURES.put(paisley, new byte[0]); available.set(null, null); require(!SummoningMixinPlugin.enabled(), "No-addon gate");
        FIXTURES.put(minion, minionBytes); available.set(null, null); require(!SummoningMixinPlugin.enabled(), "Summonity-only gate");
        FIXTURES.put(paisley, paisleyBytes);
        ClassWriter old = new ClassWriter(0); old.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "dev/summoning/compat/SummoningCompat", null, "java/lang/Object", null); old.visitEnd();
        FIXTURES.put("dev.summoning.compat.SummoningCompat", old.toByteArray()); available.set(null, null); require(!SummoningMixinPlugin.enabled(), "Old bridge duplicate gate");
        FIXTURES.remove("dev.summoning.compat.SummoningCompat"); available.set(null, null); require(SummoningMixinPlugin.enabled(), "Gate reset");
    }
    private static void modifierChecks() {
        var attribute = new RangedAttribute("fixture", 10, 0, 1000); var value = new AttributeInstance(attribute, a -> {});
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        for (int i = 0; i < 1000; i++) {
            SummonEquipmentBridge.reconcile(value, first, "first", .15, AttributeModifier.Operation.MULTIPLY_TOTAL);
            SummonEquipmentBridge.reconcile(value, second, "second", .25, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        require(value.getModifiers().size() == 2 && Math.abs(value.getValue() - 14.375) < 1e-9, "Duplicate or drifting modifier");
        SummonEquipmentBridge.reconcile(value, first, "first", 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        require(value.getValue() == 12.5 && value.getBaseValue() == 10, "Unequip or base preservation");
        var legacy = new AttributeInstance(new RangedAttribute("legacy", 2, 0, 1000), a -> {});
        var canonical = new AttributeInstance(new RangedAttribute("canonical", 1, 0, 1000), a -> {});
        legacy.addPermanentModifier(new AttributeModifier(first, "slot", 2, AttributeModifier.Operation.ADDITION));
        for (int i = 0; i < 1000; i++) SummonEquipmentBridge.migrateSlots(legacy, canonical);
        require(legacy.getModifiers().isEmpty() && canonical.getModifiers().size() == 1 && canonical.getValue() == 3, "Slot migration duplicated or added legacy base");
        canonical.removeModifier(first); require(canonical.getValue() == 1, "Migrated modifier cannot be unequipped");
    }
    private static ClassNode node(byte[] bytes) { ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node; }
    private static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static final class Loader extends ClassLoader {
        Loader() { super(SummoningCompatCheck.class.getClassLoader()); }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (FIXTURES.containsKey(name) || name.startsWith("dev.karma.calamitouscore.compat.summoning.")) {
                Class<?> type = findLoadedClass(name); if (type == null) type = findClass(name); if (resolve) resolveClass(type); return type;
            }
            return super.loadClass(name, resolve);
        }
        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = OUTPUT.get(name);
            if (bytes == null && FIXTURES.containsKey(name)) { byte[] original = FIXTURES.get(name); bytes = transformer.transformClassBytes(name, name, original); OUTPUT.put(name, bytes); }
            if (bytes == null) try (var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) { if (in != null) bytes = in.readAllBytes(); } catch (IOException ex) { throw new ClassNotFoundException(name, ex); }
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
