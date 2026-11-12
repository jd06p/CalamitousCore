package dev.karma.calamitouscore.compat.stellara;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.*;

/** Opt-in local diagnostics. No glFinish, blocking query waits, server packets or default FPS logging. */
@Mod.EventBusSubscriber(modid = "calamitouscore", value = Dist.CLIENT)
public final class SkyRenderDiagnostics {
    private enum Mode { IDLE, CURRENT, PREVIOUS, OFF }
    private static Mode mode = Mode.IDLE;
    private static long deadline, lastFrame, cpuStart;
    private static final List<Double> frames = new ArrayList<>(), cpu = new ArrayList<>(), gpu = new ArrayList<>();
    private static final int[] starts = new int[64], ends = new int[64];
    private static final boolean[] pending = new boolean[64];
    private static int cursor, activeSlot = -1, skippedSamples;
    private static boolean gpuAvailable;
    private static long fans, hiddenFans, billboards, hiddenBillboards;
    private SkyRenderDiagnostics() {}
    public static boolean cullingEnabled() { return mode != Mode.PREVIOUS; }
    public static boolean active() { return mode != Mode.IDLE; }
    public static boolean skipSky() { return mode == Mode.OFF; }

    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        if (!ModList.get().isLoaded("stellara")) return;
        var root = Commands.literal("ccskyprofile");
        for (String name : List.of("current", "previous", "off")) {
            Mode requested = Mode.valueOf(name.toUpperCase(Locale.ROOT));
            root.then(Commands.literal(name).executes(context -> start(requested, 15))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 60))
                            .executes(context -> start(requested, IntegerArgumentType.getInteger(context, "seconds")))));
        }
        root.then(Commands.literal("stop").executes(context -> { finish(); return 1; }));
        event.getDispatcher().register(root);
    }
    private static int start(Mode requested, int seconds) {
        if (mode != Mode.IDLE) finish();
        frames.clear(); cpu.clear(); gpu.clear(); lastFrame = cpuStart = 0; activeSlot = -1; cursor = skippedSamples = 0;
        fans = hiddenFans = billboards = hiddenBillboards = 0;
        gpuAvailable = requested != Mode.OFF && GL.getCapabilities().OpenGL33;
        if (gpuAvailable) for (int i = 0; i < starts.length; i++) { starts[i] = GL15.glGenQueries(); ends[i] = GL15.glGenQueries(); pending[i] = false; }
        mode = requested; deadline = System.nanoTime() + seconds * 1_000_000_000L;
        message("Stellara profile " + mode + " for " + seconds + "s. Keep the same camera; results go to chat and latest.log. OFF is temporary; PREVIOUS disables only new camera clipping.");
        return 1;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (mode == Mode.IDLE || event.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        if (now >= deadline) { finish(); return; }
        if (lastFrame != 0 && frames.size() < 20_000) frames.add((now - lastFrame) / 1e6);
        lastFrame = now; poll();
    }
    public static void beginSky() {
        if (mode == Mode.IDLE || mode == Mode.OFF) return;
        cpuStart = System.nanoTime(); activeSlot = -1;
        if (!gpuAvailable) return;
        int slot = cursor++ % starts.length;
        if (pending[slot]) { skippedSamples++; return; }
        GL33.glQueryCounter(starts[slot], GL33.GL_TIMESTAMP); activeSlot = slot;
    }
    public static void endSky() {
        if (mode == Mode.IDLE || mode == Mode.OFF || cpuStart == 0) return;
        if (activeSlot >= 0) { GL33.glQueryCounter(ends[activeSlot], GL33.GL_TIMESTAMP); pending[activeSlot] = true; activeSlot = -1; }
        if (cpu.size() < 20_000) cpu.add((System.nanoTime() - cpuStart) / 1e6);
        cpuStart = 0;
    }
    public static void fan(boolean hidden) { if (mode != Mode.IDLE) { fans++; if (hidden) hiddenFans++; } }
    public static void billboard(boolean hidden) { if (mode != Mode.IDLE) { billboards++; if (hidden) hiddenBillboards++; } }
    private static void poll() {
        if (!gpuAvailable) return;
        for (int i = 0; i < starts.length; i++) if (pending[i] && GL15.glGetQueryObjecti(ends[i], GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
            long begin = GL33.glGetQueryObjecti64(starts[i], GL15.GL_QUERY_RESULT);
            long end = GL33.glGetQueryObjecti64(ends[i], GL15.GL_QUERY_RESULT);
            if (end >= begin && gpu.size() < 20_000) gpu.add((end - begin) / 1e6);
            pending[i] = false;
        }
    }
    private static void finish() {
        if (mode == Mode.IDLE) return;
        poll(); Mode completed = mode; mode = Mode.IDLE;
        int unfinished = 0;
        if (gpuAvailable) for (int i = 0; i < starts.length; i++) {
            if (pending[i]) unfinished++;
            GL15.glDeleteQueries(starts[i]); GL15.glDeleteQueries(ends[i]); starts[i] = ends[i] = 0; pending[i] = false;
        }
        gpuAvailable = false; cpuStart = 0; activeSlot = -1;
        message(String.format(Locale.ROOT, "Stellara %s: frame %s; avg FPS %.1f; sky CPU %s; sky GPU %s; dropped/pending GPU %d/%d; vanilla shader verified color/texture=%s/%s. GPU utilization/VRAM not measured.",
                completed, stats(frames), frames.isEmpty() ? 0 : 1000 / average(frames), stats(cpu), stats(gpu), skippedSamples, unfinished,
                SkyVisibility.colorShaderVerified, SkyVisibility.texturedShaderVerified));
        message("Clipped fans " + hiddenFans + "/" + fans + "; billboards " + hiddenBillboards + "/" + billboards + "; last shader=" + SkyVisibility.lastShader);
        frames.clear(); cpu.clear(); gpu.clear();
    }
    private static double average(List<Double> samples) { return samples.stream().mapToDouble(Double::doubleValue).average().orElse(0); }
    private static String stats(List<Double> samples) {
        if (samples.isEmpty()) return "no samples";
        double[] sorted = samples.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return String.format(Locale.ROOT, "n=%d avg=%.3f p50=%.3f p95=%.3f p99=%.3f ms", sorted.length, average(samples), percentile(sorted, .5), percentile(sorted, .95), percentile(sorted, .99));
    }
    private static double percentile(double[] sorted, double quantile) { return sorted[Math.min(sorted.length - 1, (int)Math.ceil(sorted.length * quantile) - 1)]; }
    private static void message(String text) {
        LogUtils.getLogger().info("[CalamitousCore sky profile] {}", text);
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), false);
    }
}
