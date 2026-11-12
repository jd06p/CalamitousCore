package dev.karma.calamitouscore.compat.stellara;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

/** Real Mixin transformation + original-JAR CPU mesh rendering. No game or GPU context. */
public final class StellaraRenderCheck {
    private static final String ROOT = "it.hurts.shatterbyte.stellara.client.sky.";
    private static volatile float blackhole;
    private static int checks;
    private static Map<String, byte[]> fixtures;
    private static final List<byte[]> captured = new ArrayList<>();
    private static boolean captureBytes;
    private static final com.sun.management.ThreadMXBean ALLOCATIONS = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Exception {
        Map<String, String> methods = new HashMap<>(), fields = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(args[1]))) {
            if (!line.startsWith("\t") || line.startsWith("\t\t")) continue;
            String[] parts = line.trim().split(" +");
            if (parts.length >= 3 && parts[1].startsWith("(")) methods.put(parts[2], parts[0]);
            else if (parts.length == 2) fields.put(parts[1], parts[0]);
        }
        fixtures = new HashMap<>();
        Map<String, JsonObject> skyboxes = new TreeMap<>();
        try (ZipFile zip = new ZipFile(args[0])) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                if (entry.getName().startsWith("it/") && entry.getName().endsWith(".class")) {
                    ClassWriter writer = new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassRemapper(writer, new Remapper() {
                        @Override public String mapMethodName(String owner, String name, String desc) { return owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/") ? methods.getOrDefault(name, name) : name; }
                        @Override public String mapFieldName(String owner, String name, String desc) { return owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/") ? fields.getOrDefault(name, name) : name; }
                    }), 0);
                    if (entry.getName().endsWith("/StarFieldSkyContent.class")) {
                        // Test-only: execute the original renderer and all CPU vertex encoding,
                        // replacing GL state calls/draw submission with buffer capture.
                        ClassWriter capture = new ClassWriter(0);
                        new ClassReader(writer.toByteArray()).accept(new ClassVisitor(Opcodes.ASM9, capture) {
                            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                                MethodVisitor parent = super.visitMethod(access, name, desc, sig, ex);
                                return new MethodVisitor(Opcodes.ASM9, parent) {
                                    @Override public void visitMethodInsn(int op, String owner, String method, String descriptor, boolean itf) {
                                        if (owner.equals("com/mojang/blaze3d/systems/RenderSystem") && org.objectweb.asm.Type.getReturnType(descriptor).equals(org.objectweb.asm.Type.VOID_TYPE)) {
                                            org.objectweb.asm.Type[] types = org.objectweb.asm.Type.getArgumentTypes(descriptor);
                                            for (int i = types.length - 1; i >= 0; i--) super.visitInsn(types[i].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
                                        } else if (owner.equals("com/mojang/blaze3d/vertex/Tesselator") && method.equals("end"))
                                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/karma/calamitouscore/compat/stellara/StellaraRenderCheck", "capture", "(Lcom/mojang/blaze3d/vertex/Tesselator;)V", false);
                                        else super.visitMethodInsn(op, owner, method, descriptor, itf);
                                    }
                                };
                            }
                        }, 0);
                        writer = capture;
                    }
                    fixtures.put(entry.getName().replace('/', '.').replace(".class", ""), writer.toByteArray());
                } else if (entry.getName().startsWith("assets/stellara/skyboxes/") && entry.getName().endsWith(".json"))
                    skyboxes.put(entry.getName().substring(entry.getName().lastIndexOf('/') + 1).replace(".json", ""), JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }
        StellaraTestMixinService.fixtures = fixtures;
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("calamitouscore-stellara-test.mixins.json");
        Method phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true); phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        IMixinTransformer transformer = ((StellaraTestMixinService) MixinService.getService()).transformer();
        Loader original = new Loader(null), optimized = new Loader(transformer);
        // Force transformation of all seven target classes, including private records and the shared registry.
        for (String name : List.of("StellaraSkyboxRenderer", "render.SkyGeometry", "api.SkyboxRegistry", "content.AuroraSkyContent", "content.SkyRibbonContent", "content.SkyRibbonContent$Ribbon", "content.RainbowSkyContent", "content.StarFieldSkyContent", "content.StarFieldSkyContent$Star")) optimized.loadClass(ROOT + name);
        require(optimized.loadClass(ROOT + "content.AuroraSkyContent").getDeclaredField("calamitouscore$grid") != null, "Mixin was not applied");
        System.out.println("PASS: actual Mixin 0.8.5 transformation of all nine Stellara targets.");
        com.mojang.blaze3d.systems.RenderSystem.initRenderThread();
        require(skyboxes.size() == 11, "Skybox inventory changed");
        if (Boolean.getBoolean("stellara.visibilityCaptureOnly")) {
            checkVisibility(optimized, skyboxes);
            System.out.println("PASS: wide-FOV production buffers captured for all 11 skyboxes."); return;
        }
        System.out.println("Skybox,mesh,vertices,baseline_cpu_ms,optimized_cpu_ms,baseline_java_bytes,optimized_java_bytes");
        for (var entry : skyboxes.entrySet()) {
            List<?> before = parse(original, entry.getKey(), entry.getValue());
            List<?> after = parse(optimized, entry.getKey(), entry.getValue());
            require(before.size() == after.size(), "Entry count " + entry.getKey());
            int meshes = 0;
            for (int i = 0; i < before.size(); i++) {
                Object a = content(before.get(i)), b = content(after.get(i));
                String type = a.getClass().getSimpleName();
                if (type.equals("StarFieldSkyContent")) {
                    checkStars(a, b);
                    StarDraw da = new StarDraw(a), db = new StarDraw(b);
                    for (float time : new float[]{0, 123.5F, 82001.75F}) {
                        List<byte[]> reference = da.bytes(time), result = db.bytes(time);
                        require(reference.size() == result.size(), "Star draw count changed");
                        for (int draw = 0; draw < reference.size(); draw++) require(Arrays.equals(reference.get(draw), result.get(draw)), "Star vertex/order mismatch " + entry.getKey());
                    }
                    for (int warm = 0; warm < 150; warm++) { da.run(warm, false); db.run(warm, false); }
                    double[] timesA = new double[7], timesB = new double[7];
                    for (int sample = 0; sample < 7; sample++) {
                        if ((sample & 1) == 0) { timesA[sample] = measureStars(da); timesB[sample] = measureStars(db); }
                        else { timesB[sample] = measureStars(db); timesA[sample] = measureStars(da); }
                    }
                    Arrays.sort(timesA); Arrays.sort(timesB);
                    long allocationA = allocated(() -> da.run(200, false)), allocationB = allocated(() -> db.run(200, false));
                    System.out.printf(Locale.ROOT, "%s,StarFieldSkyContent,%d,%.4f,%.4f,%d,%d%n", entry.getKey(), ((List<?>) field(a, "stars")).size() * 4, timesA[3], timesB[3], allocationA, allocationB);
                    continue;
                }
                if (!Set.of("AuroraSkyContent", "SkyRibbonContent", "RainbowSkyContent").contains(type)) continue;
                meshes++;
                Draw da = new Draw(a), db = new Draw(b);
                String export = System.getProperty("stellara.fixtureExport");
                if (export != null) {
                    Path directory = Path.of(export); Files.createDirectories(directory);
                    Files.write(directory.resolve(entry.getKey() + "-" + type + ".vertices"), db.bytes(123.45F, 0.7F));
                }
                int vertices = 0;
                for (float time : new float[]{0, 1.25F, 123.45F, 82001.75F, 10}) {
                    for (float alpha : new float[]{0.01F, 0.57F, 1}) {
                        byte[] reference = da.bytes(time, alpha), result = db.bytes(time, alpha);
                        require(Arrays.equals(reference, result), "Vertex mismatch: " + entry.getKey() + " " + type + " time=" + time + " alpha=" + alpha + " at byte " + Arrays.mismatch(reference, result));
                        vertices = reference.length / DefaultVertexFormat.POSITION_COLOR.getVertexSize();
                    }
                }
                double[] samplesA = new double[7], samplesB = new double[7];
                for (int warm = 0; warm < 150; warm++) { da.run(warm, 0.7F, false); db.run(warm, 0.7F, false); }
                for (int sample = 0; sample < samplesA.length; sample++) {
                    if ((sample & 1) == 0) { samplesA[sample] = measure(da); samplesB[sample] = measure(db); }
                    else { samplesB[sample] = measure(db); samplesA[sample] = measure(da); }
                }
                Arrays.sort(samplesA); Arrays.sort(samplesB);
                long allocationA = allocated(() -> da.run(200, 0.7F, false)), allocationB = allocated(() -> db.run(200, 0.7F, false));
                System.out.printf(Locale.ROOT, "%s,%s,%d,%.4f,%.4f,%d,%d%n", entry.getKey(), type, vertices, samplesA[3], samplesB[3], allocationA, allocationB);
            }
            checkRegistry(optimized, after);
            System.out.println("PASS: " + entry.getKey() + " (" + before.size() + " layers, " + meshes + " optimized mesh layers); source artwork/conditions unchanged.");
        }
        checkMemoBounds();
        if (Boolean.getBoolean("stellara.visibilityCheck")) checkVisibility(optimized, skyboxes);
        System.out.println("PASS: " + checks + " checks; byte-identical mesh vertex data across 15 time/alpha samples per mesh; registry reload/registration invalidation; exact star grouping.");
        System.out.println("CPU-only benchmark: median of 7 batches x 12 iterations after warmup; mesh construction/vertex encoding only, no GPU draw, FPS or in-game visual benchmark.");
    }
    private static double measure(Draw draw) throws Exception {
        long start = System.nanoTime(); for (int i = 0; i < 12; i++) draw.run(150 + i * 0.5F, 0.7F, false);
        return (System.nanoTime() - start) / 12e6;
    }
    private static double measureStars(StarDraw draw) throws Exception {
        long start = System.nanoTime(); for (int i = 0; i < 12; i++) draw.run(150 + i * 0.5F, false);
        return (System.nanoTime() - start) / 12e6;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void checkVisibility(Loader loader, Map<String, JsonObject> skyboxes) throws Exception {
        // Test-only uninitialized ShaderInstance: no GL program is constructed or used. Only
        // getClass()/getName() are exercised by the production shader-safety gate.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        Object shader = unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, net.minecraft.client.renderer.ShaderInstance.class);
        Field shaderName = net.minecraft.client.renderer.ShaderInstance.class.getDeclaredField("name"); shaderName.setAccessible(true); shaderName.set(shader, "position_color");
        com.mojang.blaze3d.systems.RenderSystem.setShader(() -> (net.minecraft.client.renderer.ShaderInstance) shader);
        com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix().identity();
        Class<?> visibility = loader.loadClass("dev.karma.calamitouscore.compat.stellara.SkyVisibility");
        Method begin = visibility.getMethod("begin"), end = visibility.getMethod("end");
        visibility.getField("colorShaderVerified").setBoolean(null, true);
        visibility.getField("texturedShaderVerified").setBoolean(null, true);
        Class<?> diagnostics = loader.loadClass("dev.karma.calamitouscore.compat.stellara.SkyRenderDiagnostics");
        Field mode = diagnostics.getDeclaredField("mode"); mode.setAccessible(true);
        Object previous = Enum.valueOf((Class) mode.getType(), "PREVIOUS"), current = Enum.valueOf((Class) mode.getType(), "CURRENT");
        int fov = Integer.getInteger("stellara.visibilityFov", 70);
        Path folder = Path.of(fov == 70 ? "build/stellara-visibility-fixtures" : "build/stellara-visibility-fixtures-" + fov); Files.createDirectories(folder);
        System.out.println("VISIBILITY,skybox,layer,yaw,vertices_before,vertices_after,cpu_before_ms,cpu_after_ms");
        try {
            for (var definition : skyboxes.entrySet()) for (Object entry : parse(loader, definition.getKey(), definition.getValue())) {
                Object content = content(entry); String type = content.getClass().getSimpleName();
                if (!Set.of("AuroraSkyContent", "SkyRibbonContent", "RainbowSkyContent", "StarFieldSkyContent").contains(type)) continue;
                Draw draw = type.equals("StarFieldSkyContent") ? null : new Draw(content);
                StarDraw stars = draw == null ? new StarDraw(content) : null;
                for (int yaw : new int[]{0, 90, 180, 270}) {
                    Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(fov), 16F / 9, .05F, 1024).rotateY((float)Math.toRadians(yaw));
                    com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
                    byte[][] buffers = new byte[2][]; double[] timings = new double[2];
                    for (int variant = 0; variant < 2; variant++) {
                        mode.set(null, variant == 0 ? previous : current);
                        begin.invoke(null);
                        if (draw != null) buffers[variant] = draw.bytes(123.45F, .7F);
                        else {
                            var out = new ByteArrayOutputStream(); for (byte[] batch : stars.bytes(123.45F)) out.write(batch);
                            buffers[variant] = starPositions(out.toByteArray());
                        }
                        end.invoke(null);
                        if (Boolean.getBoolean("stellara.visibilityCaptureOnly")) continue;
                        for (int warm = 0; warm < 15; warm++) { begin.invoke(null); if (draw != null) draw.run(123.45F, .7F, false); else stars.run(123.45F, false); end.invoke(null); }
                        double[] samples = new double[5];
                        for (int sample = 0; sample < 5; sample++) {
                            long start = System.nanoTime();
                            for (int iteration = 0; iteration < 12; iteration++) { begin.invoke(null); if (draw != null) draw.run(123.45F, .7F, false); else stars.run(123.45F, false); end.invoke(null); }
                            samples[sample] = (System.nanoTime() - start) / 12e6;
                        }
                        Arrays.sort(samples); timings[variant] = samples[2];
                    }
                    require(buffers[1].length <= buffers[0].length, "Clipping added vertices");
                    String file = definition.getKey() + "-" + type + "-yaw" + yaw;
                    Files.write(folder.resolve(file + ".before"), buffers[0]); Files.write(folder.resolve(file + ".after"), buffers[1]);
                    System.out.printf(Locale.ROOT, "VISIBILITY,%s,%s,%d,%d,%d,%.4f,%.4f%n", definition.getKey(), type, yaw, buffers[0].length / 16, buffers[1].length / 16, timings[0], timings[1]);
                }
            }
        } finally { mode.set(null, Enum.valueOf((Class) mode.getType(), "IDLE")); end.invoke(null); }
        System.out.println("PASS: production clipping paths executed; actual previous/current-mode buffers exported for GPU framebuffer comparison.");
    }
    private static byte[] starPositions(byte[] source) {
        // Keep original positions/colors. Opaque footprint testing is conservative for the
        // tiny transparent star textures; UV/artwork and draw order are untouched in production.
        byte[] output = new byte[source.length / 24 * 16];
        for (int i = 0, j = 0; i < source.length; i += 24, j += 16) {
            System.arraycopy(source, i, output, j, 12); System.arraycopy(source, i + 20, output, j + 12, 4);
        }
        return output;
    }
    @FunctionalInterface private interface CheckedRun { void run() throws Exception; }
    private static long allocated(CheckedRun draw) throws Exception {
        if (!ALLOCATIONS.isThreadAllocatedMemorySupported()) return -1;
        long thread = Thread.currentThread().getId(), start = ALLOCATIONS.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 12; i++) draw.run();
        return (ALLOCATIONS.getThreadAllocatedBytes(thread) - start) / 12;
    }
    public static void capture(Tesselator tesselator) {
        BufferBuilder.RenderedBuffer rendered = tesselator.getBuilder().end();
        try {
            ByteBuffer data = rendered.vertexBuffer(); if (data.hasRemaining()) blackhole = data.getFloat(0);
            if (captureBytes) { byte[] bytes = new byte[data.remaining()]; data.get(bytes); captured.add(bytes); }
        } finally { rendered.release(); }
    }
    private static List<?> parse(ClassLoader loader, String id, JsonObject json) throws Exception {
        // The geometry test never evaluates selection conditions. Avoid bootstrapping Minecraft's
        // global biome/dimension registries in this headless JVM; the shipped JSON is untouched.
        json = json.deepCopy();
        for (String key : List.of("biomes", "biome_tags", "dimensions")) {
            json.remove(key);
            if (json.has("layers")) for (JsonElement layer : json.getAsJsonArray("layers")) layer.getAsJsonObject().remove(key);
        }
        Class<?> type = loader.loadClass(ROOT + "data.SkyboxDataReloadListener");
        Method method = type.getDeclaredMethod("parseEntries", ResourceLocation.class, JsonObject.class); method.setAccessible(true);
        return (List<?>) method.invoke(null, new ResourceLocation("stellara", id), json);
    }
    private static Object content(Object entry) throws Exception { return entry.getClass().getMethod("content").invoke(entry); }
    private static Object field(Object object, String name) throws Exception { Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    private static void checkStars(Object original, Object optimized) throws Exception {
        List<?> stars = (List<?>) field(original, "stars"), textures = (List<?>) field(original, "textures");
        // Run the real injected initialization without the GL-bearing body of render.
        Method init = Arrays.stream(optimized.getClass().getDeclaredMethods()).filter(m -> m.getName().contains("calamitouscore$begin")).findFirst().orElseThrow();
        init.setAccessible(true); init.invoke(optimized, new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("fixture", false));
        List<?> groups = (List<?>) field(optimized, "calamitouscore$byTexture");
        int count = 0;
        for (int texture = 0; texture < textures.size(); texture++) {
            List<String> expected = new ArrayList<>(), actual = new ArrayList<>();
            for (Object star : stars) if ((int) field(star, "textureIndex") == texture) expected.add(star.toString());
            for (Object star : (List<?>) groups.get(texture)) actual.add(star.toString());
            require(expected.equals(actual), "Star membership/order changed"); count += actual.size();
        }
        require(count == stars.size(), "Star missing/duplicated");
    }
    private static void checkRegistry(Loader loader, List<?> entries) throws Exception {
        Class<?> registry = loader.loadClass(ROOT + "api.SkyboxRegistry");
        Field api = registry.getDeclaredField("ENTRIES"); api.setAccessible(true); ((List<?>) api.get(null)).clear();
        Method replace = registry.getMethod("replaceDataEntries", List.class), get = registry.getMethod("entries");
        replace.invoke(null, entries); Object first = get.invoke(null);
        require(first == get.invoke(null), "Registry snapshot not reused");
        replace.invoke(null, List.of()); require(((List<?>) get.invoke(null)).isEmpty(), "Reload retained stale entries");
        replace.invoke(null, entries); require(first != get.invoke(null), "Reload failed invalidation");
        Method register = Arrays.stream(registry.getMethods()).filter(m -> m.getName().equals("register") && m.getParameterCount() == 3).findFirst().orElseThrow();
        int count = ((List<?>) get.invoke(null)).size();
        Object e = entries.get(0);
        register.invoke(null, content(e), e.getClass().getMethod("condition").invoke(e), 0.08F);
        require(((List<?>) get.invoke(null)).size() == count + 1, "API registration failed invalidation");
        Class<?> contentType = loader.loadClass(ROOT + "api.SkyboxContent");
        Object custom = Proxy.newProxyInstance(loader, new Class<?>[]{contentType}, (proxy, method, args) -> method.getName().equals("priority") ? 100 : new ResourceLocation("fixture", "custom"));
        register.invoke(null, custom, e.getClass().getMethod("condition").invoke(e), 0.08F);
        require(get.invoke(null) != get.invoke(null), "Third-party content with potentially dynamic priority must retain original sorting");
    }
    private static void checkMemoBounds() {
        SkyGridMemo<Object> memo = new SkyGridMemo<>(); memo.begin(Integer.MAX_VALUE, 2, 1, false, false);
        require(memo.next() == -1, "Oversized pack mesh allocated a memo table");
        memo.begin(2, 2, 1, false, false); int slot = memo.next(); Object token = new Object(); memo.put(slot, token);
        memo.begin(2, 2, 1, false, false); require(memo.get(memo.next()) == null, "Animated mesh reused a previous frame");
    }
    private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static final class Loader extends ClassLoader {
        private final IMixinTransformer transformer;
        Loader(IMixinTransformer transformer) { super(StellaraRenderCheck.class.getClassLoader()); this.transformer = transformer; }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("dev.karma.calamitouscore.compat.stellara.Sky")) {
                Class<?> found = findLoadedClass(name);
                if (found == null) found = findClass(name);
                if (resolve) resolveClass(found);
                return found;
            }
            return super.loadClass(name, resolve);
        }
        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = fixtures.get(name);
            if (bytes == null && name.startsWith("dev.karma.calamitouscore.compat.stellara.Sky")) {
                try (var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (in != null) bytes = in.readAllBytes();
                } catch (IOException error) { throw new ClassNotFoundException(name, error); }
            }
            if (bytes == null) throw new ClassNotFoundException(name);
            if (transformer != null) bytes = transformer.transformClassBytes(name, name, bytes);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
    private static final class Draw {
        private final Object content; private final Method method; private final String type;
        private final BufferBuilder buffer = new BufferBuilder(256 * 1024);
        Draw(Object content) {
            this.content = content; type = content.getClass().getSimpleName();
            String name = type.equals("AuroraSkyContent") ? "renderCurtains" : type.equals("SkyRibbonContent") ? "renderRibbon" : "renderGradient";
            method = Arrays.stream(content.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElseThrow(); method.setAccessible(true);
        }
        byte[] bytes(float time, float alpha) throws Exception { return run(time, alpha, true); }
        byte[] run(float time, float alpha, boolean copy) throws Exception {
            buffer.begin(type.equals("RainbowSkyContent") ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            try {
                if (type.equals("AuroraSkyContent")) method.invoke(content, buffer, new Matrix4f().rotateX(-1.5707964F).rotateY(0.83F), time, alpha);
                else if (type.equals("SkyRibbonContent")) method.invoke(content, buffer, new Matrix4f().rotateX(-1.5707964F).rotateY(0.83F), field(content, "ribbon"), time, alpha);
                else { float radius = (float) field(content, "radius"), thickness = (float) field(content, "thickness"); method.invoke(content, buffer, new Matrix4f().rotateX(-1.5707964F).rotateY(0.83F), radius, radius - thickness, alpha); }
                BufferBuilder.RenderedBuffer rendered = buffer.end();
                try { ByteBuffer data = rendered.vertexBuffer(); if (data.remaining() >= 4) blackhole = data.getFloat(0); if (copy) { byte[] bytes = new byte[data.remaining()]; data.get(bytes); return bytes; } return null; }
                finally { rendered.release(); }
            } catch (InvocationTargetException error) { throw new RuntimeException(type, error.getCause()); }
        }
    }
    private static final class StarDraw {
        private final Object content; private final Constructor<?> context; private final Method render;
        StarDraw(Object content) throws Exception {
            this.content = content;
            Class<?> contextType = content.getClass().getClassLoader().loadClass(ROOT + "api.SkyboxRenderContext");
            context = contextType.getConstructors()[0]; render = content.getClass().getMethod("render", contextType, float.class);
        }
        List<byte[]> bytes(float time) throws Exception { run(time, true); return new ArrayList<>(captured); }
        void run(float time, boolean copy) throws Exception {
            PoseStack pose = new PoseStack(); pose.last().pose().rotateX(-1.5707964F).rotateY(0.83F);
            Object ctx = context.newInstance(null, null, null, pose, 0.5F, time, 45F, 18000F, 1F, 1F, 1F, null, null);
            captured.clear(); captureBytes = copy; render.invoke(content, ctx, 0.7F);
        }
    }
}





