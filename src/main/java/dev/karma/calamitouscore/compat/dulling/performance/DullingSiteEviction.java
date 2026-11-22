package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.Map;
import net.mcreator.thedulling.AnthroMindData.MemorySite;

/** Exact fast path for rememberSite's normal one-entry overflow. No cache or saved-data changes. */
public final class DullingSiteEviction {
    private DullingSiteEviction() {}

    public static boolean trimOne(Map<Long, MemorySite> sites) {
        MemorySite first = null;
        for (MemorySite candidate : sites.values()) {
            // Strictly less: stable sort originally retained map iteration order for equal priorities.
            if (first == null || compare(candidate, first) < 0) first = candidate;
        }
        if (first != null) sites.remove(first.pos().asLong());
        return first != null;
    }

    public static int compare(MemorySite a, MemorySite b) {
        int owned = Boolean.compare(a.anthroOwned(), b.anthroOwned());
        if (owned != 0) return owned;
        int renewable = Boolean.compare(a.renewable(), b.renewable());
        return renewable != 0 ? renewable : Long.compare(a.lastSeenGameTime(), b.lastSeenGameTime());
    }
}
