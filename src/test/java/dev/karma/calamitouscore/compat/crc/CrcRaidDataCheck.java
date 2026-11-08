package dev.karma.calamitouscore.compat.crc;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Standalone regression check; no running server or world required. */
public final class CrcRaidDataCheck {
    public static void main(String[] args) throws Exception {
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        CrcRaidData data = new CrcRaidData();
        data.add(owner, a); data.add(owner, a); data.add(owner, b); data.add(other, a);
        CompoundTag saved = data.save(new CompoundTag());
        if (saved.getList("Roster", 10).size() != 3) throw new AssertionError("Duplicate roster entry");
        var load = CrcRaidData.class.getDeclaredMethod("load", CompoundTag.class);
        load.setAccessible(true);
        CrcRaidData restored = (CrcRaidData) load.invoke(null, saved);
        restored.remove(owner, a);
        if (restored.empty(owner)) throw new AssertionError("Completed with pending reinforcement");
        restored.remove(owner, b);
        if (!restored.empty(owner) || restored.empty(other)) throw new AssertionError("Cross-player roster corruption");
        restored.remove(owner, b);
        if (restored.save(new CompoundTag()).getList("Roster", 10).size() != 1)
            throw new AssertionError("Removal was not idempotent");
        System.out.println("CRC raid roster: persistence, duplicate tracking, owner isolation and completion checks passed.");
    }
}
