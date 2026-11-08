package dev.karma.calamitouscore.compat.crc;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent roster: unloading is not a defeat, and offline players retain their raid. */
public final class CrcRaidData extends SavedData {
    public static final String OWNER = "CalamitousCoreRaidOwner";
    public static final String COUNT = "CalamitousCoreRaidWaves";
    private final Map<UUID, Set<UUID>> rosters = new HashMap<>();
    public static CrcRaidData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CrcRaidData::load,
                CrcRaidData::new, "calamitouscore_crc_raids");
    }
    public void add(UUID owner, UUID entity) {
        if (rosters.computeIfAbsent(owner, k -> new HashSet<>()).add(entity)) setDirty();
    }
    public void remove(UUID owner, UUID entity) {
        Set<UUID> roster = rosters.get(owner);
        if (roster != null && roster.remove(entity)) {
            if (roster.isEmpty()) rosters.remove(owner);
            setDirty();
        }
    }
    public boolean empty(UUID owner) { return !rosters.containsKey(owner); }
    private static CrcRaidData load(CompoundTag tag) {
        CrcRaidData data = new CrcRaidData();
        for (Tag entry : tag.getList("Roster", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) entry;
            data.add(row.getUUID("Owner"), row.getUUID("Entity"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag();
        rosters.forEach((owner, entities) -> entities.forEach(entity -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Owner", owner); row.putUUID("Entity", entity); rows.add(row);
        }));
        tag.put("Roster", rows); return tag;
    }
}
