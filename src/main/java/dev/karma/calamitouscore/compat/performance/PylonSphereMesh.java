package dev.karma.calamitouscore.compat.performance;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Identical 8x16 animated quads; shared UV/ripple evaluations, with no quality or animation reduction. */
public final class PylonSphereMesh {
    private static final int VERTICES = 9 * 17;
    private static final float[] NX = new float[VERTICES], NY = new float[VERTICES], NZ = new float[VERTICES], A = new float[VERTICES], B = new float[VERTICES];
    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);
    static {
        for (int lat = 0; lat <= 8; lat++) for (int lon = 0; lon <= 16; lon++) {
            int i = lat * 17 + lon; float v = lat / 8.0F, u = lon / 16.0F;
            float phi = (v - .5F) * (float)Math.PI, theta = u * ((float)Math.PI * 2F);
            NX[i] = Mth.cos(theta) * Mth.cos(phi); NY[i] = Mth.sin(phi); NZ[i] = Mth.sin(theta) * Mth.cos(phi);
            A[i] = theta * 4F + phi * 3F; B[i] = theta * 7F - phi * 5F;
        }
    }
    private PylonSphereMesh() {}
    private record Mesh(float radius, float time, float strength, float speed, float seed, float[] xyz) {}
    private static final class Cache { final Mesh[] meshes = new Mesh[8]; int cursor; }
    private static final class Workspace { final float[] xyz = new float[VERTICES * 3]; final Vector4f vector = new Vector4f(); }
    private static final class Scratch { final Workspace[] workspaces = new Workspace[8]; int depth; }
    private static float[] positions(float radius, float time, float strength, float speed, float seed) {
        Cache cache = CACHE.get();
        for (Mesh mesh : cache.meshes) if (mesh != null && mesh.radius == radius && mesh.time == time && mesh.strength == strength && mesh.speed == speed && mesh.seed == seed) return mesh.xyz;
        float[] xyz = new float[VERTICES * 3];
        for (int i = 0; i < VERTICES; i++) {
            float ripple = (Mth.sin(A[i] - time * speed + seed) * .55F + Mth.sin(B[i] + time * speed * .75F + seed) * .45F) * strength;
            float scaled = radius * (1F + ripple); int offset = i * 3;
            xyz[offset] = NX[i] * scaled; xyz[offset + 1] = NY[i] * scaled; xyz[offset + 2] = NZ[i] * scaled;
        }
        // Arrays are immutable after publication, so nested render calls cannot overwrite an in-use mesh.
        cache.meshes[cache.cursor++ & 7] = new Mesh(radius, time, strength, speed, seed, xyz);
        return xyz;
    }
    public static void render(PoseStack pose, VertexConsumer buffer, float radius, float r, float g, float b, float alpha, float time, float strength, float speed, float seed) {
        float[] xyz = positions(radius, time, strength, speed, seed); Matrix4f matrix = pose.last().pose();
        Scratch scratch = SCRATCH.get(); int depth = scratch.depth++;
        Workspace workspace = depth < 8 ? scratch.workspaces[depth] : new Workspace();
        if (workspace == null) scratch.workspaces[depth] = workspace = new Workspace();
        try {
            boolean sodium = buffer.getClass().getName().equals("me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder");
            if (sodium && PylonBulkVertices.submit(buffer, matrix, xyz, workspace.xyz, r, g, b, alpha)) return;
            boolean nativeBuffer = buffer instanceof BufferBuilder;
            if (nativeBuffer) {
                for (int i = 0; i < xyz.length; i += 3) {
                    Vector4f vector = workspace.vector.set(xyz[i], xyz[i+1], xyz[i+2], 1);
                    matrix.transform(vector); workspace.xyz[i] = vector.x(); workspace.xyz[i+1] = vector.y(); workspace.xyz[i+2] = vector.z();
                }
                xyz = workspace.xyz;

            }
            for (int lat = 0; lat < 8; lat++) for (int lon = 0; lon < 16; lon++) {
                int first = lat * 17 + lon;
                emit(buffer, matrix, xyz, first, r,g,b,alpha,nativeBuffer); emit(buffer, matrix, xyz, first + 17, r,g,b,alpha,nativeBuffer);
                emit(buffer, matrix, xyz, first + 18, r,g,b,alpha,nativeBuffer); emit(buffer, matrix, xyz, first + 1, r,g,b,alpha,nativeBuffer);
            }
        } finally { scratch.depth--; }
    }
    private static void emit(VertexConsumer buffer, Matrix4f matrix, float[] xyz, int i, float r, float g, float b, float alpha, boolean nativeBuffer) {
        int offset = i * 3;
        VertexConsumer target = nativeBuffer ? buffer.vertex(xyz[offset], xyz[offset + 1], xyz[offset + 2]) : buffer.vertex(matrix, xyz[offset], xyz[offset + 1], xyz[offset + 2]);
        target.color(r,g,b,alpha).endVertex();
    }
}


