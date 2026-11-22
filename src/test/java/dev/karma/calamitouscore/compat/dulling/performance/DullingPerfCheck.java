package dev.karma.calamitouscore.compat.dulling.performance;

import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

/** Actual installed-class transformations, stable-eviction equivalence and synthetic operation benchmarks. */
public final class DullingPerfCheck {
    private static final Map<String, byte[]> INPUT = new HashMap<>(), OUTPUT = new HashMap<>();
    private static final String ROOT = "net.mcreator.thedulling.";
    private static int checks;
    public static void main(String[] args) throws Exception {
        DullingPerformancePlugin plugin = new DullingPerformancePlugin();
        require(plugin.shouldApplyMixin("", "AnthroMindDataMixin"), "Optimization disabled by default");
        System.setProperty("calamitouscore.dullingPerformance.disabled", "true");
        require(!plugin.shouldApplyMixin("", "AnthroMindDataMixin") && plugin.shouldApplyMixin("", "AnthroWorkProfileMixin"), "A/B switch disables profiling or retains optimization");
        System.clearProperty("calamitouscore.dullingPerformance.disabled");
        Map<String, String> methods = new HashMap<>(), fields = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("build/createMcpToSrg/output.tsrg"))) {
            if (!line.startsWith("\t") || line.startsWith("\t\t")) continue;
            String[] p = line.trim().split(" +");
            if (p.length >= 3 && p[1].startsWith("(")) methods.put(p[2], p[0]); else if (p.length == 2) fields.put(p[1], p[0]);
        }
        for (String jar : List.of("thedulling-1.0.0-profiled.jar", "geckolib-forge-1.20.1-4.8.4.jar")) {
            try (ZipFile zip = new ZipFile("libs/" + jar)) {
                for (ZipEntry entry : Collections.list(zip.entries())) if (entry.getName().endsWith(".class")) {
                    byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                    ClassWriter writer = new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassRemapper(writer, new Remapper() {
                        @Override public String mapMethodName(String owner, String name, String desc) { return methods.getOrDefault(name, name); }
                        @Override public String mapFieldName(String owner, String name, String desc) { return fields.getOrDefault(name, name); }
                    }), 0);
                    INPUT.put(entry.getName().replace('/', '.').replace(".class", ""), writer.toByteArray());
                }
            }
        }
        StellaraTestMixinService.fixtures = INPUT;
        System.setProperty("mixin.env.disableRefMap", "true");
        boolean serverOnly = args.length > 0 && args[0].equals("server");
        MixinBootstrap.init(); MixinEnvironment.getDefaultEnvironment().setSide(serverOnly ? MixinEnvironment.Side.SERVER : MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("calamitouscore.dulling-performance.mixins.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class); phase.setAccessible(true); phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        IMixinTransformer transformer = ((StellaraTestMixinService) MixinService.getService()).transformer();
        List<String> targets = serverOnly ? List.of("AnthroMindData", "AnthroMind") : List.of("AnthroMindData", "AnthroMind", "SeamlessPortalClientRenderer", "client.SeraphimWarningOverlay", "client.GreatDevourerDevourRenderHandler");
        Loader original = new Loader(false), optimized = new Loader(true);
        for (String simple : targets) {
            String name = ROOT + simple;
            byte[] bytes = transformer.transformClassBytes(name, name, INPUT.get(name)); OUTPUT.put(name, bytes);
            ClassNode node = node(bytes);
            require(node.methods.stream().anyMatch(m -> m.name.contains("calamitouscore$")), "Hook absent: " + name);
            StringWriter errors = new StringWriter(); CheckClassAdapter.verify(new ClassReader(bytes), optimized, false, new PrintWriter(errors));
            require(errors.toString().isBlank(), "Invalid transformed body: " + name + "\n" + errors);
        }
        if (serverOnly) {
            for (String name : List.of("SeamlessPortalClientRenderer", "client.SeraphimWarningOverlay", "client.GreatDevourerDevourRenderHandler")) {
                String full = ROOT + name;
                require(Arrays.equals(INPUT.get(full), transformer.transformClassBytes(full, full, INPUT.get(full))), "Client mixin activated on SERVER: " + full);
            }
            System.out.println("PASS: common Dulling mixins transform on SERVER; client rendering mixins excluded.");
            return;
        }
        verifyCadence();
        eviction(original, optimized);
        DullingScanFixture.run(optimized);
        System.out.println("PASS: " + checks + " checks; 5 actual installed targets transformed and verified; original tick/scan cadences retained.");
        System.out.println("Synthetic isolated-method measurements only; no live world MSPT/TPS/FPS comparison was performed.");
    }
    private static ClassNode node(byte[] bytes) { ClassNode n = new ClassNode(); new ClassReader(bytes).accept(n, 0); return n; }
    private static void verifyCadence() {
        ClassNode before = node(INPUT.get(ROOT + "AnthroMind")), after = node(OUTPUT.get(ROOT + "AnthroMind"));
        MethodNode a = before.methods.stream().filter(m -> m.name.equals("onServerTick")).findFirst().orElseThrow();
        MethodNode b = after.methods.stream().filter(m -> m.name.equals("onServerTick")).findFirst().orElseThrow();
        List<Object> ca = constants(a), cb = constants(b); require(ca.equals(cb), "Server schedule constants changed");
        for (String method : List.of("rescanBlackFallbackPortals", "rescanUnlinkedPortalGizmos")) {
            var n = node(OUTPUT.get(ROOT + "SeamlessPortalClientRenderer"));
            var m = n.methods.stream().filter(x -> x.name.equals(method)).findFirst().orElseThrow();
            require(Arrays.stream(m.instructions.toArray()).filter(x -> x instanceof MethodInsnNode call && call.name.contains("calamitouscore$sparseScan")).count() == 1, "Sparse scan binding " + method);
        }
    }
    private static List<Object> constants(MethodNode node) {
        List<Object> result = new ArrayList<>();
        for (var insn : node.instructions) if (insn instanceof IntInsnNode i) result.add(i.operand); else if (insn instanceof LdcInsnNode i) result.add(i.cst);
        return result;
    }
    private static void eviction(Loader original, Loader optimized) throws Exception {
        Class<?> dataA = original.loadClass(ROOT + "AnthroMindData"), dataB = optimized.loadClass(ROOT + "AnthroMindData");
        var trimA = dataA.getDeclaredMethod("trimSites", Map.class); trimA.setAccessible(true);
        var trimB = dataB.getDeclaredMethod("trimSites", Map.class); trimB.setAccessible(true);
        Constructor<?> recordA = constructor(original), recordB = constructor(optimized);
        Object kindA = original.loadClass(ROOT + "AnthroMindData$SiteKind").getEnumConstants()[0];
        Object kindB = optimized.loadClass(ROOT + "AnthroMindData$SiteKind").getEnumConstants()[0];
        for (int seed = 0; seed < 80; seed++) {
            Map<Long, Object> a = new HashMap<>(), b = new HashMap<>(); Random r = new Random(seed);
            int count = seed < 4 ? new int[]{0, 4096, 4098, 4200}[seed] : 4097;
            for (int i = 0; i < count; i++) {
                BlockPos pos = new BlockPos(i * 17, (i % 384) - 64, i * 7); long time = seed % 3 == 0 ? 17 : r.nextLong();
                boolean renewable = r.nextBoolean(), owned = r.nextBoolean();
                a.put(pos.asLong(), recordA.newInstance(new ResourceLocation("minecraft", "overworld"), pos, kindA, time, i, renewable, owned));
                b.put(pos.asLong(), recordB.newInstance(new ResourceLocation("minecraft", "overworld"), pos, kindB, time, i, renewable, owned));
            }
            require(trimA.invoke(null, a).equals(trimB.invoke(null, b)), "Eviction return mismatch " + seed);
            require(a.keySet().equals(b.keySet()), "Different retained sites/tie behavior " + seed);
        }
        Map<Long,Object> a = workload(recordA, kindA), b = workload(recordB, kindB);
        for (int i = 0; i < 200; i++) { cycle(trimA, a); cycle(trimB, b); }
        double[] before = measure(trimA, a), after = measure(trimB, b);
        String row = "site_eviction_4097," + before[0] + "," + after[0] + "," + before[1] + "," + after[1];
        Files.writeString(Path.of("build/dulling-method-benchmark.csv"), "case,original_us,optimized_us,original_bytes,optimized_bytes\n" + row + "\n");
        System.out.printf(Locale.ROOT, "4097-site actual trimSites: original %.3f us / %.0f B; optimized %.3f us / %.0f B (median 7 x 128 calls).%n", before[0], before[1], after[0], after[1]);
    }
    private static Constructor<?> constructor(Loader loader) throws Exception {
        return loader.loadClass(ROOT + "AnthroMindData$MemorySite").getConstructor(ResourceLocation.class, BlockPos.class, loader.loadClass(ROOT + "AnthroMindData$SiteKind"), long.class, int.class, boolean.class, boolean.class);
    }
    private static Map<Long,Object> workload(Constructor<?> c, Object kind) throws Exception {
        Map<Long,Object> map = new HashMap<>(); Random random = new Random(32);
        for (int i = 0; i < 4097; i++) { BlockPos pos = new BlockPos(i, 65, i * 3); map.put(pos.asLong(), c.newInstance(new ResourceLocation("minecraft", "overworld"), pos, kind, random.nextLong(), i, random.nextBoolean(), random.nextBoolean())); }
        return map;
    }
    private static void cycle(java.lang.reflect.Method trim, Map<Long,Object> map) throws Exception {
        // Find original minimum outside the timed call; identity equals is not required across loaders.
        Object removed = null; long removedKey = 0;
        List<Map.Entry<Long,Object>> entries = new ArrayList<>(map.entrySet());
        trim.invoke(null, map);
        for (var entry : entries) if (!map.containsKey(entry.getKey())) {removed = entry.getValue(); removedKey = entry.getKey(); break;}
        if (removed != null) map.put(removedKey, removed);
    }
    private static double[] measure(java.lang.reflect.Method trim, Map<Long,Object> map) throws Exception {
        Map<Long,Object> copy = new HashMap<>(map); trim.invoke(null, copy);
        long key = map.keySet().stream().filter(k -> !copy.containsKey(k)).findFirst().orElseThrow(); Object value = map.get(key);
        var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean(); long thread = Thread.currentThread().getId();
        double[] times = new double[7], allocations = new double[7];
        for (int batch = 0; batch < 7; batch++) {
            long allocated = bean.getThreadAllocatedBytes(thread), time = System.nanoTime();
            for (int i = 0; i < 128; i++) { trim.invoke(null, map); map.put(key, value); }
            times[batch] = (System.nanoTime() - time) / 128000.0; allocations[batch] = (bean.getThreadAllocatedBytes(thread) - allocated) / 128.0;
        }
        Arrays.sort(times); Arrays.sort(allocations); return new double[]{times[3], allocations[3]};
    }
    static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    static final class Loader extends ClassLoader {
        private final boolean transformed;
        Loader(boolean transformed) { super(DullingPerfCheck.class.getClassLoader()); this.transformed = transformed; }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (INPUT.containsKey(name) || name.startsWith("dev.karma.calamitouscore.compat.dulling.performance.") && !name.contains("DullingPerfCheck") && !name.contains("DullingScanFixture")) {
                Class<?> type = findLoadedClass(name); if (type == null) type = findClass(name); if (resolve) resolveClass(type); return type;
            }
            return super.loadClass(name, resolve);
        }
        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = transformed ? OUTPUT.getOrDefault(name, INPUT.get(name)) : INPUT.get(name);
            if (bytes == null) try (var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) { if (in != null) bytes = in.readAllBytes(); } catch (IOException e) {throw new ClassNotFoundException(name, e);}
            if (bytes == null) throw new ClassNotFoundException(name); return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
