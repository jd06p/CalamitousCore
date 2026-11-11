package dev.karma.calamitouscore.revival;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

/** Saints' Dragons 0.9.88's entity-owned recruitment and quest memory. */
public final class IvyRevivalCompat {
    private IvyRevivalCompat() {}

    public static boolean isIvy(LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()
                .equals("saintsdragons:ivy_oleander");
    }

    public static boolean eligible(CompoundTag saved, UUID owner) {
        if (owner == null || !saved.getBoolean("Tame") || !saved.hasUUID("OwnerUUID")
                || !owner.equals(saved.getUUID("OwnerUUID"))) return false;
        for (Tag row : saved.getList("KnownDialoguePlayers", Tag.TAG_COMPOUND)) {
            CompoundTag player = (CompoundTag) row;
            if (!player.hasUUID("UUID") || !owner.equals(player.getUUID("UUID"))) continue;
            boolean recruited = false;
            boolean completed = false;
            for (Tag flag : player.getList("DialogueFlags", Tag.TAG_STRING)) {
                recruited |= flag.getAsString().equals("ivy_recruited");
                completed |= flag.getAsString().equals("cindervane_quest_completed");
            }
            if (recruited && completed) return true;
        }
        return false;
    }

    public static void prepareSnapshot(CompoundTag saved) {
        saved.putBoolean("Downed", false);
        saved.putInt("DownedBleedTicks", 0);
        saved.putInt("DownedFinishHits", 0);
        saved.putInt("DownedReviveProgress", 0);
        saved.putBoolean("NoAI", false);
    }
}
