package dev.karma.calamitouscore.compat.performance;

/** JourneyMap-only boxing cache. Values are immutable; heights outside this range use Java's normal boxing. */
public final class HeightIntegers {
    private static final class Values {
        static final Integer[] CACHE = new Integer[4096];
        static { for (int i = 0; i < CACHE.length; i++) CACHE[i] = Integer.valueOf(i - 1024); }
    }
    private HeightIntegers() {}
    public static Integer valueOf(int value) {
        if (value >= -128 && value <= 127) return Integer.valueOf(value);
        return value >= -1024 && value < 3072 ? Values.CACHE[value + 1024] : Integer.valueOf(value);
    }
}
