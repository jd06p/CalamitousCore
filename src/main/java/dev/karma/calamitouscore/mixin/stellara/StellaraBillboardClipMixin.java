package dev.karma.calamitouscore.mixin.stellara;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.karma.calamitouscore.compat.stellara.SkyVisibility;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.render.SkyGeometry", remap = false)
public abstract class StellaraBillboardClipMixin {
    @Shadow private static void addBillboard(BufferBuilder buffer, Matrix4f matrix, SkyPoint center, float size, float angle, SkyColor color, float alpha, boolean textured) { throw new AssertionError(); }
    @Redirect(method = {"addTexturedBillboard", "addBillboard(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Matrix4f;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;FFLit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;F)V"}, at = @At(value = "INVOKE", target = "Lit/hurts/shatterbyte/stellara/client/sky/render/SkyGeometry;addBillboard(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Matrix4f;Lit/hurts/shatterbyte/stellara/client/sky/math/SkyPoint;FFLit/hurts/shatterbyte/stellara/client/sky/math/SkyColor;FZ)V"))
    private static void calamitouscore$billboard(BufferBuilder buffer, Matrix4f matrix, SkyPoint center, float size, float angle, SkyColor color, float alpha, boolean textured) {
        if (!SkyVisibility.hiddenBillboard(matrix, center.x(), center.y(), center.z(), size)) addBillboard(buffer, matrix, center, size, angle, color, alpha, textured);
    }
}
