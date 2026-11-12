package dev.karma.calamitouscore.mixin.stellara;

import dev.karma.calamitouscore.compat.stellara.SkyGridMemo;
import dev.karma.calamitouscore.compat.stellara.SkyTriangleFan;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import it.hurts.shatterbyte.stellara.client.sky.content.AuroraSkyContent;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.AuroraSkyContent", remap = false)
public abstract class StellaraAuroraMixin {
    @Shadow @Final private int segments;
    @Shadow @Final private int bands;
    @Shadow @Final private int gradientSteps;
    @Shadow private SkyPoint curtainPoint(float h, float pitch, float depth, float v) { throw new AssertionError(); }
    @Shadow private SkyColor color(float progress) { throw new AssertionError(); }
    @Unique private final SkyGridMemo<SkyPoint> calamitouscore$grid = new SkyGridMemo<>();
    @Unique private SkyColor[] calamitouscore$palette;
    @Unique private int calamitouscore$colorCall;

    @Inject(method = "renderCurtains", at = @At("HEAD"))
    private void calamitouscore$begin(CallbackInfo ci) {
        calamitouscore$fan.begin();
        calamitouscore$grid.begin(segments, gradientSteps, bands, false, false);
        if (calamitouscore$palette == null) calamitouscore$palette = new SkyColor[gradientSteps + 1];
        calamitouscore$colorCall = 0;
    }
    @Redirect(method = "renderCurtains", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/AuroraSkyContent;curtainPoint(FFFF)Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;"))
    private SkyPoint calamitouscore$point(AuroraSkyContent owner, float h, float pitch, float depth, float v) {
        int slot = calamitouscore$grid.next();
        SkyPoint point = calamitouscore$grid.get(slot);
        if (point == null) { point = curtainPoint(h, pitch, depth, v); calamitouscore$grid.put(slot, point); }
        return point;
    }
    @Redirect(method = "renderCurtains", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/AuroraSkyContent;color(F)Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;"))
    private SkyColor calamitouscore$color(AuroraSkyContent owner, float progress) {
        int slot = (calamitouscore$colorCall + 1) >> 1;
        if (++calamitouscore$colorCall == gradientSteps * 2) calamitouscore$colorCall = 0;
        SkyColor color = calamitouscore$palette[slot];
        if (color == null) calamitouscore$palette[slot] = color = color(progress);
        return color;
    }
    @Unique private final SkyTriangleFan calamitouscore$fan = new SkyTriangleFan();
    @Redirect(method = "renderCurtains", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/AuroraSkyContent;addCurtain(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Matrix4f;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;FF)V"))
    private void calamitouscore$fan(AuroraSkyContent owner, BufferBuilder buffer, Matrix4f matrix, SkyPoint ll, SkyPoint ul, SkyPoint ur, SkyPoint lr, SkyColor lower, SkyColor upper, float lowAlpha, float highAlpha) {
        calamitouscore$fan.draw(buffer, matrix, ll, ul, ur, lr, lower, upper, lowAlpha, highAlpha, highAlpha, lowAlpha, (lowAlpha + highAlpha) * 0.5F);
    }
}



