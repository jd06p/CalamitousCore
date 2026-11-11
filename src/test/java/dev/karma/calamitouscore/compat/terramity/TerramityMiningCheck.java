package dev.karma.calamitouscore.compat.terramity;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Fixture checks against the installed JARs; not a substitute for world/block-breaking tests. */
public final class TerramityMiningCheck {
    private static final Path RESOURCES = Path.of("src/main/resources/data");
    private static final Map<String, ZipFile> MODS = new HashMap<>();
    private static final Map<String, Set<String>> CONSTANTS = new HashMap<>();
    private static int assertions;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply installed mods directory");
        Path mods = Path.of(args[0]);
        String[][] fixtures = {
            {"terramity", "terramity-0.9.8-forge-1.20.1.jar", "net/mcreator/terramity/init/TerramityModItems.class"},
            {"thedulling", "thedulling-0.3.5-forge-1.20.1.jar", "net/mcreator/thedulling/init/ThedullingModItems.class"},
            {"composite_material", "CompositeMaterial-Forge-1.20.1-1.3.0.jar", "io/github/rcneg/compositematerial/common/init/ItemRegistry.class"},
            {"saintsdragons", "saintsdragons-0.9.88+forge-1.20.1.jar", "com/leon/saintsdragons/common/registry/ModItems.class"},
            {"enlightened_end", "enlightend-5.0.14-1.20.1.jar", "net/mcreator/enlightened_end/init/EnlightenedEndModItems.class"}
        };
        try {
            for (String[] f : fixtures) {
                ZipFile zip = new ZipFile(mods.resolve(f[1]).toFile()); MODS.put(f[0], zip);
                Set<String> constants = new HashSet<>();
                new ClassReader(read(zip, f[2])).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitLdcInsn(Object value) { if (value instanceof String s) constants.add(s); }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                CONSTANTS.put(f[0], constants);
            }
            Map<String, Integer> assigned = new TreeMap<>();
            String[] groups = {"iridium", "profanum", "highest"}; int[] counts = {7, 9, 3};
            for (int i = 0; i < groups.length; i++) {
                Set<String> ids = resolve("calamitouscore:terramity/" + groups[i], false, new HashSet<>());
                require(ids.size() == counts[i], "Unexpected group size " + groups[i]);
                for (String id : ids) {
                    require(assigned.put(id, i + 1) == null, "Duplicate assignment " + id);
                    String[] split = id.split(":");
                    require(CONSTANTS.get(split[0]).contains(split[1]), "Item missing from installed registry initializer: " + id);
                }
            }
            Set<String> all = resolve("calamitouscore:terramity/compatible_pickaxes", false, new HashSet<>());
            require(all.equals(assigned.keySet()), "Native fallback scope differs from selected items");
            String[] gates = {"netherite_tier", "plus_one_tier", "plus_two_tier", "plus_three_tier"};
            for (int level = 0; level < gates.length; level++) {
                String tag = "terramity:" + gates[level];
                Set<String> original = resolve(tag, true, new HashSet<>());
                Set<String> merged = resolve(tag, false, new HashSet<>());
                require(merged.containsAll(original), "Lost original members in " + tag);
                for (var entry : assigned.entrySet())
                    require(merged.contains(entry.getKey()) == (entry.getValue() >= level), "Incorrect ceiling/inheritance " + tag + " " + entry);
                Set<String> added = new HashSet<>(merged); added.removeAll(original);
                require(assigned.keySet().containsAll(added), "Unlisted tool gained membership");
            }
            checkNativeGates();
            System.out.println("Terramity fixture checks passed: " + assertions + " assertions; 19 installed IDs; 4 merged tier allowlists; lower-tier inheritance and higher-tier exclusion.");
            assigned.forEach((id, level) -> System.out.println(id + " -> " + groups[level - 1]));
            System.out.println("These are resource/bytecode checks. Actual block breaking, loot and client/server tag synchronization require an in-game test.");
        } finally { for (ZipFile z : MODS.values()) z.close(); }
    }

    private static void checkNativeGates() throws Exception {
        ZipFile zip = MODS.get("terramity"); int checked = 0;
        Set<String> known = Set.of("NetheriteOrBetterProcedure", "PlusOneOrBetterProcedure", "PlusTwoOrBetterProcedure", "PlusThreeOrBetterProcedure");
        for (var entry : Collections.list(zip.entries())) {
            if (!entry.getName().startsWith("net/mcreator/terramity/block/") || !entry.getName().endsWith(".class")) continue;
            List<String> instructions = new ArrayList<>();
            new ClassReader(read(zip, entry.getName())).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                    if (!name.equals("canHarvestBlock")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int op, String owner, String method, String descriptor, boolean itf) {
                            if (op == Opcodes.INVOKESPECIAL && method.equals("canHarvestBlock")) instructions.add("super");
                            String simple = owner.substring(owner.lastIndexOf('/') + 1);
                            if (known.contains(simple) && method.equals("execute")) instructions.add(simple);
                        }
                        @Override public void visitJumpInsn(int op, Label label) { if (op == Opcodes.IFEQ) instructions.add("ifFalse"); }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (instructions.stream().anyMatch(known::contains)) {
                require(instructions.size() >= 4 && instructions.get(0).equals("super") && instructions.get(1).equals("ifFalse")
                        && known.contains(instructions.get(2)) && instructions.get(3).equals("ifFalse"),
                        "Native harvest gate no longer ANDs superclass and custom tier: " + entry.getName() + instructions);
                checked++;
            }
        }
        require(checked == 22, "Installed gated-block implementation changed: " + checked);
        System.out.println("Verified all 22 Terramity gated blocks retain superclass AND custom-tier validation.");
        String[] names = {"Netherite", "PlusOne", "PlusTwo", "PlusThree"};
        String[] tags = {"netherite_tier", "plus_one_tier", "plus_two_tier", "plus_three_tier"};
        for (int i = 0; i < names.length; i++) {
            Set<String> references = new HashSet<>();
            new ClassReader(read(zip, "net/mcreator/terramity/procedures/" + names[i] + "OrBetterProcedure.class")).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitLdcInsn(Object value) { if (value instanceof String s) references.add(s); }
                        @Override public void visitMethodInsn(int op, String owner, String method, String descriptor, boolean itf) { references.add(owner + "." + method); }
                    };
                }
            }, 0);
            require(references.contains("terramity:" + tags[i]) && references.contains("net/minecraft/tags/ItemTags.create")
                    && references.contains("net/minecraft/world/item/ItemStack.m_204117_"), "Tier mechanism changed " + names[i]);
        }
    }

    private static Set<String> resolve(String tag, boolean originalOnly, Set<String> visiting) throws Exception {
        require(visiting.add(tag), "Cyclic tag " + tag);
        String[] split = tag.split(":", 2); String relative = split[0] + "/tags/items/" + split[1] + ".json";
        Set<String> result = new HashSet<>();
        ZipFile original = MODS.get(split[0]);
        if (original != null && original.getEntry("data/" + relative) != null)
            merge(JsonParser.parseString(new String(read(original, "data/" + relative), StandardCharsets.UTF_8)).getAsJsonObject(), originalOnly, visiting, result);
        Path local = RESOURCES.resolve(relative);
        if (!originalOnly && Files.exists(local)) {
            JsonObject json = JsonParser.parseString(Files.readString(local)).getAsJsonObject();
            require(!json.get("replace").getAsBoolean(), "Compatibility must append, never replace: " + tag);
            merge(json, false, visiting, result);
        }
        visiting.remove(tag); return result;
    }
    private static void merge(JsonObject json, boolean originalOnly, Set<String> visiting, Set<String> result) throws Exception {
        for (JsonElement value : json.getAsJsonArray("values")) {
            String id = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
            if (id.startsWith("#")) result.addAll(resolve(id.substring(1), originalOnly, visiting)); else result.add(id);
        }
    }
    private static byte[] read(ZipFile zip, String path) throws IOException {
        var entry = zip.getEntry(path); if (entry == null) throw new IOException("Missing " + path);
        try (var in = zip.getInputStream(entry)) { return in.readAllBytes(); }
    }
    private static void require(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
}
