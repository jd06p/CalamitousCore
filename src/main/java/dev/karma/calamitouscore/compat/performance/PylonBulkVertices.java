package dev.karma.calamitouscore.compat.performance;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

/** Client-only adapter reached exclusively for the installed SodiumBufferBuilder. */
final class PylonBulkVertices {
    private static final int[] OFFSETS = new int[512];
    static {
        int index = 0;
        for (int lat = 0; lat < 8; lat++) {
            for (int lon = 0; lon < 16; lon++) {
                int first = 3 * (17 * lat + lon);
                OFFSETS[index++] = first;
                OFFSETS[index++] = first + 51;
                OFFSETS[index++] = first + 54;
                OFFSETS[index++] = first + 3;
            }
        }
    }

    static boolean submit(VertexConsumer consumer, Matrix4f matrix, float[] source, float[] xyz, float r, float g, float b, float a) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
        if (writer == null) return false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long start = stack.nmalloc(16, OFFSETS.length * ColorVertex.STRIDE);
            encode(start, matrix, source, xyz, r, g, b, a);
            writer.push(stack, start, OFFSETS.length, ColorVertex.FORMAT);
        }
        return true;
    }

    static void encode(long start, Matrix4f matrix, float[] source, float[] xyz, float r, float g, float b, float a) {
        for (int i = 0; i < source.length; i += 3) {
            float x = source[i], y = source[i + 1], z = source[i + 2];
            xyz[i] = MatrixHelper.transformPositionX(matrix, x, y, z);
            xyz[i + 1] = MatrixHelper.transformPositionY(matrix, x, y, z);
            xyz[i + 2] = MatrixHelper.transformPositionZ(matrix, x, y, z);
        }
        int color = channel(r) | channel(g) << 8 | channel(b) << 16 | channel(a) << 24;
        long cursor = start;
        for (int offset : OFFSETS) {
            org.lwjgl.system.MemoryUtil.memPutFloat(cursor, xyz[offset]);
            org.lwjgl.system.MemoryUtil.memPutFloat(cursor + 4, xyz[offset + 1]);
            org.lwjgl.system.MemoryUtil.memPutFloat(cursor + 8, xyz[offset + 2]);
            org.lwjgl.system.MemoryUtil.memPutInt(cursor + 12, color);
            cursor += ColorVertex.STRIDE;
        }
    }

    private static int channel(float value) {
        return (int)(value * 255.0F) & 255;
    }
}


