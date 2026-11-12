package dev.karma.calamitouscore.compat.stellara;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Original rainbow quad with a conservative all-corners clip test before submission. */
public final class SkyVisibleQuad {
    private final SkyClipVolume clip = new SkyClipVolume();
    private final Vector4f vector = new Vector4f();
    private final float[] xyz = new float[12];
    public void begin() { SkyVisibility.prepare(clip); }
    public void draw(BufferBuilder buffer, Matrix4f matrix, SkyPoint a, SkyPoint b, SkyPoint c, SkyPoint d,
                     SkyColor outer, SkyColor inner, float aa, float ab, float ac, float ad) {
        transform(matrix, 0, a); transform(matrix, 1, b); transform(matrix, 2, c); transform(matrix, 3, d);
        if (clip.outside(xyz, 4)) return;
        emit(buffer, 0, outer, aa); emit(buffer, 1, inner, ab); emit(buffer, 2, inner, ac); emit(buffer, 3, outer, ad);
    }
    private void transform(Matrix4f matrix, int index, SkyPoint point) {
        matrix.transform(vector.set(point.x(), point.y(), point.z(), 1));
        int i = index * 3; xyz[i] = vector.x(); xyz[i + 1] = vector.y(); xyz[i + 2] = vector.z();
    }
    private void emit(BufferBuilder buffer, int index, SkyColor color, float alpha) {
        int i = index * 3;
        buffer.vertex(xyz[i], xyz[i + 1], xyz[i + 2]).color(color.red(), color.green(), color.blue(), alpha).endVertex();
    }
}
