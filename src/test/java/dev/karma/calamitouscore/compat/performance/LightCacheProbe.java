package dev.karma.calamitouscore.compat.performance;

import java.util.concurrent.ConcurrentHashMap;

/** Fixture with the inspected captured-callback shape; remapped to an isolated cache owner during tests. */
public final class LightCacheProbe {
    public static final ConcurrentHashMap<Object,Integer> CACHE=new ConcurrentHashMap<>();
    public int computations;
    public int calculate(Object key,int fallback){return CACHE.computeIfAbsent(key,ignored->{computations++;return fallback;});}
    public void invalidate(){CACHE.clear();}
}
