package dev.karma.calamitouscore.compat.stellara;

import org.joml.Matrix4f;

/** Conservative homogeneous-clip test. It rejects geometry only wholly outside one plane. */
public final class SkyClipVolume {
    private final float[] planes = new float[24];
    private final float[] normals = new float[6];
    private boolean enabled;
    public void disable() { enabled = false; }
    public void set(Matrix4f matrix) {
        for (int axis = 0; axis < 3; axis++) for (int sign = 0; sign < 2; sign++) {
            int offset = (axis * 2 + sign) * 4;
            float scale = sign == 0 ? 1 : -1;
            for (int col = 0; col < 4; col++) planes[offset + col] = matrix.get(col, 3) + scale * matrix.get(col, axis);
            float a = planes[offset], b = planes[offset + 1], c = planes[offset + 2];
            normals[offset / 4] = (float)Math.sqrt(a * a + b * b + c * c);
        }
        enabled = true;
    }
    public boolean outsideSphere(float x, float y, float z, float radius) {
        if (!enabled || !Float.isFinite(radius)) return false;
        for (int p = 0; p < 24; p += 4) {
            float ax = planes[p] * x, by = planes[p + 1] * y, cz = planes[p + 2] * z, d = planes[p + 3];
            float margin = 0.0001F * (Math.abs(ax) + Math.abs(by) + Math.abs(cz) + Math.abs(d) + 1);
            if (ax + by + cz + d < -Math.abs(radius) * normals[p / 4] - margin) return true;
        }
        return false;
    }
    public boolean outside(float[] xyz, int count) {
        if (!enabled) return false;
        for (int plane = 0; plane < 24; plane += 4) {
            float a = planes[plane], b = planes[plane + 1], c = planes[plane + 2], d = planes[plane + 3];
            boolean allOutside = true;
            for (int point = 0; point < count; point++) {
                int i = point * 3;
                float x = xyz[i], y = xyz[i + 1], z = xyz[i + 2];
                // Conservative allowance for CPU/GPU multiply/FMA and clipping-rounding differences.
                float margin = 0.0001F * (Math.abs(a * x) + Math.abs(b * y) + Math.abs(c * z) + Math.abs(d) + 1);
                float distance = a * x + b * y + c * z + d;
                if (!Float.isFinite(distance) || distance >= -margin) { allOutside = false; break; }
            }
            if (allOutside) return true;
        }
        return false;
    }
}
