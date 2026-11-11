package dev.karma.calamitouscore.revival;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class PetRevivalEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long REVIVAL_TICKS = 600;
    private static final int SEARCH_RADIUS = 16;
    private static final TicketType<UUID> REVIVAL_TICKET =
            TicketType.create("calamitouscore_pet_revival", UUID::compareTo, 40);

    private PetRevivalEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tame(AnimalTameEvent event) {
        if (!event.isCanceled() && !event.getAnimal().level().isClientSide) {
            event.getAnimal().getPersistentData().putUUID(
                    PermanentPetPolicy.TAMED_BY_PLAYER, event.getTamer().getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity pet = event.getEntity();
        if (event.isCanceled() || pet.isRemoved() || !(pet.level() instanceof ServerLevel level)) return;
        try {
            UUID owner = PermanentPetPolicy.eligibleOwner(pet);
            if (owner == null) return;
            PetRevivalData data = PetRevivalData.get(level.getServer());
            if (data.pending.containsKey(pet.getUUID())) {
                event.setCanceled(true);
                pet.discard();
                return;
            }
            CompoundTag entity = pet.saveWithoutId(new CompoundTag());
            entity.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
            if (IvyRevivalCompat.isIvy(pet)) IvyRevivalCompat.prepareSnapshot(entity);
            // Riders, leash holders and status damage are separate from the pet's identity.
            entity.remove("Passengers");
            entity.remove("Leash");
            entity.remove("ActiveEffects");
            entity.putFloat("Health", pet.getMaxHealth());
            entity.putShort("DeathTime", (short) 0);
            entity.putShort("HurtTime", (short) 0);
            entity.putShort("Fire", (short) 0);
            entity.putFloat("FallDistance", 0);

            CompoundTag record = new CompoundTag();
            record.putUUID("Pet", pet.getUUID());
            record.putUUID("Owner", owner);
            record.put("Entity", entity);
            record.putString("Dimension", level.dimension().location().toString());
            record.putString("Name", pet.getName().getString());
            record.putLong("Due", level.getServer().overworld().getGameTime() + REVIVAL_TICKS);
            record.putDouble("X", pet.getX());
            record.putDouble("Y", pet.getY());
            record.putDouble("Z", pet.getZ());
            // Preserve the queue before removing the body. Cancellation prevents inventory/loot duplication.
            data.pending.put(pet.getUUID(), new PetRevivalData.Pending(record));
            data.setDirty();
            event.setCanceled(true);
            pet.ejectPassengers();
            if (pet instanceof Mob mob && mob.isLeashed()) mob.dropLeash(true, false);
            pet.discard();
            data.message(level.getServer(), owner, "THE BODY OF " + record.getString("Name")
                    + " HAS CRUMBLED COMPLETELY, BUT IT'S DETERMINED SOUL REFUSES TO LEAVE.. IT'S BODY WILL REAWAKEN IN 30 SECONDS", false);
        } catch (RuntimeException e) {
            LOGGER.error("Could not preserve pet {} for revival", pet.getUUID(), e);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PetRevivalData.get(player.server).deliver(player);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        PetRevivalData data = PetRevivalData.get(server);
        long now = server.overworld().getGameTime();
        for (PetRevivalData.Pending pending : List.copyOf(data.pending.values())) {
            if (now < pending.nextAttempt - 20) continue;
            try {
                attempt(server, data, pending, now);
            } catch (RuntimeException e) {
                warn(pending, "Pet revival failed; its full snapshot remains queued", e);
                pending.nextAttempt = now + 100;
            }
        }
    }

    private static void attempt(MinecraftServer server, PetRevivalData data,
            PetRevivalData.Pending pending, long now) {
        CompoundTag record = pending.record;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(record.getString("Dimension"))));
        if (level == null) {
            warn(pending, "Pet revival dimension is unavailable; retaining snapshot", null);
            pending.nextAttempt = now + 100;
            return;
        }
        BlockPos death = BlockPos.containing(record.getDouble("X"), record.getDouble("Y"), record.getDouble("Z"));
        ChunkPos center = new ChunkPos(death);
        level.getChunkSource().addRegionTicket(REVIVAL_TICKET, center, 5, pending.id());
        boolean loaded = true;
        for (int x = center.x - 3; x <= center.x + 3; x++) {
            for (int z = center.z - 3; z <= center.z + 3; z++) {
                level.getChunk(x, z);
                loaded &= level.areEntitiesLoaded(ChunkPos.asLong(x, z));
            }
        }
        if (now < pending.nextAttempt || !loaded) return;

        // Prefer an existing UUID after a save interruption over constructing a second pet.
        for (ServerLevel candidate : server.getAllLevels()) {
            Entity existing = candidate.getEntity(pending.id());
            if (existing != null && !existing.isRemoved()) {
                if (existing instanceof LivingEntity living && !living.isAlive()) {
                    existing.discard();
                } else {
                    complete(level, data, pending);
                    return;
                }
            }
        }
        Entity restored = EntityType.loadEntityRecursive(record.getCompound("Entity").copy(), level, entity -> entity);
        if (!(restored instanceof LivingEntity pet) || !pending.id().equals(pet.getUUID())
                || !(pet instanceof OwnableEntity owned)
                || !record.getUUID("Owner").equals(owned.getOwnerUUID())) {
            warn(pending, "Pet data could not be restored with its original UUID and owner; retaining snapshot", null);
            defer(level, pending, now);
            return;
        }
        float maxHealth = pet.getMaxHealth();
        if (!Float.isFinite(maxHealth) || maxHealth <= 0) {
            warn(pending, "Pet attributes did not restore correctly; retaining snapshot", null);
            defer(level, pending, now);
            return;
        }
        pet.setHealth(maxHealth);
        pet.deathTime = 0;
        pet.hurtTime = 0;
        pet.clearFire();
        pet.fallDistance = 0;
        pet.setDeltaMovement(Vec3.ZERO);
        if (pet instanceof Mob mob) mob.setPersistenceRequired();

        Vec3 safe = safePosition(level, pet, death);
        if (safe == null) {
            warn(pending, "No safe position near the pet's death location; retaining snapshot and retrying", null);
            defer(level, pending, now);
            return;
        }
        pet.moveTo(safe.x, safe.y, safe.z, pet.getYRot(), pet.getXRot());
        if (!level.addFreshEntity(pet) || level.getEntity(pet.getUUID()) != pet) {
            defer(level, pending, now);
            return;
        }
        try {
            PermanentPetPolicy.restoreDragonCodex(level, pet, record.getUUID("Owner"));
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Pet {} revived, but its Saints' Dragons codex needs repair", pending.id(), e);
        }
        complete(level, data, pending);
        data.message(server, record.getUUID("Owner"),
                record.getString("Name") + " HAS RETURNED FROM THE OTHERSIDE", true);
    }

    private static void complete(ServerLevel level, PetRevivalData data, PetRevivalData.Pending pending) {
        data.pending.remove(pending.id());
        data.setDirty();
        BlockPos death = BlockPos.containing(pending.record.getDouble("X"),
                pending.record.getDouble("Y"), pending.record.getDouble("Z"));
        level.getChunkSource().removeRegionTicket(REVIVAL_TICKET, new ChunkPos(death), 5, pending.id());
    }

    private static void defer(ServerLevel level, PetRevivalData.Pending pending, long now) {
        pending.nextAttempt = now + 100;
        BlockPos death = BlockPos.containing(pending.record.getDouble("X"),
                pending.record.getDouble("Y"), pending.record.getDouble("Z"));
        level.getChunkSource().removeRegionTicket(REVIVAL_TICKET, new ChunkPos(death), 5, pending.id());
    }

    private static Vec3 safePosition(ServerLevel level, LivingEntity pet, BlockPos death) {
        int baseY = Math.max(level.getMinBuildHeight() + 1,
                Math.min(death.getY(), level.getMaxBuildHeight() - 2));
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dy = 0; dy <= SEARCH_RADIUS; dy++) {
                int[] heights = dy == 0 ? new int[]{baseY} : new int[]{baseY + dy, baseY - dy};
                for (int y : heights) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                            BlockPos feet = new BlockPos(death.getX() + dx, y, death.getZ() + dz);
                            BlockPos floor = feet.below();
                            var state = level.getBlockState(floor);
                            if (!state.isFaceSturdy(level, floor, Direction.UP) || hazardous(state)) continue;
                            pet.setPos(feet.getX() + 0.5, y, feet.getZ() + 0.5);
                            var box = pet.getBoundingBox();
                            if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()) continue;
                            if (level.getWorldBorder().isWithinBounds(box) && level.noCollision(pet)
                                    && !level.containsAnyLiquid(box)
                                    && BlockPos.betweenClosedStream(box).noneMatch(pos -> hazardous(level.getBlockState(pos)))) {
                                return pet.position();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean hazardous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(BlockTags.FIRE);
    }

    private static void warn(PetRevivalData.Pending pending, String text, Throwable error) {
        if (pending.warned) return;
        pending.warned = true;
        LOGGER.warn("{} ({})", text, pending.id(), error);
    }
}
