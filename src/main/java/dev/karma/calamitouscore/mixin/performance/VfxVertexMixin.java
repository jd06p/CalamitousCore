package dev.karma.calamitouscore.mixin.performance;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.karma.calamitouscore.compat.performance.DullingVertices;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.VfxSphereRenderer", remap = false)
public abstract class VfxVertexMixin {
    @Redirect(method = "vertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;vertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", remap = true))
    private static VertexConsumer calamitouscore$transform(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z) {
        return DullingVertices.vertex(buffer, matrix, x,y,z);
    }
}
