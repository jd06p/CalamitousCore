package dev.karma.calamitouscore.compat.performance;

import com.mojang.blaze3d.vertex.*;
import org.joml.*;

/** Reuses only the temporary transform vector, never the submitted vertex data. */
public final class DullingVertices {
    private static final ThreadLocal<Vector4f> VECTOR = ThreadLocal.withInitial(Vector4f::new);
    private DullingVertices() {}
    public static VertexConsumer vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z) {
        // Preserve custom wrapper behavior. The native solar BufferSource supplies a BufferBuilder.
        if (!(buffer instanceof BufferBuilder)) return buffer.vertex(matrix, x, y, z);
        Vector4f vector = VECTOR.get().set(x, y, z, 1);
        matrix.transform(vector);
        // Arguments are evaluated before calling the consumer, so a reentrant consumer cannot mutate them.
        return buffer.vertex(vector.x(), vector.y(), vector.z());
    }
}
