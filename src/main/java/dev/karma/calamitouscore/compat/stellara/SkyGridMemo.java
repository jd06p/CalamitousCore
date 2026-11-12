package dev.karma.calamitouscore.compat.stellara;

import java.util.Arrays;

/** Bounded CPU-only memoization of the four original corner evaluations per grid cell. */
public final class SkyGridMemo<T> {
    private Object[] values = new Object[0];
    private int columns, rows, layers, corner, column, row, layer;
    private boolean transposed;

    public void begin(int columns, int rows, int layers, boolean transposed, boolean retain) {
        long size = ((long) columns + 1) * ((long) rows + 1) * layers;
        int capacity = columns > 0 && rows > 0 && layers > 0 && size <= 65_536 ? (int) size : 0;
        if (this.columns != columns || this.rows != rows || this.layers != layers || values.length != capacity) {
            values = new Object[capacity];
        } else if (!retain) {
            Arrays.fill(values, null);
        }
        this.columns = columns; this.rows = rows; this.layers = layers;
        this.transposed = transposed; corner = column = row = layer = 0;
    }

    public int next() {
        if (values.length == 0) return -1;
        if (layer >= layers) return -1;
        int slot = (layer * (columns + 1) + column) * (rows + 1) + row;
        if (transposed ? corner == 1 || corner == 2 : corner == 2 || corner == 3) slot += rows + 1;
        if (transposed ? corner == 2 || corner == 3 : corner == 1 || corner == 2) slot++;
        if (++corner == 4) {
            corner = 0;
            if (++row == rows) { row = 0; if (++column == columns) { column = 0; layer++; } }
        }
        return slot;
    }

    @SuppressWarnings("unchecked")
    public T get(int slot) { return slot < 0 ? null : (T) values[slot]; }
    public void put(int slot, T value) { if (slot >= 0) values[slot] = value; }
}
