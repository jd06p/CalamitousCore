package dev.karma.calamitouscore.compat.dulling;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class EndDullingProgress extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ClassValue<Optional<Method>> BETTER_END_KILL_COUNT = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("betterendisland$numTimesDragonKilled"));
            } catch (NoSuchMethodException e) {
                LOGGER.error("Better End Island's dragon kill counter is unavailable", e);
                return Optional.empty();
            }
        }
    };

    // A historical defeat also consumes the announcement, so installing on an old save stays quiet.
    private boolean unlocked;
    private boolean checkedHistory;

    private static EndDullingProgress get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EndDullingProgress::load,
                EndDullingProgress::new, "calamitouscore_end_dulling");
    }

    private static EndDullingProgress load(CompoundTag tag) {
        EndDullingProgress state = new EndDullingProgress();
        state.unlocked = tag.getBoolean("Unlocked");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Unlocked", unlocked);
        return tag;
    }

    public static boolean isUnlocked(ServerLevel level) {
        EndDragonFight fight = level.getDragonFight();
        if (fight == null) return false;
        EndDullingProgress state = get(level);
        state.checkHistory(fight);
        return state.unlocked;
    }

    private void checkHistory(EndDragonFight fight) {
        if (unlocked || checkedHistory) return;
        if (ModList.get().isLoaded("betterendisland")) {
            checkedHistory = true;
            if (betterEndKillCount(fight) > 0) unlock();
        } else if (!fight.saveData().needsStateScanning()) {
            checkedHistory = true;
            if (fight.hasPreviouslyKilledDragon()) unlock();
        }
    }

    public static void completeDragonFight(EndDragonFight fight, EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel level) || !Level.END.equals(level.dimension())
                || fight != level.getDragonFight() || !dragon.getUUID().equals(fight.getDragonUUID())) {
            fight.setDragonKilled(dragon);
            return;
        }

        EndDullingProgress state = get(level);
        state.checkHistory(fight);
        boolean betterEnd = ModList.get().isLoaded("betterendisland");
        int before = betterEnd && !state.unlocked ? betterEndKillCount(fight) : 0;
        fight.setDragonKilled(dragon);
        if (state.unlocked) return;

        boolean completed = betterEnd
                ? before >= 0 && betterEndKillCount(fight) > before
                : fight.hasPreviouslyKilledDragon();
        if (!completed) return;
        state.unlock();
        Component message = Component.literal(
                "THE GUARDIAN OF THE END HAS BEEN DEFEATED. THE DULLING CHURCH SHALL BEGIN MANIFESTING ITSELF INTO THIS REALM.")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    private void unlock() {
        unlocked = true;
        setDirty();
    }

    private static int betterEndKillCount(EndDragonFight fight) {
        Optional<Method> method = BETTER_END_KILL_COUNT.get(fight.getClass());
        if (method.isEmpty()) return -1;
        try {
            return (int) method.get().invoke(fight);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not read Better End Island's dragon kill counter", e);
            return -1;
        }
    }
}
