package dev.karma.calamitouscore.compat.stellara;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Keeps Stellara's exact four-triangle fan, but transforms each unique vertex only once. */
public final class SkyTriangleFan {
    private final Vector4f vector = new Vector4f();
    private final float[] xyz = new float[15];
    private final SkyClipVolume clip = new SkyClipVolume();
    public void begin() { SkyVisibility.prepare(clip); }

    public void draw(BufferBuilder buffer, Matrix4f matrix, SkyPoint ll, SkyPoint ul, SkyPoint ur, SkyPoint lr,
                     SkyColor lower, SkyColor upper, float aLL, float aUL, float aUR, float aLR, float centerAlpha) {
        transform(matrix, 0, ll.x(), ll.y(), ll.z());
        transform(matrix, 1, ul.x(), ul.y(), ul.z());
        transform(matrix, 2, ur.x(), ur.y(), ur.z());
        transform(matrix, 3, lr.x(), lr.y(), lr.z());
        float x = ll.x() + ul.x() + ur.x() + lr.x();
        float y = ll.y() + ul.y() + ur.y() + lr.y();
        float z = ll.z() + ul.z() + ur.z() + lr.z();
        float length = 100.0F / Mth.sqrt(x * x + y * y + z * z);
        transform(matrix, 4, x * length, y * length, z * length);
        boolean hidden = clip.outside(xyz, 5);
        SkyRenderDiagnostics.fan(hidden);
        if (hidden) return;
        float red = (lower.red() + upper.red()) * 0.5F;
        float green = (lower.green() + upper.green()) * 0.5F;
        float blue = (lower.blue() + upper.blue()) * 0.5F;
        emit(buffer, 0, lower.red(), lower.green(), lower.blue(), aLL);
        emit(buffer, 1, upper.red(), upper.green(), upper.blue(), aUL);
        emit(buffer, 4, red, green, blue, centerAlpha);
        emit(buffer, 1, upper.red(), upper.green(), upper.blue(), aUL);
        emit(buffer, 2, upper.red(), upper.green(), upper.blue(), aUR);
        emit(buffer, 4, red, green, blue, centerAlpha);
        emit(buffer, 2, upper.red(), upper.green(), upper.blue(), aUR);
        emit(buffer, 3, lower.red(), lower.green(), lower.blue(), aLR);
        emit(buffer, 4, red, green, blue, centerAlpha);
        emit(buffer, 3, lower.red(), lower.green(), lower.blue(), aLR);
        emit(buffer, 0, lower.red(), lower.green(), lower.blue(), aLL);
        emit(buffer, 4, red, green, blue, centerAlpha);
    }
    private void transform(Matrix4f matrix, int slot, float x, float y, float z) {
        matrix.transform(vector.set(x, y, z, 1.0F));
        int i = slot * 3; xyz[i] = vector.x(); xyz[i + 1] = vector.y(); xyz[i + 2] = vector.z();
    }
    private void emit(BufferBuilder buffer, int slot, float r, float g, float b, float alpha) {
        int i = slot * 3;
        buffer.vertex(xyz[i], xyz[i + 1], xyz[i + 2]).color(r, g, b, alpha).endVertex();
    }
}
