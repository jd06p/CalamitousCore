package dev.karma.calamitouscore.revival;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public final class PermanentPetPolicy {
    public static final String TAMED_BY_PLAYER = "CalamitousCorePermanentTame";
    private static final String DRAGON_BASE = "com.leon.saintsdragons.server.entity.base.DragonEntity";
    private static final String MINION = "com.fevzi.summonity.entity.SummonedMinion";
    private static final Set<String> SUMMON_MODS = Set.of(
            "summonity", "terramity_paisleys_summoning", "thedulling");

    private PermanentPetPolicy() {}

    public static UUID eligibleOwner(LivingEntity pet) {
        if (!(pet instanceof OwnableEntity owned) || owned.getOwnerUUID() == null) return null;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).getNamespace();
        if (SUMMON_MODS.contains(namespace) || implementsNamed(pet.getClass(), MINION)) return null;

        UUID owner = owned.getOwnerUUID();
        if (IvyRevivalCompat.isIvy(pet)) {
            return IvyRevivalCompat.eligible(pet.saveWithoutId(new CompoundTag()), owner) ? owner : null;
        }
        if (pet instanceof TamableAnimal animal && !animal.isTame()) return null;
        if (pet instanceof AbstractHorse horse && !horse.isTamed()) return null;
        boolean tamed = pet instanceof TamableAnimal || pet instanceof AbstractHorse;
        boolean witnessedTame = pet.getPersistentData().hasUUID(TAMED_BY_PLAYER)
                && owner.equals(pet.getPersistentData().getUUID(TAMED_BY_PLAYER));
        if (!tamed && !witnessedTame) return null;

        boolean vanilla = namespace.equals("minecraft") && pet.getClass().getName().startsWith("net.minecraft.");
        boolean dragon = namespace.equals("saintsdragons") && isSaintsDragon(pet);
        return vanilla || dragon || witnessedTame ? owner : null;
    }

    public static boolean isSaintsDragon(LivingEntity pet) {
        for (Class<?> type = pet.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(DRAGON_BASE)) return true;
        }
        return false;
    }

    private static boolean implementsNamed(Class<?> type, String name) {
        if (type == null) return false;
        if (type.getName().equals(name)) return true;
        for (Class<?> contract : type.getInterfaces()) {
            if (implementsNamed(contract, name)) return true;
        }
        return implementsNamed(type.getSuperclass(), name);
    }

    public static void restoreDragonCodex(ServerLevel level, LivingEntity pet, UUID owner)
            throws ReflectiveOperationException {
        if (!isSaintsDragon(pet)) return;
        Class<?> dragon = Class.forName(DRAGON_BASE);
        Class<?> data = Class.forName("com.leon.saintsdragons.server.data.DragonCodexSavedData");
        Object codex = data.getMethod("get", ServerLevel.class).invoke(null, level);
        Method add = data.getMethod("addDragon", UUID.class, dragon);
        add.invoke(codex, owner, pet);
    }
}
