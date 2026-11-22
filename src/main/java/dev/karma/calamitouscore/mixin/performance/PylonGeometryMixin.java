package dev.karma.calamitouscore.mixin.performance;

import com.mojang.blaze3d.vertex.*;
import dev.karma.calamitouscore.compat.performance.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.HealingPylonRenderEvents", remap = false)
public abstract class PylonGeometryMixin {
    /** @author CalamitousCore @reason Reuse identical animated vertex calculations without an allocation at every sphere. */
    @Overwrite
    private static void renderRippleSphere(PoseStack pose, VertexConsumer buffer, float radius, float r, float g, float b, float alpha, float time, float strength, float speed, float seed) {
        PylonSphereMesh.render(pose, buffer, radius,r,g,b,alpha,time,strength,speed,seed);
    }
    @Redirect(method = {"addQuad", "addGradientQuad"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;vertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", remap = true))
    private static VertexConsumer calamitouscore$beamTransform(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z) {
        return DullingVertices.vertex(buffer, matrix, x,y,z);
    }
}
