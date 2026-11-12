package dev.karma.calamitouscore.mixin.stellara;

import dev.karma.calamitouscore.compat.stellara.SkyGridMemo;
import dev.karma.calamitouscore.compat.stellara.SkyTriangleFan;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import it.hurts.shatterbyte.stellara.client.sky.content.SkyRibbonContent;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import org.spongepowered.asm.mixin.*;
import net.minecraft.util.Mth;
import it.hurts.shatterbyte.stellara.client.sky.render.SkyGeometry;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.SkyRibbonContent", remap = false)
public abstract class StellaraRibbonMixin {
    @Shadow @Final private int segments;
    @Shadow @Final private float arc, thickness, waveFrequency, waveAmplitude, wobbleAmplitude, basePitch, tilt, centerYaw;
    @Shadow private float periodicFrequency(float frequency) { throw new AssertionError(); }
    @Shadow @Final private int gradientSteps;
    @Shadow private SkyColor ribbonColor(float progress) { throw new AssertionError(); }
    @Unique private final SkyGridMemo<SkyPoint> calamitouscore$grid = new SkyGridMemo<>();
    @Unique private SkyColor[] calamitouscore$palette;
    @Unique private int calamitouscore$colorCall;

    @Inject(method = "renderRibbon", at = @At("HEAD"))
    private void calamitouscore$begin(CallbackInfo ci) {
        calamitouscore$fan.begin();
        calamitouscore$grid.begin(segments, gradientSteps, 1, false, false);
        if (calamitouscore$palette == null) calamitouscore$palette = new SkyColor[gradientSteps + 1];
        calamitouscore$colorCall = 0;
    }
    // The private Ribbon record is deliberately not named in Java source. Mixin coerces its owner/record parameter.
    @Redirect(method = "renderRibbon", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/SkyRibbonContent;point(Lit/hurts/shatterbyte/stellara/client/sky/content/SkyRibbonContent$Ribbon;FFFF)Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;"))
    private SkyPoint calamitouscore$point(SkyRibbonContent owner, @Coerce Object ribbon, float offset, float progress, float width, float time) {
        int slot = calamitouscore$grid.next();
        SkyPoint point = calamitouscore$grid.get(slot);
        if (point == null) {
            point = calamitouscore$evaluate((StellaraRibbonDataAccessor) ribbon, offset, progress, width, time);
            calamitouscore$grid.put(slot, point);
        }
        return point;
    }
    /** Original 0.1.4 point formula; only evaluated once for a shared corner per frame. */
    @Unique private SkyPoint calamitouscore$evaluate(StellaraRibbonDataAccessor ribbon, float offset, float progress, float width, float time) {
        float angle = (progress - 0.5F) * arc;
        float widthOffset = (width - 0.5F) * thickness * ribbon.calamitouscore$thicknessScale();
        float primary = periodicFrequency(waveFrequency);
        float detail = periodicFrequency(waveFrequency * 2.7F);
        float wobble = periodicFrequency(waveFrequency * 1.35F);
        float largeWave = Mth.sin(progress * ((float)Math.PI * 2F) * primary + time + ribbon.calamitouscore$phaseA()) * waveAmplitude;
        float smallWave = Mth.sin(progress * ((float)Math.PI * 2F) * detail - time * 0.64F + ribbon.calamitouscore$phaseB()) * waveAmplitude * 0.34F;
        float sideWobble = Mth.sin(progress * ((float)Math.PI * 2F) * wobble + time * 0.41F + ribbon.calamitouscore$phaseC()) * wobbleAmplitude;
        float pitch = Mth.clamp(basePitch + offset + widthOffset + largeWave + smallWave, -1.42F, 1.42F);
        SkyPoint point = SkyGeometry.point(angle + sideWobble, pitch);
        float cosTilt = Mth.cos(tilt), sinTilt = Mth.sin(tilt);
        SkyPoint tilted = Math.abs(tilt) < 0.001F ? point : new SkyPoint(point.x(), point.y() * cosTilt - point.z() * sinTilt, point.y() * sinTilt + point.z() * cosTilt);
        float cosYaw = Mth.cos(centerYaw), sinYaw = Mth.sin(centerYaw);
        return new SkyPoint(tilted.x() * cosYaw + tilted.z() * sinYaw, tilted.y(), tilted.z() * cosYaw - tilted.x() * sinYaw);
    }
    @Redirect(method = "renderRibbon", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/SkyRibbonContent;ribbonColor(F)Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;"))
    private SkyColor calamitouscore$color(SkyRibbonContent owner, float progress) {
        int slot = (calamitouscore$colorCall + 1) >> 1;
        if (++calamitouscore$colorCall == gradientSteps * 2) calamitouscore$colorCall = 0;
        SkyColor color = calamitouscore$palette[slot];
        if (color == null) calamitouscore$palette[slot] = color = ribbonColor(progress);
        return color;
    }
    @Unique private final SkyTriangleFan calamitouscore$fan = new SkyTriangleFan();
    @Redirect(method = "renderRibbon", at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/content/SkyRibbonContent;addQuad(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Matrix4f;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;FFFF)V"))
    private void calamitouscore$fan(BufferBuilder buffer, Matrix4f matrix, SkyPoint ll, SkyPoint ul, SkyPoint ur, SkyPoint lr, SkyColor lower, SkyColor upper, float aLL, float aUL, float aUR, float aLR) {
        calamitouscore$fan.draw(buffer, matrix, ll, ul, ur, lr, lower, upper, aLL, aUL, aUR, aLR, (aLL + aUL + aUR + aLR) * 0.25F);
    }
}




