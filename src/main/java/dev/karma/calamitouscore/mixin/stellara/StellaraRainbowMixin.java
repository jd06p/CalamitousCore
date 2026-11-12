package dev.karma.calamitouscore.mixin.stellara;

import dev.karma.calamitouscore.compat.stellara.SkyGridMemo;
import dev.karma.calamitouscore.compat.stellara.SkyVisibleQuad;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.content.RainbowSkyContent;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.RainbowSkyContent", remap = false)
public abstract class StellaraRainbowMixin {
    @Shadow @Final private int segments;
    @Shadow @Final private List<?> colors;
    @Shadow private SkyPoint point(float progress, float radius) { throw new AssertionError(); }
    @Unique private final SkyGridMemo<SkyPoint> calamitouscore$grid = new SkyGridMemo<>();

    @Inject(method = "renderGradient", at = @At("HEAD"))
    private void calamitouscore$begin(CallbackInfo ci) {
        calamitouscore$quad.begin();
        // Rainbow geometry is time/alpha independent. Its fade still uses the original current alpha.
        calamitouscore$grid.begin(Math.max(24, colors.size() * 12), segments, 1, true, true);
    }
    @Redirect(method = "renderGradient", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/RainbowSkyContent;point(FF)Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;"))
    private SkyPoint calamitouscore$point(RainbowSkyContent owner, float progress, float radius) {
        int slot = calamitouscore$grid.next();
        SkyPoint point = calamitouscore$grid.get(slot);
        if (point == null) { point = point(progress, radius); calamitouscore$grid.put(slot, point); }
        return point;
    }
    @Unique private final SkyVisibleQuad calamitouscore$quad = new SkyVisibleQuad();
    @Redirect(method = "renderGradient", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/RainbowSkyContent;addBand(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Matrix4f;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;FFFF)V"))
    private void calamitouscore$quad(BufferBuilder buffer, Matrix4f matrix, SkyPoint a, SkyPoint b, SkyPoint c, SkyPoint d, SkyColor outer, SkyColor inner, float aa, float ab, float ac, float ad) {
        calamitouscore$quad.draw(buffer, matrix, a, b, c, d, outer, inner, aa, ab, ac, ad);
    }
}

