package dev.karma.calamitouscore.revival;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PetRevivalData extends SavedData {
    public static final class Pending {
        final CompoundTag record;
        long nextAttempt;
        boolean warned;

        Pending(CompoundTag record) {
            this.record = record.copy();
            nextAttempt = record.getLong("Due");
        }

        UUID id() { return record.getUUID("Pet"); }
    }

    final Map<UUID, Pending> pending = new LinkedHashMap<>();
    private final List<CompoundTag> mail = new ArrayList<>();

    public static PetRevivalData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PetRevivalData::load, PetRevivalData::new, "calamitouscore_pet_revivals");
    }

    static PetRevivalData load(CompoundTag tag) {
        PetRevivalData data = new PetRevivalData();
        for (Tag row : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            CompoundTag record = (CompoundTag) row;
            if (record.hasUUID("Pet") && record.hasUUID("Owner") && record.contains("Entity")) {
                Pending pending = new Pending(record);
                data.pending.put(pending.id(), pending);
            }
        }
        for (Tag row : tag.getList("Mail", Tag.TAG_COMPOUND)) data.mail.add(((CompoundTag) row).copy());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag pets = new ListTag();
        pending.values().forEach(pet -> pets.add(pet.record.copy()));
        tag.put("Pending", pets);
        ListTag messages = new ListTag();
        mail.forEach(row -> messages.add(row.copy()));
        tag.put("Mail", messages);
        return tag;
    }

    void message(MinecraftServer server, UUID owner, String text, boolean returned) {
        CompoundTag row = new CompoundTag();
        row.putUUID("Owner", owner);
        row.putString("Text", text);
        row.putBoolean("Returned", returned);
        mail.add(row);
        setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) deliver(player);
    }

    void deliver(ServerPlayer player) {
        mail.removeIf(row -> {
            if (!player.getUUID().equals(row.getUUID("Owner"))) return false;
            Component message = formatMessage(row.getString("Text"), row.getBoolean("Returned"));
            player.sendSystemMessage(message);
            return true;
        });
        setDirty();
    }

    static Component formatMessage(String text, boolean returned) {
        if (returned) return Component.literal(text).withStyle(style -> style.withColor(0x55AAFF));
        return Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
    }
}
