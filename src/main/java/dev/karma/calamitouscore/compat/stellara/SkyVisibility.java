package dev.karma.calamitouscore.compat.stellara;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/** Render-thread only. No GL objects or level/camera references are retained. */
public final class SkyVisibility {
    private static final Matrix4f BASE = new Matrix4f(), LAST_POSE = new Matrix4f(), CLIP = new Matrix4f();
    private static final SkyClipVolume BILLBOARDS = new SkyClipVolume();
    private static boolean active, poseKnown;
    public static boolean colorShaderVerified, texturedShaderVerified;
    public static String lastShader = "not sampled";
    private static ShaderInstance reportedShader;
    private SkyVisibility() {}
    public static void begin() {
        active = true; poseKnown = false; reportedShader = null;
        BASE.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
    }
    public static void end() { active = false; poseKnown = false; reportedShader = null; }
    private static boolean supportedShader() {
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null && shader != reportedShader) {
            reportedShader = shader;
            if (SkyRenderDiagnostics.active()) lastShader = shader.getClass().getName() + ":" + shader.getName();
        }
        if (shader == null || shader.getClass() != ShaderInstance.class) return false;
        return switch (shader.getName()) {
            case "position_color" -> colorShaderVerified;
            case "position_tex_color" -> texturedShaderVerified;
            default -> false;
        };
    }
    public static void prepare(SkyClipVolume volume) {
        if (active && supportedShader() && SkyRenderDiagnostics.cullingEnabled()) volume.set(BASE);
        else volume.disable();
    }
    public static boolean hiddenBillboard(Matrix4f pose, float x, float y, float z, float size) {
        if (!active || !supportedShader() || !SkyRenderDiagnostics.cullingEnabled()) {
            SkyRenderDiagnostics.billboard(false); return false;
        }
        if (!poseKnown || !LAST_POSE.equals(pose)) {
            LAST_POSE.set(pose); BILLBOARDS.set(BASE.mul(pose, CLIP)); poseKnown = true;
        }
        // Stellara's rotated square has radius sqrt(2)*size. Two is a conservative bound,
        // including its table-based trigonometry; no star/planet touching the view is removed.
        boolean hidden = BILLBOARDS.outsideSphere(x, y, z, Math.abs(size) * 2.0F + 0.001F);
        SkyRenderDiagnostics.billboard(hidden); return hidden;
    }
}
