package dev.karma.calamitouscore.compat.difficulty;

import com.misanthropy.linggango.difficulty_enhancement.server.Fate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class BossRushSchedule extends SavedData {
    private static final String NEXT = "difficulty_enhancement.boss_rush_at";
    private static final String WARNED = "difficulty_enhancement.boss_rush_warned";
    private final CompoundTag clock;
    private long savedDeadline;
    private boolean savedWarning;

    private BossRushSchedule(CompoundTag tag) {
        clock = tag;
        remember();
    }

    public static CompoundTag forPlayer(ServerPlayer player) {
        var storage = player.server.overworld().getDataStorage();
        var schedule = storage.computeIfAbsent(BossRushSchedule::new, () -> {
            CompoundTag legacy = Fate.persisted(player);
            CompoundTag tag = new CompoundTag();
            tag.putLong(NEXT, legacy.getLong(NEXT));
            tag.putBoolean(WARNED, legacy.getBoolean(WARNED));
            var migrated = new BossRushSchedule(tag);
            migrated.setDirty();
            return migrated;
        }, "calamitouscore_boss_rush");
        return schedule.clock;
    }

    private void remember() {
        savedDeadline = clock.getLong(NEXT);
        savedWarning = clock.getBoolean(WARNED);
    }

    @Override public boolean isDirty() {
        return super.isDirty() || clock.getLong(NEXT) != savedDeadline || clock.getBoolean(WARNED) != savedWarning;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.merge(clock);
        remember();
        return tag;
    }
}
