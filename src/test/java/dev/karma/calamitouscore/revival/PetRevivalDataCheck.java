package dev.karma.calamitouscore.revival;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

/** Exercises the actual compressed-NBT save/load path without creating a game world. */
public final class PetRevivalDataCheck {
    public static void main(String[] args) throws Exception {
        checkIvy();
        checkMessageStyles();
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        CompoundTag pet = new CompoundTag();
        pet.putString("id", "minecraft:horse");
        pet.putUUID("UUID", first);
        pet.putUUID("Owner", owner);
        pet.putString("CustomName", "{\"text\":\"Persistent Companion\"}");
        pet.putBoolean("CustomNameVisible", true);
        pet.putInt("Age", -1200);
        pet.putInt("Variant", 6);
        CompoundTag equipment = new CompoundTag();
        equipment.putString("id", "minecraft:diamond");
        equipment.putByte("Count", (byte) 17);
        ListTag items = new ListTag();
        items.add(equipment);
        pet.put("Items", items);
        CompoundTag capabilities = new CompoundTag();
        capabilities.putString("test:custom_state", "preserve me");
        pet.put("ForgeCaps", capabilities);

        CompoundTag record = new CompoundTag();
        record.putUUID("Pet", first);
        record.putUUID("Owner", owner);
        record.put("Entity", pet);
        record.putLong("Due", 1600);
        PetRevivalData data = new PetRevivalData();
        data.pending.put(first, new PetRevivalData.Pending(record));
        CompoundTag other = record.copy();
        other.putUUID("Pet", second);
        other.putLong("Due", 1700);
        data.pending.put(second, new PetRevivalData.Pending(other));
        record.getCompound("Entity").putInt("Variant", 99);
        if (data.pending.get(first).record.getCompound("Entity").getInt("Variant") != 6) {
            throw new AssertionError("Snapshot aliases the live tag");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(data.save(new CompoundTag()), bytes);
        PetRevivalData restored = PetRevivalData.load(NbtIo.readCompressed(
                new ByteArrayInputStream(bytes.toByteArray())));
        if (restored.pending.size() != 2) throw new AssertionError("Independent pets lost");
        if (!restored.pending.get(first).record.getCompound("Entity")
                .equals(data.pending.get(first).record.getCompound("Entity"))) {
            throw new AssertionError("Pet NBT did not survive compressed persistence");
        }
        if (restored.pending.get(first).nextAttempt != 1600 || restored.pending.get(second).nextAttempt != 1700) {
            throw new AssertionError("Independent server-tick deadlines changed");
        }
        restored.pending.remove(first);
        if (!restored.pending.containsKey(second)) throw new AssertionError("Completion removed another pet");
        restored = PetRevivalData.load(restored.save(new CompoundTag()));
        if (restored.pending.containsKey(first)) throw new AssertionError("Completed revival returned to queue");
        System.out.println("PASS: compressed NBT, inventory/capability identity, defensive snapshot copies, independent deadlines, and completed-entry persistence.");
    }

    private static void checkIvy() throws Exception {
        UUID owner = UUID.randomUUID();
        CompoundTag ivy = new CompoundTag();
        ivy.putUUID("OwnerUUID", owner);
        ivy.putBoolean("Tame", true);
        ivy.putInt("Command", 2);
        ivy.putBoolean("Downed", true);
        ivy.putBoolean("NoAI", true);
        ListTag memory = new ListTag();
        CompoundTag player = new CompoundTag();
        player.putUUID("UUID", owner);
        ListTag flags = new ListTag();
        flags.add(StringTag.valueOf("ivy_recruited"));
        player.put("DialogueFlags", flags);
        memory.add(player);
        ivy.put("KnownDialoguePlayers", memory);
        if (IvyRevivalCompat.eligible(ivy, owner)) throw new AssertionError("Incomplete quest eligible");
        flags.add(StringTag.valueOf("cindervane_quest_completed"));
        if (!IvyRevivalCompat.eligible(ivy, owner)) throw new AssertionError("Completed companion excluded");
        if (IvyRevivalCompat.eligible(ivy, UUID.randomUUID())) throw new AssertionError("Wrong owner eligible");
        player.putUUID("UUID", UUID.randomUUID());
        if (IvyRevivalCompat.eligible(ivy, owner)) throw new AssertionError("Another player's quest counted");
        player.putUUID("UUID", owner);
        ivy.putBoolean("Tame", false);
        if (IvyRevivalCompat.eligible(ivy, owner)) throw new AssertionError("Unrecruited Ivy eligible");
        ivy.putBoolean("Tame", true);
        flags.remove(0);
        if (IvyRevivalCompat.eligible(ivy, owner)) throw new AssertionError("Quest-only Ivy eligible");
        flags.add(StringTag.valueOf("ivy_recruited"));
        ListTag inventory = new ListTag();
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:diamond_sword");
        item.putByte("Count", (byte) 1);
        inventory.add(item);
        ivy.put("IvyInventory", inventory);
        CompoundTag expectedMemory = player.copy();
        IvyRevivalCompat.prepareSnapshot(ivy);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(ivy, bytes);
        CompoundTag restored = NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        if (!IvyRevivalCompat.eligible(restored, owner) || restored.getInt("Command") != 2
                || !restored.getList("IvyInventory", 10).equals(inventory)
                || !restored.getList("KnownDialoguePlayers", 10).getCompound(0).equals(expectedMemory)) {
            throw new AssertionError("Ivy relationship, quest, command or inventory did not survive");
        }
        if (restored.getBoolean("Downed") || restored.getBoolean("NoAI")) {
            throw new AssertionError("Ivy retained her incapacitated state");
        }
        System.out.println("PASS: Ivy quest/owner/recruitment restrictions and compressed inventory/command/dialogue state.");
    }

    private static void checkMessageStyles() {
        String text = "Pet's exact message..";
        var death = PetRevivalData.formatMessage(text, false);
        var returned = PetRevivalData.formatMessage(text, true);
        if (!death.getString().equals(text) || !death.getStyle().isBold()
                || death.getStyle().getColor().getValue() != 0xAA0000) {
            throw new AssertionError("Death message must be exact, bold and dark red");
        }
        if (!returned.getString().equals(text) || returned.getStyle().isBold()
                || returned.getStyle().getColor().getValue() != 0x55AAFF) {
            throw new AssertionError("Return message formatting changed");
        }
        System.out.println("PASS: bold dark-red death and light-blue return formatting.");
    }
}
